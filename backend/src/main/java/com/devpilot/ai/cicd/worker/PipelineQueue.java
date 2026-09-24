package com.devpilot.ai.cicd.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PipelineQueue {

    private static final Logger log = LoggerFactory.getLogger(PipelineQueue.class);

    private final BlockingQueue<PipelineJob> queue;
    private final int maxQueueSize;
    private final int maxConcurrentRuns;
    private final int maxConcurrentPerProject;
    private final AtomicInteger activeRuns = new AtomicInteger(0);
    private final Map<String, AtomicInteger> activeRunsPerProject = new ConcurrentHashMap<>();

    public PipelineQueue(
            @Value("${app.cicd.queue-size:50}") int maxQueueSize,
            @Value("${app.cicd.max-concurrent-runs:5}") int maxConcurrentRuns,
            @Value("${app.cicd.max-concurrent-per-project:2}") int maxConcurrentPerProject) {
        this.maxQueueSize = maxQueueSize;
        this.maxConcurrentRuns = maxConcurrentRuns;
        this.maxConcurrentPerProject = maxConcurrentPerProject;
        this.queue = new LinkedBlockingQueue<>(maxQueueSize);
    }

    public boolean enqueue(PipelineJob job) {
        if (queue.size() >= maxQueueSize) {
            log.warn("Pipeline queue is full (size: {}). Rejecting job runId: {}", queue.size(), job.runId());
            return false;
        }
        boolean offer = queue.offer(job);
        if (offer) {
            log.info("Enqueued pipeline job runId: {} for project ID: {}", job.runId(), job.projectId());
        }
        return offer;
    }

    public PipelineJob poll() throws InterruptedException {
        return queue.take();
    }

    public void incrementActive(String projectIdStr) {
        activeRuns.incrementAndGet();
        activeRunsPerProject.computeIfAbsent(projectIdStr, k -> new AtomicInteger(0)).incrementAndGet();
    }

    public void decrementActive(String projectIdStr) {
        activeRuns.decrementAndGet();
        AtomicInteger count = activeRunsPerProject.get(projectIdStr);
        if (count != null) {
            count.decrementAndGet();
        }
    }

    public boolean canExecuteProjectRun(String projectIdStr) {
        if (activeRuns.get() >= maxConcurrentRuns) {
            return false;
        }
        AtomicInteger count = activeRunsPerProject.get(projectIdStr);
        return count == null || count.get() < maxConcurrentPerProject;
    }

    public int getQueueSize() {
        return queue.size();
    }

    public int getActiveRuns() {
        return activeRuns.get();
    }
}
