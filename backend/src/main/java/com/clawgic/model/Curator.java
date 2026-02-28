package com.clawgic.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "curator")
@IdClass(CuratorId.class)
public class Curator {
    @Id
    @Column(length = 44)
    private String wallet;

    @Id
    @Column(name = "market_id")
    private Integer marketId;

    @Column(name = "identity_id", nullable = false)
    private Integer identityId;

    @Column(nullable = false)
    private Long earned = 0L;

    @Column(nullable = false)
    private Long lost = 0L;

    @Column(name = "curator_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal curatorScore = BigDecimal.ZERO;

    @Column(name = "calibration_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal calibrationRate = BigDecimal.ZERO;

    @Column(name = "audit_pass_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal auditPassRate = BigDecimal.ZERO;

    @Column(name = "alignment_stability", nullable = false, precision = 5, scale = 4)
    private BigDecimal alignmentStability = BigDecimal.ZERO;

    @Column(name = "fraud_flags", nullable = false)
    private Integer fraudFlags = 0;

    @Column(name = "pairs_this_epoch", nullable = false)
    private Integer pairsThisEpoch = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public String getWallet() {
        return wallet;
    }

    public void setWallet(String wallet) {
        this.wallet = wallet;
    }

    public Integer getMarketId() {
        return marketId;
    }

    public void setMarketId(Integer marketId) {
        this.marketId = marketId;
    }

    public Integer getIdentityId() {
        return identityId;
    }

    public void setIdentityId(Integer identityId) {
        this.identityId = identityId;
    }

    public Long getEarned() {
        return earned;
    }

    public void setEarned(Long earned) {
        this.earned = earned;
    }

    public Long getLost() {
        return lost;
    }

    public void setLost(Long lost) {
        this.lost = lost;
    }

    public BigDecimal getCuratorScore() {
        return curatorScore;
    }

    public void setCuratorScore(BigDecimal curatorScore) {
        this.curatorScore = curatorScore;
    }

    public BigDecimal getCalibrationRate() {
        return calibrationRate;
    }

    public void setCalibrationRate(BigDecimal calibrationRate) {
        this.calibrationRate = calibrationRate;
    }

    public BigDecimal getAuditPassRate() {
        return auditPassRate;
    }

    public void setAuditPassRate(BigDecimal auditPassRate) {
        this.auditPassRate = auditPassRate;
    }

    public BigDecimal getAlignmentStability() {
        return alignmentStability;
    }

    public void setAlignmentStability(BigDecimal alignmentStability) {
        this.alignmentStability = alignmentStability;
    }

    public Integer getFraudFlags() {
        return fraudFlags;
    }

    public void setFraudFlags(Integer fraudFlags) {
        this.fraudFlags = fraudFlags;
    }

    public Integer getPairsThisEpoch() {
        return pairsThisEpoch;
    }

    public void setPairsThisEpoch(Integer pairsThisEpoch) {
        this.pairsThisEpoch = pairsThisEpoch;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
