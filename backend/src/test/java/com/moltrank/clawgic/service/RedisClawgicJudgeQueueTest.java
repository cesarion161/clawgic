package com.moltrank.clawgic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moltrank.clawgic.config.ClawgicRuntimeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisClawgicJudgeQueueTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    private ClawgicRuntimeProperties clawgicRuntimeProperties;
    private RedisClawgicJudgeQueue redisClawgicJudgeQueue;

    @BeforeEach
    void setUp() {
        clawgicRuntimeProperties = new ClawgicRuntimeProperties();
        clawgicRuntimeProperties.getWorker().setRedisQueueKey("clawgic:test:judge:queue");
        clawgicRuntimeProperties.getWorker().setRedisPopTimeoutSeconds(2);
        redisClawgicJudgeQueue = new RedisClawgicJudgeQueue(
                stringRedisTemplate,
                clawgicRuntimeProperties,
                new ObjectMapper()
        );
        when(stringRedisTemplate.opsForList()).thenReturn(listOperations);
    }

    @Test
    void enqueuePushesSerializedMessageOntoConfiguredQueue() {
        UUID matchId = UUID.randomUUID();
        ClawgicJudgeQueueMessage message = new ClawgicJudgeQueueMessage(matchId, "mock-judge-primary");
        when(listOperations.rightPush(eq("clawgic:test:judge:queue"), anyString())).thenReturn(1L);

        redisClawgicJudgeQueue.enqueue(message);

        verify(listOperations).rightPush(
                eq("clawgic:test:judge:queue"),
                argThat(payload -> payload.contains(matchId.toString()) && payload.contains("\"judgeKey\":\"mock-judge-primary\""))
        );
    }

    @Test
    void pollOnceDispatchesDeserializedMessageToConsumer() throws Exception {
        UUID matchId = UUID.randomUUID();
        ClawgicJudgeQueueMessage queuedMessage = new ClawgicJudgeQueueMessage(matchId, "mock-judge-primary");
        String payload = new ObjectMapper().writeValueAsString(queuedMessage);
        when(listOperations.leftPop("clawgic:test:judge:queue", 2L, TimeUnit.SECONDS)).thenReturn(payload);

        AtomicReference<ClawgicJudgeQueueMessage> receivedMessage = new AtomicReference<>();
        redisClawgicJudgeQueue.setConsumer(receivedMessage::set);

        boolean processed = redisClawgicJudgeQueue.pollOnce();

        assertTrue(processed);
        assertEquals(queuedMessage, receivedMessage.get());
    }

    @Test
    void pollOnceReturnsFalseWhenQueueIsEmpty() throws InterruptedException {
        when(listOperations.leftPop("clawgic:test:judge:queue", 2L, TimeUnit.SECONDS)).thenReturn(null);
        redisClawgicJudgeQueue.setConsumer(ignored -> {
        });

        boolean processed = redisClawgicJudgeQueue.pollOnce();

        assertFalse(processed);
    }

    @Test
    void enqueueFailsFastWhenQueueKeyIsBlank() {
        clawgicRuntimeProperties.getWorker().setRedisQueueKey("   ");

        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> redisClawgicJudgeQueue.enqueue(new ClawgicJudgeQueueMessage(UUID.randomUUID(), "mock-judge-primary"))
        );

        assertEquals("clawgic.worker.redis-queue-key must not be blank", thrown.getMessage());
    }
}
