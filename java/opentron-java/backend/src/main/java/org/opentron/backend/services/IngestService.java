package org.opentron.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IngestService {

    private static final Logger logger = LoggerFactory.getLogger(IngestService.class);
    private final Map<String, String> jobStates = new ConcurrentHashMap<>();

    public String createJob(String jobId) {
        jobStates.put(jobId, "queued");
        return jobId;
    }

    public String getJobState(String jobId) {
        return jobStates.getOrDefault(jobId, "unknown");
    }

    @Async
    public void processIngest(String jobId, Map<String, Object> request) {
        logger.info("[IngestService] processIngest job={} request={}", jobId, request);
        jobStates.put(jobId, "running");
        try {
            // TODO: replace this sleep with real ingestion/indexing logic
            Thread.sleep(2000);
            jobStates.put(jobId, "completed");
            logger.info("[IngestService] completed job={}", jobId);
        } catch (InterruptedException e) {
            jobStates.put(jobId, "failed");
            logger.error("[IngestService] job interrupted {}", jobId, e);
        }
    }

    @Async
    public void processFileIngest(String jobId, Path directory) {
        logger.info("[IngestService] processFileIngest job={} dir={}", jobId, directory);
        jobStates.put(jobId, "running");
        try {
            // TODO: implement file parsing, chunking, and indexing into vector DB
            Thread.sleep(2000);
            jobStates.put(jobId, "completed");
            logger.info("[IngestService] completed file job={}", jobId);
        } catch (InterruptedException e) {
            jobStates.put(jobId, "failed");
            logger.error("[IngestService] file job interrupted {}", jobId, e);
        }
    }
}
