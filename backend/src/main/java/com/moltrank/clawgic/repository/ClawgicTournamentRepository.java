package com.moltrank.clawgic.repository;

import com.moltrank.clawgic.model.ClawgicTournament;
import com.moltrank.clawgic.model.ClawgicTournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ClawgicTournamentRepository extends JpaRepository<ClawgicTournament, UUID> {
    List<ClawgicTournament> findByStatusAndStartTimeAfterOrderByStartTimeAsc(
            ClawgicTournamentStatus status,
            OffsetDateTime now
    );
}
