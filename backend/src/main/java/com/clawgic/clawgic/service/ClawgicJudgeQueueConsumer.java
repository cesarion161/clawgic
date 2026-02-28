package com.clawgic.clawgic.service;

@FunctionalInterface
public interface ClawgicJudgeQueueConsumer {
    void accept(ClawgicJudgeQueueMessage message);
}
