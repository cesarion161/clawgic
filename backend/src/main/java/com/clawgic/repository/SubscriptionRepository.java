package com.clawgic.repository;

import com.clawgic.model.Subscription;
import com.clawgic.model.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Integer> {
    List<Subscription> findByReaderWallet(String readerWallet);
    List<Subscription> findByMarketId(Integer marketId);
    List<Subscription> findByType(SubscriptionType type);
}
