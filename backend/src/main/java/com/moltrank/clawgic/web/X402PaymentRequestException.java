package com.moltrank.clawgic.web;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class X402PaymentRequestException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public X402PaymentRequestException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static X402PaymentRequestException malformedHeader(String message) {
        return new X402PaymentRequestException(
                HttpStatus.BAD_REQUEST,
                "x402_malformed_payment_header",
                message
        );
    }

    public static X402PaymentRequestException replayRejected(String message) {
        return new X402PaymentRequestException(
                HttpStatus.CONFLICT,
                "x402_replay_rejected",
                message
        );
    }

    public static X402PaymentRequestException verificationPending() {
        return new X402PaymentRequestException(
                HttpStatus.PAYMENT_REQUIRED,
                "x402_verification_pending",
                "Payment authorization captured and pending verification"
        );
    }

    public static X402PaymentRequestException verificationFailed(String message) {
        return new X402PaymentRequestException(
                HttpStatus.PAYMENT_REQUIRED,
                "x402_verification_failed",
                message
        );
    }
}
