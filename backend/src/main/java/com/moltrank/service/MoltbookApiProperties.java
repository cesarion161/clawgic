package com.moltrank.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for the Moltbook API connection.
 */
@Component
@ConfigurationProperties(prefix = "moltrank.moltbook")
public class MoltbookApiProperties {

    private String apiUrl;

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
}
