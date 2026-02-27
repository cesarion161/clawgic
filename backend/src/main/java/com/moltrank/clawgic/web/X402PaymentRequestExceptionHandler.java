package com.moltrank.clawgic.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class X402PaymentRequestExceptionHandler {

    @ExceptionHandler(X402PaymentRequestException.class)
    public ResponseEntity<X402PaymentErrorResponse> handle(X402PaymentRequestException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new X402PaymentErrorResponse(ex.getCode(), ex.getMessage()));
    }

    public record X402PaymentErrorResponse(
            String code,
            String message
    ) {
    }
}
