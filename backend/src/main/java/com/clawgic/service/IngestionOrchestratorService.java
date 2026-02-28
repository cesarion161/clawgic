package com.clawgic.service;

import com.clawgic.model.Market;
import com.clawgic.repository.MarketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Runs periodic topic-scoped ingestion so posts are continuously available
 * for ranking and curation rounds.
 *
 * Legacy subsystem: disabled by default for the Clawgic pivot.
 */
@Service
@ConditionalOnProperty(name = "clawgic.ingestion.enabled", havingValue = "true")
public class IngestionOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(IngestionOrchestratorService.class);

    @Value("${clawgic.ingestion.enabled:false}")
    private boolean ingestionEnabled;

    @Value("${clawgic.ingestion.run-on-startup:false}")
    private boolean runOnStartup;

    @Value("${clawgic.ingestion.fetch-limit:100}")
    private int fetchLimit;

    private final ScraperService scraperService;
    private final MarketRepository marketRepository;

    public IngestionOrchestratorService(
            ScraperService scraperService,
            MarketRepository marketRepository) {
        this.scraperService = scraperService;
        this.marketRepository = marketRepository;
    }

    /**
     * Triggers initial ingestion once app startup is complete.
     * Runs after bootstrap seeders so default markets are already present.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!ingestionEnabled || !runOnStartup) {
            log.debug("Startup ingestion skipped (enabled={}, runOnStartup={})",
                    ingestionEnabled, runOnStartup);
            return;
        }
        runIngestionCycle("startup");
    }

    /**
     * Scheduled ingestion loop for continuous post ingestion.
     */
    @Scheduled(
            fixedDelayString = "${clawgic.ingestion.interval-ms:300000}",
            initialDelayString = "${clawgic.ingestion.initial-delay-ms:60000}")
    public void scheduledIngestion() {
        runIngestionCycle("scheduler");
    }

    /**
     * Runs one ingestion cycle over all configured markets.
     *
     * @param source Source label for logging (startup/scheduler/manual)
     * @return Total number of newly ingested posts across markets
     */
    int runIngestionCycle(String source) {
        if (!ingestionEnabled) {
            log.debug("Ingestion cycle ({}) skipped: ingestion disabled", source);
            return 0;
        }

        if (fetchLimit <= 0) {
            log.warn("Ingestion cycle ({}) skipped: invalid fetch limit {}", source, fetchLimit);
            return 0;
        }

        List<Market> markets = marketRepository.findAll();
        if (markets.isEmpty()) {
            log.debug("Ingestion cycle ({}) skipped: no markets found", source);
            return 0;
        }

        int totalNewPosts = 0;
        for (Market market : markets) {
            if (market.getSubmoltId() == null || market.getSubmoltId().isBlank()) {
                log.warn("Skipping ingestion for market {} (id={}): blank submoltId",
                        market.getName(), market.getId());
                continue;
            }

            try {
                int newPosts = scraperService.scrapePosts(market, fetchLimit).size();
                totalNewPosts += newPosts;
                log.info("Ingestion cycle ({}) market {} ingested {} new posts",
                        source, market.getName(), newPosts);
            } catch (Exception e) {
                log.error("Ingestion cycle ({}) failed for market {}", source, market.getName(), e);
            }
        }

        log.info("Ingestion cycle ({}) complete: {} market(s), {} new post(s)",
                source, markets.size(), totalNewPosts);
        return totalNewPosts;
    }
}
