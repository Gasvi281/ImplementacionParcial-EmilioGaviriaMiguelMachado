package com.example.implementacionparcial.results.repository;

import com.example.implementacionparcial.results.entity.RaceResult;
import com.example.implementacionparcial.results.entity.ResultStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IRaceResultRepository extends JpaRepository<RaceResult, UUID> {

    @Query("""
            SELECT r FROM RaceResult r
            WHERE r.race.id = :raceId
              AND (:status IS NULL OR r.status = :status)
            """)
    Page<RaceResult> search(@Param("raceId") UUID raceId,
                             @Param("status") ResultStatus status,
                             Pageable pageable);

    // Usado por RaceService para la regla "no completar sin resultados oficiales": una carrera
    // tiene resultado oficial cuando existe un ganador (finalPosition == 1) registrado.
    boolean existsByRace_IdAndFinalPosition(UUID raceId, Integer finalPosition);

    boolean existsByRace_IdAndFinalPositionAndIdNot(UUID raceId, Integer finalPosition, UUID id);

    boolean existsByRace_IdAndFinalPositionAndStatus(UUID raceId, Integer finalPosition, ResultStatus status);

    boolean existsByRace_IdAndFinalPositionAndStatusAndIdNot(UUID raceId, Integer finalPosition,
                                                              ResultStatus status, UUID id);

    boolean existsByRace_IdAndCompetitor_Id(UUID raceId, UUID competitorId);

    boolean existsByRace_IdAndTeam_Id(UUID raceId, UUID teamId);

    // Usado por StandingService: trae todo en una sola consulta (evita N+1 al resolver
    // competitor/team/race de cada resultado durante la agregación).
    @Query("""
            SELECT r FROM RaceResult r
            JOIN FETCH r.race
            LEFT JOIN FETCH r.competitor
            LEFT JOIN FETCH r.team
            """)
    List<RaceResult> findAllForStandings();
}
