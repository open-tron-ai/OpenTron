package org.opentron.backend.storage.repositories;

import org.opentron.backend.storage.entities.AgentMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for AgentMemory
 * Provides query methods for agent memory access
 */
@Repository
public interface AgentMemoryRepository extends JpaRepository<AgentMemory, Long> {
    
    /**
     * Find all memory entries for an agent, ordered by timestamp
     */
    List<AgentMemory> findByAgentNameOrderByTimestampDesc(String agentName);
    
    /**
     * Find memory entries for an agent after a specific timestamp
     */
    List<AgentMemory> findByAgentNameAndTimestampAfter(String agentName, LocalDateTime timestamp);
    
    /**
     * Find memory by trace hash (for deduplication)
     */
    Optional<AgentMemory> findByTraceHash(String traceHash);
    
    /**
     * Find recent non-archived memory for an agent
     */
    @Query("SELECT am FROM AgentMemory am WHERE am.agentName = :agentName AND am.isArchived = false ORDER BY am.timestamp DESC LIMIT :limit")
    List<AgentMemory> findRecentMemory(@Param("agentName") String agentName, @Param("limit") int limit);
    
    /**
     * Archive old memory entries (move to archive table)
     */
    @Query(value = "DELETE FROM agent_memory WHERE timestamp < :cutoffDate AND is_archived = false", nativeQuery = true)
    void deleteOldMemory(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Count memory entries for a specific agent
     */
    long countByAgentName(String agentName);
}
