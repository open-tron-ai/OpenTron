package org.opentron.backend.storage.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.opentron.backend.storage.entities.IngestJob;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IngestJobRepository extends JpaRepository<IngestJob, Long> {
    Optional<IngestJob> findByJobId(String jobId);

    List<IngestJob> findByStatus(String status);

    List<IngestJob> findByConnectorType(String connectorType);

    @Query("SELECT j FROM IngestJob j WHERE j.createdAt >= :since ORDER BY j.createdAt DESC")
    List<IngestJob> findRecentJobs(@Param("since") Instant since);

    @Query("SELECT j FROM IngestJob j WHERE j.status IN ('running', 'queued') ORDER BY j.createdAt DESC")
    List<IngestJob> findActiveJobs();

    @Query("SELECT j FROM IngestJob j WHERE j.connectorType = :connector AND j.status = :status ORDER BY j.createdAt DESC LIMIT 10")
    List<IngestJob> findRecentJobsByConnectorAndStatus(@Param("connector") String connector, @Param("status") String status);
}
