package com.moltrank.repository;

import com.moltrank.model.Identity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdentityRepository extends JpaRepository<Identity, Integer> {
    Optional<Identity> findByWallet(String wallet);
    Optional<Identity> findByXAccount(String xAccount);
}
