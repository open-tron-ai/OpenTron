package org.opentron.backend.services;

import org.opentron.backend.storage.entities.PullJob;
import org.opentron.backend.storage.repositories.PullJobRepository;
import org.opentron.backend.util.EngineRouting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PullService {

    private static final Logger logger = LoggerFactory.getLogger(PullService.class);

    private final PullJobRepository jobRepo;
    private final WebClient webClient;
    private final EngineRouting engineRouting;

    // in-memory sinks for streaming job events
    private final Map<String, Sinks.Many<String>> sinks = new ConcurrentHashMap<>();

    public PullService(PullJobRepository jobRepo, WebClient webClient, EngineRouting engineRouting) {
        this.jobRepo = jobRepo;
        this.webClient = webClient;
        this.engineRouting = engineRouting;
    }

    public PullJob createJob(String modelName) {
        PullJob j = new PullJob();
        j.setJobId(UUID.randomUUID().toString());
        j.setModelName(modelName);
        j.setStatus("queued");
        j.setCreatedAt(Instant.now());
        return jobRepo.save(j);
    }

    public Flux<String> streamEvents(String jobId) {
        Sinks.Many<String> sink = sinks.computeIfAbsent(jobId, id -> Sinks.many().multicast().onBackpressureBuffer());
        return sink.asFlux();
    }

    public PullJob getJob(String jobId) {
        return jobRepo.findByJobId(jobId).orElse(null);
    }

    @Async
    public void runPull(PullJob job, Map<String, Object> requestBody) {
        String jobId = job.getJobId();
        try {
            job.setStatus("running");
            job.setStartedAt(Instant.now());
            jobRepo.save(job);

            Sinks.Many<String> sink = sinks.computeIfAbsent(jobId, id -> Sinks.many().multicast().onBackpressureBuffer());

            String targetPath = engineRouting.translateRequestPath("/v1/models/pull");
            webClient.post()
                    .uri(targetPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .doOnNext(chunk -> {
                        sink.tryEmitNext(chunk);
                    })
                    .doOnError(err -> {
                        logger.error("Engine pull error for job {}", jobId, err);
                        sink.tryEmitNext("{\"error\":\"pull_failed\",\"message\":\"" + err.getMessage() + "\"}");
                        sink.tryEmitComplete();
                        job.setStatus("failed");
                        job.setMessage(err.getMessage());
                        job.setCompletedAt(Instant.now());
                        jobRepo.save(job);
                    })
                    .doOnComplete(() -> {
                        sink.tryEmitNext("{\"status\":\"completed\"}");
                        sink.tryEmitComplete();
                        job.setStatus("completed");
                        job.setCompletedAt(Instant.now());
                        jobRepo.save(job);
                    })
                    .subscribe();

        } catch (Exception e) {
            logger.error("Failed to start pull job {}", job.getJobId(), e);
            job.setStatus("failed");
            job.setMessage(e.getMessage());
            job.setCompletedAt(Instant.now());
            jobRepo.save(job);
            Sinks.Many<String> sink = sinks.computeIfAbsent(jobId, id -> Sinks.many().multicast().onBackpressureBuffer());
            sink.tryEmitNext("{\"error\":\"pull_failed\",\"message\":\"" + e.getMessage() + "\"}");
            sink.tryEmitComplete();
        }
    }
}
