package com.clawgic.clawgic.provider;

import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Shared retry policy for live provider HTTP calls.
 */
final class ClawgicProviderRetrySupport {

    private ClawgicProviderRetrySupport() {
    }

    static <T> T execute(
            String providerName,
            int configuredMaxAttempts,
            long configuredBackoffMs,
            RetryableCall<T> call
    ) {
        int maxAttempts = Math.max(1, configuredMaxAttempts);
        long backoffMs = Math.max(0L, configuredBackoffMs);

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return call.invoke();
            } catch (RestClientResponseException ex) {
                if (!isRetryableStatus(ex.getStatusCode().value()) || attempt >= maxAttempts) {
                    throw new IllegalStateException(
                            providerName
                                    + " provider request failed with status "
                                    + ex.getStatusCode().value()
                                    + ": "
                                    + summarizeBody(ex.getResponseBodyAsString()),
                            ex
                    );
                }
                sleepBackoff(backoffMs, attempt);
            } catch (ResourceAccessException ex) {
                if (attempt >= maxAttempts) {
                    throw new IllegalStateException(
                            providerName + " provider request failed: " + ex.getMessage(),
                            ex
                    );
                }
                sleepBackoff(backoffMs, attempt);
            }
        }

        throw new IllegalStateException(providerName + " provider request failed after retries");
    }

    private static boolean isRetryableStatus(int statusCode) {
        return statusCode == 408 || statusCode == 429 || statusCode >= 500;
    }

    private static void sleepBackoff(long backoffMs, int attempt) {
        if (backoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(backoffMs * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Provider retry wait interrupted", ex);
        }
    }

    private static String summarizeBody(String responseBody) {
        String normalized = StringUtils.hasText(responseBody) ? responseBody.trim() : "<empty response body>";
        return normalized.length() > 280 ? normalized.substring(0, 280) + "..." : normalized;
    }

    @FunctionalInterface
    interface RetryableCall<T> {
        T invoke();
    }
}

