package org.opentron.backend.telemetry;

import org.springframework.stereotype.Service;

/**
 * TelemetryService now delegates to PersistentTelemetryStore.
 * All data is persisted to disk automatically.
 */
@Service
public class TelemetryService {

    private final PersistentTelemetryStore store;

    public TelemetryService(PersistentTelemetryStore store) {
        this.store = store;
    }

    public void recordRequest() {
        store.recordRequest();
    }

    public void addTokens(long tokens) {
        store.addTokens(tokens);
    }

    public void addEnergyJ(long j) {
        store.addEnergyJ(j);
    }

    public long getTotalRequests() {
        return store.getTotalRequests();
    }

    public long getTotalTokens() {
        return store.getTotalTokens();
    }

    public long getTotalEnergyJ() {
        return store.getTotalEnergyJ();
    }

    public void reset() {
        store.reset();
    }
}
