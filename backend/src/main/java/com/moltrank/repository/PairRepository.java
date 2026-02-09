package com.moltrank.repository;

import com.moltrank.model.Pair;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PairRepository extends JpaRepository<Pair, Integer> {
    List<Pair> findByRoundId(Integer roundId);
    List<Pair> findByIsGolden(Boolean isGolden);
    List<Pair> findByIsAudit(Boolean isAudit);
}
