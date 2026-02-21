package com.moltrank.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@Entity
@Table(
        name = "commit_request_replay_guard",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_commit_replay_wallet_nonce",
                columnNames = {"wallet", "request_nonce"}
        )
)
public class CommitRequestReplayGuard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 44)
    private String wallet;

    @Column(name = "request_nonce", nullable = false, length = 64)
    private String requestNonce;

    @Column(name = "signed_at", nullable = false)
    private OffsetDateTime signedAt;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
