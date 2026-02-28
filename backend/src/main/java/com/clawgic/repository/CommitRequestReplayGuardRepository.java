package com.clawgic.repository;

import com.clawgic.model.CommitRequestReplayGuard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

public interface CommitRequestReplayGuardRepository extends JpaRepository<CommitRequestReplayGuard, Integer> {
    void deleteByExpiresAtBefore(OffsetDateTime timestamp);
}
