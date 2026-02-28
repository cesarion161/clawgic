package com.moltrank.clawgic.service;

import com.moltrank.clawgic.config.X402Properties;
import com.moltrank.clawgic.model.ClawgicPaymentAuthorization;
import com.moltrank.clawgic.model.ClawgicPaymentAuthorizationStatus;
import com.moltrank.clawgic.repository.ClawgicPaymentAuthorizationRepository;
import com.moltrank.clawgic.web.X402PaymentRequestException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class X402PaymentAuthorizationAttemptService {

    private final ClawgicPaymentAuthorizationRepository clawgicPaymentAuthorizationRepository;
    private final X402PaymentHeaderParser x402PaymentHeaderParser;
    private final X402Eip3009SignatureVerifier x402Eip3009SignatureVerifier;
    private final X402Properties x402Properties;

    public X402PaymentAuthorizationAttemptService(
            ClawgicPaymentAuthorizationRepository clawgicPaymentAuthorizationRepository,
            X402PaymentHeaderParser x402PaymentHeaderParser,
            X402Eip3009SignatureVerifier x402Eip3009SignatureVerifier,
            X402Properties x402Properties
    ) {
        this.clawgicPaymentAuthorizationRepository = clawgicPaymentAuthorizationRepository;
        this.x402PaymentHeaderParser = x402PaymentHeaderParser;
        this.x402Eip3009SignatureVerifier = x402Eip3009SignatureVerifier;
        this.x402Properties = x402Properties;
    }

    @Transactional
    public ClawgicPaymentAuthorization recordPendingVerificationAttempt(
            UUID tournamentId,
            UUID agentId,
            String walletAddress,
            BigDecimal fallbackAmountUsdc,
            String paymentHeaderValue
    ) {
        X402PaymentHeaderParser.ParsedX402PaymentHeader parsedHeader =
                x402PaymentHeaderParser.parse(paymentHeaderValue);

        if (clawgicPaymentAuthorizationRepository.existsByWalletAddressAndRequestNonce(
                walletAddress,
                parsedHeader.requestNonce()
        )) {
            throw X402PaymentRequestException.replayRejected(
                    "Duplicate request nonce for wallet: " + parsedHeader.requestNonce()
            );
        }
        if (clawgicPaymentAuthorizationRepository.existsByWalletAddressAndIdempotencyKey(
                walletAddress,
                parsedHeader.idempotencyKey()
        )) {
            throw X402PaymentRequestException.replayRejected(
                    "Duplicate idempotency key for wallet: " + parsedHeader.idempotencyKey()
            );
        }

        OffsetDateTime now = OffsetDateTime.now();
        ClawgicPaymentAuthorization authorization = new ClawgicPaymentAuthorization();
        authorization.setPaymentAuthorizationId(UUID.randomUUID());
        authorization.setTournamentId(tournamentId);
        authorization.setAgentId(agentId);
        authorization.setWalletAddress(walletAddress);
        authorization.setRequestNonce(parsedHeader.requestNonce());
        authorization.setIdempotencyKey(parsedHeader.idempotencyKey());
        authorization.setAuthorizationNonce(parsedHeader.authorizationNonce());
        authorization.setStatus(ClawgicPaymentAuthorizationStatus.PENDING_VERIFICATION);
        authorization.setPaymentHeaderJson(parsedHeader.rawJson());
        authorization.setAmountAuthorizedUsdc(scaleUsdc(
                parsedHeader.amountUsdc() != null ? parsedHeader.amountUsdc() : fallbackAmountUsdc
        ));
        authorization.setChainId(parsedHeader.chainId() != null ? parsedHeader.chainId() : x402Properties.getChainId());
        authorization.setRecipientWalletAddress(
                parsedHeader.recipient() != null ? parsedHeader.recipient() : x402Properties.getSettlementAddress()
        );
        authorization.setChallengeExpiresAt(now.plusSeconds(Math.max(1L, x402Properties.getNonceTtlSeconds())));
        authorization.setReceivedAt(now);
        authorization.setCreatedAt(now);
        authorization.setUpdatedAt(now);

        try {
            return clawgicPaymentAuthorizationRepository.saveAndFlush(authorization);
        } catch (DataIntegrityViolationException ex) {
            throw X402PaymentRequestException.replayRejected(
                    "Duplicate payment authorization replay key for wallet"
            );
        }
    }

    @Transactional(noRollbackFor = X402PaymentRequestException.class)
    public ClawgicPaymentAuthorization verifyAndPersistAuthorizationOutcome(
            UUID paymentAuthorizationId,
            String expectedWalletAddress,
            BigDecimal expectedAmountUsdc
    ) {
        ClawgicPaymentAuthorization authorization = clawgicPaymentAuthorizationRepository
                .findById(paymentAuthorizationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Payment authorization not found: " + paymentAuthorizationId
                ));

        OffsetDateTime now = OffsetDateTime.now();
        try {
            X402Eip3009SignatureVerifier.VerifiedTransferWithAuthorization verifiedAuthorization =
                    x402Eip3009SignatureVerifier.verify(
                            authorization.getPaymentHeaderJson(),
                            expectedWalletAddress,
                            expectedAmountUsdc
                    );

            authorization.setStatus(ClawgicPaymentAuthorizationStatus.AUTHORIZED);
            authorization.setAmountAuthorizedUsdc(scaleUsdc(verifiedAuthorization.amountUsdc()));
            authorization.setChainId(verifiedAuthorization.chainId());
            authorization.setRecipientWalletAddress(verifiedAuthorization.to());
            authorization.setAuthorizationNonce(verifiedAuthorization.nonceHex());
            authorization.setFailureReason(null);
            authorization.setVerifiedAt(now);
            authorization.setUpdatedAt(now);
            return clawgicPaymentAuthorizationRepository.saveAndFlush(authorization);
        } catch (X402PaymentRequestException ex) {
            authorization.setStatus(ClawgicPaymentAuthorizationStatus.REJECTED);
            authorization.setFailureReason(ex.getMessage());
            authorization.setUpdatedAt(now);
            clawgicPaymentAuthorizationRepository.saveAndFlush(authorization);
            throw ex;
        }
    }

    private BigDecimal scaleUsdc(BigDecimal value) {
        return value.setScale(6, RoundingMode.HALF_UP);
    }
}
