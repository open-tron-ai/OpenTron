package org.opentron.backend.storage.repositories;

import org.opentron.backend.storage.entities.TraceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for TraceLog
 * Provides query methods for trace log access
 */
@Repository
public interface TraceLogRepository extends JpaRepository<TraceLog, Long> {
    
    /**
     * Find all traces for an agent, ordered by timestamp
     */
    List<TraceLog> findByAgentOrderByTimestampDesc(String agent);
    
    /**
     * Find traces for an agent after a specific timestamp
     */
    List<TraceLog> findByAgentAndTimestampAfter(String agent, LocalDateTime timestamp);
    
    /**
     * Find recent uncompressed traces for an agent
     */
    @Query("SELECT tl FROM TraceLog tl WHERE tl.agent = :agent AND tl.isCompressed = false ORDER BY tl.timestamp DESC LIMIT :limit")
    List<TraceLog> findRecentTraces(@Param("agent") String agent, @Param("limit") int limit);
    
    /**
     * Find all traces for an agent within a date range
     */
    List<TraceLog> findByAgentAndTimestampBetween(String agent, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Count traces for a specific agent
     */
    long countByAgent(String agent);
    
    /**
     * Archive old traces (mark as compressed or delete)
     */
    @Query(value = "DELETE FROM trace_logs WHERE timestamp < :cutoffDate", nativeQuery = true)
    void deleteOldTraces(@Param("cutoffDate") LocalDateTime cutoffDate);
}
