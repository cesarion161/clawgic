package com.moltrank.clawgic.repository;

import com.moltrank.clawgic.model.ClawgicUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClawgicUserRepository extends JpaRepository<ClawgicUser, String> {
}

