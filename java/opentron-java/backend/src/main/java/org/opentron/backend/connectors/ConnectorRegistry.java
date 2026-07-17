package org.opentron.backend.connectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Holds all registered DataConnector implementations and delegates
 * connect / fetch calls to the right one by connector ID.
 *
 * Spring auto-discovers every @Component that implements DataConnector
 * and injects them as a list — no manual registration needed.
 */
@Service
public class ConnectorRegistry {

    private static final Logger logger = LoggerFactory.getLogger(ConnectorRegistry.class);

    private final Map<String, DataConnector> connectors;

    public ConnectorRegistry(List<DataConnector> list) {
        this.connectors = list.stream()
                .collect(Collectors.toMap(DataConnector::id, c -> c));
        logger.info("ConnectorRegistry loaded {} connector(s): {}", connectors.size(), connectors.keySet());
    }

    public Optional<DataConnector> get(String id) {
        return Optional.ofNullable(connectors.get(id));
    }

    public Map<String, DataConnector> all() {
        return connectors;
    }

    public boolean has(String id) {
        return connectors.containsKey(id);
    }
}
