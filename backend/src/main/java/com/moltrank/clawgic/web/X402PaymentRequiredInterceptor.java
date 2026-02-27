package com.moltrank.clawgic.web;

import com.moltrank.clawgic.config.X402Properties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Enforces an x402 payment challenge on protected Clawgic entry routes when
 * live x402 mode is enabled and no payment header is present.
 */
public class X402PaymentRequiredInterceptor implements HandlerInterceptor {

    private static final String CHALLENGE_SCHEME = "x402";

    private final X402Properties x402Properties;

    public X402PaymentRequiredInterceptor(X402Properties x402Properties) {
        this.x402Properties = x402Properties;
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws Exception {
        if (!x402Properties.isEnabled()) {
            return true;
        }

        String paymentHeaderValue = request.getHeader(x402Properties.getPaymentHeaderName());
        if (paymentHeaderValue != null && !paymentHeaderValue.isBlank()) {
            return true;
        }

        OffsetDateTime challengeExpiresAt =
                OffsetDateTime.now().plusSeconds(Math.max(1L, x402Properties.getNonceTtlSeconds()));

        PaymentRequiredChallenge challenge = new PaymentRequiredChallenge(
                CHALLENGE_SCHEME,
                x402Properties.getNetwork(),
                x402Properties.getChainId(),
                x402Properties.getTokenAddress(),
                scaleUsdc(x402Properties.getDefaultEntryFeeUsdc()),
                x402Properties.getSettlementAddress(),
                x402Properties.getPaymentHeaderName(),
                UUID.randomUUID().toString(),
                challengeExpiresAt
        );

        response.setStatus(HttpStatus.PAYMENT_REQUIRED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(toJson(challenge));
        return false;
    }

    private BigDecimal scaleUsdc(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }
        return amount.setScale(6, RoundingMode.HALF_UP);
    }

    private String toJson(PaymentRequiredChallenge challenge) {
        return """
                {
                  "scheme":"%s",
                  "network":"%s",
                  "chainId":%d,
                  "tokenAddress":"%s",
                  "priceUsdc":%s,
                  "recipient":"%s",
                  "paymentHeader":"%s",
                  "nonce":"%s",
                  "challengeExpiresAt":"%s"
                }
                """.formatted(
                escapeJson(challenge.scheme()),
                escapeJson(challenge.network()),
                challenge.chainId(),
                escapeJson(challenge.tokenAddress()),
                challenge.priceUsdc().stripTrailingZeros().toPlainString(),
                escapeJson(challenge.recipient()),
                escapeJson(challenge.paymentHeader()),
                escapeJson(challenge.nonce()),
                challenge.challengeExpiresAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        );
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private record PaymentRequiredChallenge(
            String scheme,
            String network,
            long chainId,
            String tokenAddress,
            BigDecimal priceUsdc,
            String recipient,
            String paymentHeader,
            String nonce,
            OffsetDateTime challengeExpiresAt
    ) {
    }
}
