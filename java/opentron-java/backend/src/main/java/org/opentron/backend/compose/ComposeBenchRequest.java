package org.opentron.backend.compose;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ComposeBenchRequest {
    private String name;
    private List<String> benchmarks;

    @JsonProperty("max_samples")
    private Integer maxSamples;

    @JsonProperty("judge_model")
    private String judgeModel;

    private boolean verbose;

    public ComposeBenchRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getBenchmarks() {
        return benchmarks;
    }

    public void setBenchmarks(List<String> benchmarks) {
        this.benchmarks = benchmarks;
    }

    public Integer getMaxSamples() {
        return maxSamples;
    }

    public void setMaxSamples(Integer maxSamples) {
        this.maxSamples = maxSamples;
    }

    public String getJudgeModel() {
        return judgeModel;
    }

    public void setJudgeModel(String judgeModel) {
        this.judgeModel = judgeModel;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
