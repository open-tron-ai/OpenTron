package org.opentron.backend.storage.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.opentron.backend.storage.entities.PullJob;
import java.util.Optional;

@Repository
public interface PullJobRepository extends JpaRepository<PullJob, Long> {
    Optional<PullJob> findByJobId(String jobId);
}
