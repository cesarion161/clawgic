package com.moltrank.clawgic.repository;

import com.moltrank.clawgic.model.ClawgicAgentElo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClawgicAgentEloRepository extends JpaRepository<ClawgicAgentElo, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select agentElo from ClawgicAgentElo agentElo where agentElo.agentId = :agentId")
    Optional<ClawgicAgentElo> findByAgentIdForUpdate(@Param("agentId") UUID agentId);
}
