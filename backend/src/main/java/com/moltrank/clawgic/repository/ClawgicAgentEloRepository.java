package com.moltrank.clawgic.repository;

import com.moltrank.clawgic.model.ClawgicAgentElo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ClawgicAgentEloRepository extends JpaRepository<ClawgicAgentElo, UUID> {
}

