package org.opentron.backend.storage.service;

import org.opentron.backend.storage.entities.TraceLog;
import org.opentron.backend.storage.repositories.TraceLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Trace Service - handles all trace logging operations
 */
@Service
@Transactional
public class TraceService {
    
    private final TraceLogRepository traceLogRepo;
    
    public TraceService(TraceLogRepository traceLogRepo) {
        this.traceLogRepo = traceLogRepo;
    }
    
    /**
     * Log a trace for an agent
     */
    public TraceLog logTrace(String agent, String input, String output, Integer durationMs) {
        try {
            TraceLog trace = new TraceLog(agent, input, output, durationMs);
            TraceLog saved = traceLogRepo.save(trace);
            System.out.println("[TraceService] ✅ Trace logged for " + agent + " (ID: " + saved.getId() + ", " + durationMs + "ms)");
            return saved;
        } catch (Exception e) {
            System.err.println("[TraceService] ❌ Error logging trace: " + e.getMessage());
            throw new RuntimeException("Failed to log trace", e);
        }
    }
    
    /**
     * Load recent traces for an agent
     */
    public List<TraceLog> getRecentTraces(String agent, int limit) {
        return traceLogRepo.findRecentTraces(agent, Math.min(limit, 1000));
    }
    
    /**
     * Get all traces for an agent
     */
    public List<TraceLog> getAllTraces(String agent) {
        return traceLogRepo.findByAgentOrderByTimestampDesc(agent);
    }
    
    /**
     * Count traces for an agent
     */
    public long countTraces(String agent) {
        return traceLogRepo.countByAgent(agent);
    }
    
    /**
     * Get total trace count
     */
    public long getTotalTraceCount() {
        return traceLogRepo.count();
    }
}
