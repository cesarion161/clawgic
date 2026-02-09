package com.moltrank.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "market")
public class Market {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "submolt_id", nullable = false, length = 255)
    private String submoltId;

    @Column(name = "subscription_revenue", nullable = false)
    private Long subscriptionRevenue = 0L;

    @Column(nullable = false)
    private Integer subscribers = 0;

    @Column(name = "creation_bond", nullable = false)
    private Long creationBond = 0L;

    @Column(name = "max_pairs", nullable = false)
    private Integer maxPairs = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubmoltId() {
        return submoltId;
    }

    public void setSubmoltId(String submoltId) {
        this.submoltId = submoltId;
    }

    public Long getSubscriptionRevenue() {
        return subscriptionRevenue;
    }

    public void setSubscriptionRevenue(Long subscriptionRevenue) {
        this.subscriptionRevenue = subscriptionRevenue;
    }

    public Integer getSubscribers() {
        return subscribers;
    }

    public void setSubscribers(Integer subscribers) {
        this.subscribers = subscribers;
    }

    public Long getCreationBond() {
        return creationBond;
    }

    public void setCreationBond(Long creationBond) {
        this.creationBond = creationBond;
    }

    public Integer getMaxPairs() {
        return maxPairs;
    }

    public void setMaxPairs(Integer maxPairs) {
        this.maxPairs = maxPairs;
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
