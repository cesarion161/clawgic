package com.clawgic.service;

import com.clawgic.controller.dto.CommitPairRequest;
import com.clawgic.model.CommitRequestReplayGuard;
import com.clawgic.repository.CommitRequestReplayGuardRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class CommitSecurityService {

    private static final String AUTH_MESSAGE_PREFIX = "clawgic-commit-v1";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int REQUEST_NONCE_BYTES = 16;

    private final CommitRequestReplayGuardRepository replayGuardRepository;
    private final CommitRevealEnvelopeCryptoService commitRevealEnvelopeCryptoService;
    private final CommitSecurityProperties commitSecurityProperties;

    @Transactional
    public SecuredCommitmentPayload secureCommitPayload(Integer pairId, CommitPairRequest request) {
        final String normalizedHash;
        try {
            normalizedHash = CommitmentCodec.normalizeCommitmentHash(request.commitmentHash());
        } catch (IllegalArgumentException ex) {
            throw new CommitSecurityException(
                    CommitSecurityError.BAD_REQUEST,
                    "Commitment hash format is invalid",
                    ex);
        }

        if (!request.hasCommitAuthEnvelope()) {
            if (commitSecurityProperties.isAllowLegacyUnsignedCommits()) {
                return new SecuredCommitmentPayload(normalizedHash, request.encryptedReveal());
            }
            throw new CommitSecurityException(
                    CommitSecurityError.UNAUTHORIZED,
                    "Signed commit envelope is required");
        }

        String normalizedNonce = normalizeRequestNonce(request.requestNonce());
        long signedAtEpochSeconds = request.signedAtEpochSeconds();
        validateSignedAtWindow(signedAtEpochSeconds);

        byte[] signature = decodeBase64Field("signature", request.signature());
        byte[] revealIv = decodeBase64Field("revealIv", request.revealIv());
        if (revealIv.length != GCM_IV_BYTES) {
            throw new CommitSecurityException(
                    CommitSecurityError.BAD_REQUEST,
                    "revealIv must decode to 12 bytes");
        }
        byte[] encryptedReveal = decodeBase64Field("encryptedReveal", request.encryptedReveal());

        byte[] authMessage = buildAuthMessage(
                request.wallet(),
                pairId,
                normalizedHash,
                request.stakeAmount(),
                signedAtEpochSeconds,
                normalizedNonce
        );

        if (!verifyWalletSignature(request.wallet(), authMessage, signature)) {
            throw new CommitSecurityException(
                    CommitSecurityError.UNAUTHORIZED,
                    "Commit signature verification failed");
        }

        reserveReplayNonce(request.wallet(), normalizedNonce, signedAtEpochSeconds);

        byte[] canonicalRevealPayload = decryptClientRevealPayload(signature, revealIv, encryptedReveal, authMessage);
        CommitmentCodec.RevealPayload revealPayload = CommitmentCodec.decodeRevealPayload(canonicalRevealPayload);

        String expectedHash = CommitmentCodec.computeCommitmentHash(
                request.wallet(),
                pairId,
                revealPayload.choice(),
                request.stakeAmount(),
                revealPayload.nonce()
        );
        if (!expectedHash.equals(normalizedHash)) {
            throw new CommitSecurityException(
                    CommitSecurityError.UNAUTHORIZED,
                    "Commit payload does not match commitment hash");
        }

        String storageEnvelope = commitRevealEnvelopeCryptoService.encryptForStorage(canonicalRevealPayload);
        return new SecuredCommitmentPayload(normalizedHash, storageEnvelope);
    }

    private void validateSignedAtWindow(long signedAtEpochSeconds) {
        Instant signedAt = Instant.ofEpochSecond(signedAtEpochSeconds);
        Instant now = Instant.now();

        if (signedAt.isBefore(now.minusSeconds(commitSecurityProperties.getMaxSignatureAgeSeconds()))) {
            throw new CommitSecurityException(
                    CommitSecurityError.BAD_REQUEST,
                    "Signed commit is too old");
        }
        if (signedAt.isAfter(now.plusSeconds(commitSecurityProperties.getMaxFutureSkewSeconds()))) {
            throw new CommitSecurityException(
                    CommitSecurityError.BAD_REQUEST,
                    "Signed commit timestamp is too far in the future");
        }
    }

    private void reserveReplayNonce(String wallet, String requestNonce, long signedAtEpochSeconds) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        replayGuardRepository.deleteByExpiresAtBefore(now);

        OffsetDateTime signedAt = OffsetDateTime.ofInstant(Instant.ofEpochSecond(signedAtEpochSeconds), ZoneOffset.UTC);
        OffsetDateTime expiresAt = signedAt.plusSeconds(commitSecurityProperties.getReplayWindowSeconds());

        CommitRequestReplayGuard guard = new CommitRequestReplayGuard();
        guard.setWallet(wallet);
        guard.setRequestNonce(requestNonce);
        guard.setSignedAt(signedAt);
        guard.setExpiresAt(expiresAt);

        try {
            replayGuardRepository.save(guard);
        } catch (DataIntegrityViolationException ex) {
            throw new CommitSecurityException(
                    CommitSecurityError.REPLAY,
                    "Commit nonce already used",
                    ex);
        }
    }

    private byte[] decryptClientRevealPayload(byte[] signature, byte[] iv, byte[] encryptedReveal, byte[] aad) {
        try {
            byte[] keyBytes = deriveClientKey(signature);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyBytes, "AES"), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(aad);
            return cipher.doFinal(encryptedReveal);
        } catch (Exception ex) {
            throw new CommitSecurityException(
                    CommitSecurityError.UNAUTHORIZED,
                    "Encrypted reveal payload failed authentication",
                    ex);
        }
    }

    private byte[] deriveClientKey(byte[] signature) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(signature);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to derive client reveal key", ex);
        }
    }

    private boolean verifyWalletSignature(String wallet, byte[] message, byte[] signature) {
        try {
            byte[] walletBytes = CommitmentCodec.decodeWalletPublicKey(wallet);
            Ed25519Signer verifier = new Ed25519Signer();
            verifier.init(false, new Ed25519PublicKeyParameters(walletBytes, 0));
            verifier.update(message, 0, message.length);
            return verifier.verifySignature(signature);
        } catch (IllegalArgumentException ex) {
            throw new CommitSecurityException(
                    CommitSecurityError.UNAUTHORIZED,
                    "Wallet public key is invalid",
                    ex);
        }
    }

    private static byte[] buildAuthMessage(
            String wallet,
            int pairId,
            String normalizedHash,
            long stakeAmount,
            long signedAtEpochSeconds,
            String requestNonce) {
        String payload = AUTH_MESSAGE_PREFIX
                + "|wallet=" + wallet
                + "|pairId=" + pairId
                + "|hash=" + normalizedHash
                + "|stake=" + stakeAmount
                + "|signedAt=" + signedAtEpochSeconds
                + "|nonce=" + requestNonce;
        return payload.getBytes(StandardCharsets.UTF_8);
    }

    public static String buildAuthMessageForSigning(
            String wallet,
            int pairId,
            String commitmentHash,
            long stakeAmount,
            long signedAtEpochSeconds,
            String requestNonce) {
        String normalizedHash = CommitmentCodec.normalizeCommitmentHash(commitmentHash);
        String normalizedNonce = normalizeRequestNonce(requestNonce);
        return new String(buildAuthMessage(
                wallet,
                pairId,
                normalizedHash,
                stakeAmount,
                signedAtEpochSeconds,
                normalizedNonce
        ), StandardCharsets.UTF_8);
    }

    private static String normalizeRequestNonce(String nonce) {
        if (nonce == null) {
            throw new CommitSecurityException(CommitSecurityError.BAD_REQUEST, "requestNonce is required");
        }

        String trimmed = nonce.trim().toLowerCase();
        if (!trimmed.matches("^[0-9a-f]+$")) {
            throw new CommitSecurityException(
                    CommitSecurityError.BAD_REQUEST,
                    "requestNonce must be lowercase hex");
        }
        if (trimmed.length() != REQUEST_NONCE_BYTES * 2) {
            throw new CommitSecurityException(
                    CommitSecurityError.BAD_REQUEST,
                    "requestNonce must be " + (REQUEST_NONCE_BYTES * 2) + " hex chars");
        }
        return trimmed;
    }

    private static byte[] decodeBase64Field(String fieldName, String value) {
        if (value == null || value.isBlank()) {
            throw new CommitSecurityException(
                    CommitSecurityError.BAD_REQUEST,
                    fieldName + " is required");
        }
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ex) {
            throw new CommitSecurityException(
                    CommitSecurityError.BAD_REQUEST,
                    "Invalid base64 value for " + fieldName,
                    ex);
        }
    }

    public record SecuredCommitmentPayload(
            String normalizedHash,
            String encryptedRevealForStorage
    ) {
    }

    public enum CommitSecurityError {
        BAD_REQUEST,
        UNAUTHORIZED,
        REPLAY
    }

    @Getter
    public static class CommitSecurityException extends RuntimeException {
        private final CommitSecurityError error;

        public CommitSecurityException(CommitSecurityError error, String message) {
            super(message);
            this.error = error;
        }

        public CommitSecurityException(CommitSecurityError error, String message, Throwable cause) {
            super(message, cause);
            this.error = error;
        }

    }
}
