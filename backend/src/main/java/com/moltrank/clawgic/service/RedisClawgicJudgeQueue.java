package com.moltrank.clawgic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moltrank.clawgic.config.ClawgicRuntimeProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "clawgic.worker",
        name = "queue-mode",
        havingValue = "redis"
)
public class RedisClawgicJudgeQueue implements ClawgicJudgeQueue {

    private static final Logger log = LoggerFactory.getLogger(RedisClawgicJudgeQueue.class);

    private final StringRedisTemplate stringRedisTemplate;
    private final ClawgicRuntimeProperties clawgicRuntimeProperties;
    private final ObjectMapper objectMapper;
    private final Object consumerMonitor = new Object();

    private volatile boolean running = true;
    private volatile ClawgicJudgeQueueConsumer consumer;
    private Thread dispatcherThread;

    @PostConstruct
    void startDispatcher() {
        dispatcherThread = Thread.ofVirtual()
                .name("clawgic-judge-redis-queue-dispatcher")
                .start(this::dispatchLoop);
    }

    @PreDestroy
    void stopDispatcher() {
        running = false;
        synchronized (consumerMonitor) {
            consumerMonitor.notifyAll();
        }
        if (dispatcherThread != null) {
            dispatcherThread.interrupt();
        }
    }

    @Override
    public void enqueue(ClawgicJudgeQueueMessage message) {
        if (!running) {
            throw new IllegalStateException("Judge queue is not running");
        }
        ClawgicJudgeQueueMessage requiredMessage = Objects.requireNonNull(message, "message is required");
        Long queueDepth = stringRedisTemplate.opsForList().rightPush(
                resolveRedisQueueKey(),
                serialize(requiredMessage)
        );
        if (queueDepth == null) {
            throw new IllegalStateException("Failed to enqueue judge queue message");
        }
    }

    @Override
    public void setConsumer(ClawgicJudgeQueueConsumer consumer) {
        synchronized (consumerMonitor) {
            this.consumer = Objects.requireNonNull(consumer, "consumer is required");
            consumerMonitor.notifyAll();
        }
    }

    boolean pollOnce() throws InterruptedException {
        String payload = stringRedisTemplate.opsForList().leftPop(
                resolveRedisQueueKey(),
                resolveRedisPopTimeoutSeconds(),
                TimeUnit.SECONDS
        );
        if (payload == null) {
            return false;
        }
        ClawgicJudgeQueueConsumer queueConsumer = awaitConsumer();
        if (queueConsumer == null) {
            return false;
        }
        queueConsumer.accept(deserialize(payload));
        return true;
    }

    private void dispatchLoop() {
        while (running) {
            try {
                pollOnce();
            } catch (InterruptedException ex) {
                if (!running) {
                    Thread.currentThread().interrupt();
                    return;
                }
            } catch (RuntimeException ex) {
                log.error("Judge queue polling failed while consuming Redis messages", ex);
            }
        }
    }

    private ClawgicJudgeQueueConsumer awaitConsumer() throws InterruptedException {
        synchronized (consumerMonitor) {
            while (running && consumer == null) {
                consumerMonitor.wait();
            }
            return consumer;
        }
    }

    private String resolveRedisQueueKey() {
        String queueKey = clawgicRuntimeProperties.getWorker().getRedisQueueKey();
        if (queueKey == null || queueKey.isBlank()) {
            throw new IllegalStateException("clawgic.worker.redis-queue-key must not be blank");
        }
        return queueKey.trim();
    }

    private long resolveRedisPopTimeoutSeconds() {
        long timeoutSeconds = clawgicRuntimeProperties.getWorker().getRedisPopTimeoutSeconds();
        if (timeoutSeconds <= 0) {
            throw new IllegalStateException("clawgic.worker.redis-pop-timeout-seconds must be greater than zero");
        }
        return timeoutSeconds;
    }

    private String serialize(ClawgicJudgeQueueMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize judge queue message payload", ex);
        }
    }

    private ClawgicJudgeQueueMessage deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, ClawgicJudgeQueueMessage.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to deserialize judge queue payload", ex);
        }
    }
}
