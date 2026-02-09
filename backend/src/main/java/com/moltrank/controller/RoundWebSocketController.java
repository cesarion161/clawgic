package com.moltrank.controller;

import com.moltrank.model.Round;
import com.moltrank.repository.RoundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for real-time round status updates.
 *
 * Broadcasts to /topic/rounds when round status changes.
 */
@Controller
public class RoundWebSocketController {

    private static final Logger log = LoggerFactory.getLogger(RoundWebSocketController.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final RoundRepository roundRepository;

    public RoundWebSocketController(SimpMessagingTemplate messagingTemplate,
                                    RoundRepository roundRepository) {
        this.messagingTemplate = messagingTemplate;
        this.roundRepository = roundRepository;
    }

    /**
     * Broadcast round status update to all subscribers.
     *
     * @param roundId The round ID to broadcast
     */
    public void broadcastRoundUpdate(Integer roundId) {
        Round round = roundRepository.findById(roundId).orElse(null);

        if (round != null) {
            messagingTemplate.convertAndSend("/topic/rounds", round);
            log.info("Broadcast round update: id={}, status={}", roundId, round.getStatus());
        }
    }

    /**
     * Broadcast round status update for a specific market.
     *
     * @param marketId The market ID
     * @param roundId The round ID to broadcast
     */
    public void broadcastRoundUpdateForMarket(Integer marketId, Integer roundId) {
        Round round = roundRepository.findById(roundId).orElse(null);

        if (round != null) {
            messagingTemplate.convertAndSend("/topic/rounds/" + marketId, round);
            log.info("Broadcast round update for market {}: id={}, status={}",
                    marketId, roundId, round.getStatus());
        }
    }
}
