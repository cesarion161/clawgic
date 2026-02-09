package com.moltrank.controller;

import com.moltrank.model.Market;
import com.moltrank.model.Round;
import com.moltrank.model.Subscription;
import com.moltrank.repository.MarketRepository;
import com.moltrank.repository.RoundRepository;
import com.moltrank.repository.SubscriptionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

/**
 * REST API for subscriptions.
 */
@RestController
@RequestMapping("/api")
public class SubscriptionController {

    private final SubscriptionRepository subscriptionRepository;
    private final MarketRepository marketRepository;
    private final RoundRepository roundRepository;

    public SubscriptionController(SubscriptionRepository subscriptionRepository,
                                  MarketRepository marketRepository,
                                  RoundRepository roundRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.marketRepository = marketRepository;
        this.roundRepository = roundRepository;
    }

    /**
     * Create subscription.
     *
     * @param subscription The subscription request
     * @return Created subscription
     */
    @PostMapping("/subscribe")
    public ResponseEntity<Subscription> createSubscription(@RequestBody Subscription subscription) {
        // Fetch market
        Market market = marketRepository.findById(subscription.getMarket().getId())
                .orElse(null);

        if (market == null) {
            return ResponseEntity.badRequest().build();
        }

        subscription.setMarket(market);

        // Fetch round if specified
        if (subscription.getRound() != null && subscription.getRound().getId() != null) {
            Round round = roundRepository.findById(subscription.getRound().getId())
                    .orElse(null);
            subscription.setRound(round);
        }

        // Set timestamps
        subscription.setSubscribedAt(OffsetDateTime.now());

        // Calculate expiration based on subscription type
        // For now, set a simple 30-day expiration
        // In production, this would vary by subscription type and payment amount
        subscription.setExpiresAt(OffsetDateTime.now().plusDays(30));

        Subscription created = subscriptionRepository.save(subscription);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
