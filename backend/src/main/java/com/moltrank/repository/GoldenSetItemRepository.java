package com.moltrank.repository;

import com.moltrank.model.GoldenSetItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GoldenSetItemRepository extends JpaRepository<GoldenSetItem, Integer> {
}
