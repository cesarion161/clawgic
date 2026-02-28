package com.clawgic.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the legacy social ingestion API connection.
 */
@Component
@ConfigurationProperties(prefix = "clawgic.ingestion")
public class MoltbookApiProperties {

    private String apiUrl;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
}
