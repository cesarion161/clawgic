package com.moltrank.clawgic.repository;

import com.moltrank.clawgic.model.ClawgicAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClawgicAgentRepository extends JpaRepository<ClawgicAgent, UUID> {
    List<ClawgicAgent> findByWalletAddress(String walletAddress);
    List<ClawgicAgent> findByWalletAddressOrderByCreatedAtDesc(String walletAddress);
}
