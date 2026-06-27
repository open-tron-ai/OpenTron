package io.opentron.cli;

/**
 * Background state management for CLI tasks.
 * Tracks running background operations and their status.
 */
public class BgState {
    private static java.util.Map<String, BackgroundTask> tasks = new java.util.HashMap<>();
    
    public static class BackgroundTask {
        public String id;
        public String name;
        public String status;  // running, completed, failed, pending
        public long startTime;
        public long endTime;
        public String result;
        public String error;
    }

    public static void registerTask(String id, String name) {
        BackgroundTask task = new BackgroundTask();
        task.id = id;
        task.name = name;
        task.status = "running";
        task.startTime = System.currentTimeMillis();
        tasks.put(id, task);
    }

    public static void updateStatus(String id, String status) {
        BackgroundTask task = tasks.get(id);
        if (task != null) {
            task.status = status;
            if ("completed".equals(status) || "failed".equals(status)) {
                task.endTime = System.currentTimeMillis();
            }
        }
    }

    public static void setResult(String id, String result) {
        BackgroundTask task = tasks.get(id);
        if (task != null) {
            task.result = result;
        }
    }

    public static void setError(String id, String error) {
        BackgroundTask task = tasks.get(id);
        if (task != null) {
            task.error = error;
            task.status = "failed";
        }
    }

    public static BackgroundTask getTask(String id) {
        return tasks.get(id);
    }

    public static java.util.Collection<BackgroundTask> getAll() {
        return tasks.values();
    }

    public static void removeTask(String id) {
        tasks.remove(id);
    }

    public static String printStats() {
        int running = (int) tasks.values().stream().filter(t -> "running".equals(t.status)).count();
        int completed = (int) tasks.values().stream().filter(t -> "completed".equals(t.status)).count();
        int failed = (int) tasks.values().stream().filter(t -> "failed".equals(t.status)).count();
        
        return String.format("Running: %d | Completed: %d | Failed: %d", running, completed, failed);
    }
}
