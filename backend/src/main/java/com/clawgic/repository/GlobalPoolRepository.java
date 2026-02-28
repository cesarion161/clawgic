package com.clawgic.repository;

import com.clawgic.model.GlobalPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GlobalPoolRepository extends JpaRepository<GlobalPool, Integer> {
}
