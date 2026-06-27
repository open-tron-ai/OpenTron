package org.opentron.backend.telemetry;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class TelemetryService {

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalTokens = new AtomicLong(0);
    private final AtomicLong totalEnergyJ = new AtomicLong(0);

    public void recordRequest() {
        totalRequests.incrementAndGet();
    }

    public void addTokens(long tokens) {
        totalTokens.addAndGet(tokens);
    }

    public void addEnergyJ(long j) {
        totalEnergyJ.addAndGet(j);
    }

    public long getTotalRequests() { return totalRequests.get(); }
    public long getTotalTokens() { return totalTokens.get(); }
    public long getTotalEnergyJ() { return totalEnergyJ.get(); }
}
