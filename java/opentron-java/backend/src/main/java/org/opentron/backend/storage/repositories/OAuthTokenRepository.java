package org.opentron.backend.storage.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.opentron.backend.storage.entities.OAuthToken;
import java.util.Optional;

@Repository
public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {
    Optional<OAuthToken> findTopByConnectorIdOrderByCreatedAtDesc(String connectorId);
}
