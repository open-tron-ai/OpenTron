package org.opentron.backend.dto;

public class IndexPathRequest {
    private String path;
    private boolean recursive = true;

    public IndexPathRequest() {}

    public IndexPathRequest(String path, boolean recursive) {
        this.path = path;
        this.recursive = recursive;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isRecursive() {
        return recursive;
    }

    public void setRecursive(boolean recursive) {
        this.recursive = recursive;
    }
}
