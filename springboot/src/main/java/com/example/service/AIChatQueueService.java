package com.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * AI chatbot queue layer using LinkedBlockingQueue.
 * LinkedBlockingQueue is a classic dual-lock blocking queue
 * (put lock + take lock) and improves producer/consumer throughput.
 */
@Service
public class AIChatQueueService {

    private static final Logger log = LoggerFactory.getLogger(AIChatQueueService.class);

    private final AIService aiService;

    @Value("${ai.chat-queue.capacity:200}")
    private int queueCapacity;

    @Value("${ai.chat-queue.workers:4}")
    private int workerCount;

    @Value("${ai.chat-queue.enqueue-timeout-ms:300}")
    private long enqueueTimeoutMs;

    @Value("${ai.chat-queue.response-timeout-ms:25000}")
    private long responseTimeoutMs;

    @Value("${ai.chat-queue.max-retries:1}")
    private int maxRetries;

    @Value("${ai.chat-queue.retry-backoff-ms:200}")
    private long retryBackoffMs;

    private BlockingQueue<ChatTask> queue;
    private ExecutorService workerPool;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger activeWorkers = new AtomicInteger(0);

    private final LongAdder totalSubmitted = new LongAdder();
    private final LongAdder totalSucceeded = new LongAdder();
    private final LongAdder totalFailed = new LongAdder();
    private final LongAdder totalRetried = new LongAdder();
    private final LongAdder totalRejected = new LongAdder();
    private final LongAdder totalTimedOut = new LongAdder();

    public AIChatQueueService(AIService aiService) {
        this.aiService = aiService;
    }

    @PostConstruct
    public void init() {
        this.queue = new LinkedBlockingQueue<>(Math.max(10, queueCapacity));
        this.workerPool = Executors.newFixedThreadPool(
                Math.max(1, workerCount),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("ai-chat-worker-" + t.getId());
                    t.setDaemon(true);
                    return t;
                }
        );
        running.set(true);
        for (int i = 0; i < Math.max(1, workerCount); i++) {
            workerPool.submit(this::consumeLoop);
        }
        log.info("AI chat queue initialized: capacity={}, workers={}", queueCapacity, workerCount);
    }

    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (workerPool != null) {
            workerPool.shutdownNow();
        }
    }

    public String submitAndWait(String question, String context) {
        if (!running.get()) {
            throw new RuntimeException("AI queue is not ready");
        }
        ChatTask task = new ChatTask(question, context);
        totalSubmitted.increment();
        try {
            boolean enqueued = queue.offer(task, enqueueTimeoutMs, TimeUnit.MILLISECONDS);
            if (!enqueued) {
                totalRejected.increment();
                throw new RuntimeException("AI queue is full");
            }
            return task.future.get(responseTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            totalTimedOut.increment();
            throw new RuntimeException("AI response timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("AI queue interrupted");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new RuntimeException(cause.getMessage(), cause);
        }
    }

    public Map<String, Object> metrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("running", running.get());
        metrics.put("queueSize", queue == null ? 0 : queue.size());
        metrics.put("queueCapacity", queueCapacity);
        metrics.put("workerCount", workerCount);
        metrics.put("activeWorkers", activeWorkers.get());
        metrics.put("totalSubmitted", totalSubmitted.sum());
        metrics.put("totalSucceeded", totalSucceeded.sum());
        metrics.put("totalFailed", totalFailed.sum());
        metrics.put("totalRetried", totalRetried.sum());
        metrics.put("totalRejected", totalRejected.sum());
        metrics.put("totalTimedOut", totalTimedOut.sum());
        return metrics;
    }

    private void consumeLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                ChatTask task = queue.take();
                activeWorkers.incrementAndGet();
                try {
                    handleTask(task);
                } finally {
                    activeWorkers.decrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("AI chat worker loop exception", e);
            }
        }
    }

    private void handleTask(ChatTask task) {
        for (int attempt = 0; attempt <= Math.max(0, maxRetries); attempt++) {
            try {
                String answer = aiService.chatbotAnswer(task.question, task.context);
                if (answer == null || answer.trim().isEmpty()) {
                    throw new RuntimeException("AI answer is empty");
                }
                totalSucceeded.increment();
                task.future.complete(answer);
                return;
            } catch (Exception e) {
                if (attempt >= Math.max(0, maxRetries)) {
                    totalFailed.increment();
                    task.future.completeExceptionally(
                            new RuntimeException("AI task failed after retries, taskId=" + task.taskId, e)
                    );
                    return;
                }
                totalRetried.increment();
                try {
                    Thread.sleep(Math.max(0L, retryBackoffMs) * (attempt + 1));
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    task.future.completeExceptionally(new RuntimeException("AI task interrupted"));
                    return;
                }
            }
        }
    }

    private static final class ChatTask {
        private final String taskId = UUID.randomUUID().toString();
        private final String question;
        private final String context;
        private final CompletableFuture<String> future = new CompletableFuture<>();

        private ChatTask(String question, String context) {
            this.question = question;
            this.context = context;
        }
    }
}

