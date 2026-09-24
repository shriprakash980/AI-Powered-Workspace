package com.devpilot.ai.cicd.worker;

import com.devpilot.ai.cicd.service.PipelineExecutionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class PipelineWorker {

    private static final Logger log = LoggerFactory.getLogger(PipelineWorker.class);

    private final PipelineQueue queue;
    private final PipelineExecutionService executionService;
    private final boolean cicdEnabled;
    private final int workerPoolSize;

    private ExecutorService executor;
    private volatile boolean running = true;

    public PipelineWorker(
            PipelineQueue queue,
            @Lazy PipelineExecutionService executionService,
            @Value("${app.cicd.enabled:true}") boolean cicdEnabled,
            @Value("${app.cicd.max-concurrent-runs:5}") int workerPoolSize) {
        this.queue = queue;
        this.executionService = executionService;
        this.cicdEnabled = cicdEnabled;
        this.workerPoolSize = Math.max(1, workerPoolSize);
    }

    @PostConstruct
    public void start() {
        if (!cicdEnabled) {
            log.info("DevPilot CI/CD worker is disabled by configuration (app.cicd.enabled=false).");
            return;
        }

        log.info("Starting DevPilot CI/CD worker pool with {} threads...", workerPoolSize);
        executor = Executors.newFixedThreadPool(workerPoolSize);

        for (int i = 0; i < workerPoolSize; i++) {
            executor.submit(this::workerLoop);
        }
    }

    private void workerLoop() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                PipelineJob job = queue.poll();
                if (job != null) {
                    queue.incrementActive(job.projectId().toString());
                    try {
                        log.info("Executing pipeline job run ID: {} for project: {}", job.runId(), job.projectId());
                        executionService.executePipelineRun(job.runId());
                    } catch (Exception e) {
                        log.error("Pipeline worker exception while executing run ID: {}", job.runId(), e);
                    } finally {
                        queue.decrementActive(job.projectId().toString());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected exception in worker loop: {}", e.getMessage(), e);
            }
        }
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (executor != null) {
            log.info("Shutting down DevPilot CI/CD worker thread pool...");
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("Worker pool did not terminate cleanly within 5 seconds.");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
