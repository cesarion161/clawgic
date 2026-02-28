package com.clawgic.repository;

import com.clawgic.model.GoldenSetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoldenSetItemRepository extends JpaRepository<GoldenSetItem, Integer> {
}
