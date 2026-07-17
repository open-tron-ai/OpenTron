package org.opentron.backend.storage.repositories;

import org.opentron.backend.storage.entities.ConnectorState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConnectorStateRepository extends JpaRepository<ConnectorState, String> {

    List<ConnectorState> findByConnectedTrue();

    @Query("SELECT c FROM ConnectorState c WHERE c.syncState = 'syncing'")
    List<ConnectorState> findActivelySyncing();
}
