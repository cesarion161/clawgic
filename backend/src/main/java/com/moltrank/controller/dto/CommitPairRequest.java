package com.moltrank.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record CommitPairRequest(
        @JsonAlias("curatorWallet") String wallet,
        @JsonAlias("hash") String commitmentHash,
        @JsonAlias("stake") Long stakeAmount,
        String encryptedReveal,
        String revealIv,
        @JsonAlias("walletSignature") String signature,
        @JsonAlias("signedAt") Long signedAtEpochSeconds,
        String requestNonce
) {
    public boolean isValid() {
        return isPresent(wallet)
                && isPresent(commitmentHash)
                && stakeAmount != null
                && stakeAmount > 0
                && isPresent(encryptedReveal);
    }

    public boolean hasCommitAuthEnvelope() {
        return isPresent(revealIv)
                && isPresent(signature)
                && signedAtEpochSeconds != null
                && isPresent(requestNonce);
    }

    private static boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }
}
