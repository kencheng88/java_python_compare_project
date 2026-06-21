package com.example.todo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller containing diagnostic endpoints for memory profiling and Out Of Memory (OOM) simulation.
 *
 * <p><strong>Python/FastAPI Comparison:</strong>
 * In Python, triggering an OOM or memory leak usually involves appending large objects to a global list
 * or invoking C-level allocations. Python's memory manager might return memory back to the OS differently.
 * In Java, the JVM has strict maximum heap limits configured via {@code -Xmx}.
 * The `/oom` endpoint below simulates a standard Java memory leak by continuously appending large byte arrays
 * to a local list until the JVM throws an {@link OutOfMemoryError}.
 * </p>
 */
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    /**
     * Diagnostic endpoint returning current JVM memory statistics.
     *
     * @return map containing free, total, and max memory in MB
     */
    @GetMapping("/memory")
    public Map<String, Object> getMemoryStats() {
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();

        Map<String, Object> stats = new HashMap<>();
        stats.put("maxMemoryMb", maxMemory / (1024 * 1024));
        stats.put("allocatedMemoryMb", allocatedMemory / (1024 * 1024));
        stats.put("freeMemoryMb", freeMemory / (1024 * 1024));
        stats.put("usedMemoryMb", (allocatedMemory - freeMemory) / (1024 * 1024));
        stats.put("availableProcessors", runtime.availableProcessors());
        return stats;
    }

    /**
     * Endpoint that intentionally triggers a Java Heap Space OutOfMemoryError.
     *
     * <p><strong>WARNING:</strong> This will exhaust the JVM heap and likely require
     * restarting the Spring Boot application.
     * </p>
     */
    @GetMapping("/oom")
    public String triggerOom() {
        // Start a background thread to perform allocations so that we don't completely block the main thread,
        // but still guarantee heap exhaustion quickly.
        Thread oomThread = new Thread(() -> {
            List<byte[]> leakList = new ArrayList<>();
            try {
                while (true) {
                    // Allocate 10MB chunks
                    leakList.add(new byte[10 * 1024 * 1024]);
                }
            } catch (OutOfMemoryError e) {
                // Log and release references
                System.err.println("Out of memory simulated successfully!");
                leakList.clear();
            }
        });
        oomThread.setName("oom-simulation-thread");
        oomThread.start();

        return "OOM simulation thread started. Heap memory is being exhausted in the background.";
    }
}
