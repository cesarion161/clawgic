package com.moltrank.clawgic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "clawgic_agents")
public class ClawgicAgent {

    @Id
    @Column(name = "agent_id", nullable = false, updatable = false)
    private UUID agentId;

    @Column(name = "wallet_address", nullable = false, length = 128)
    private String walletAddress;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "system_prompt", nullable = false, columnDefinition = "TEXT")
    private String systemPrompt;

    @Column(name = "skills_markdown", columnDefinition = "TEXT")
    private String skillsMarkdown;

    @Column(columnDefinition = "TEXT")
    private String persona;

    @Column(name = "agents_md_source", columnDefinition = "TEXT")
    private String agentsMdSource;

    @Column(name = "api_key_encrypted", nullable = false, columnDefinition = "TEXT")
    private String apiKeyEncrypted;

    @Column(name = "api_key_encryption_key_id", length = 64)
    private String apiKeyEncryptionKeyId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}

