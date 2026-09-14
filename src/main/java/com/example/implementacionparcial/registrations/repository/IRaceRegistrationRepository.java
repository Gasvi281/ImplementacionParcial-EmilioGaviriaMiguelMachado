package com.example.implementacionparcial.registrations.repository;

import com.example.implementacionparcial.registrations.entity.RaceRegistration;
import com.example.implementacionparcial.registrations.entity.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.UUID;

public interface IRaceRegistrationRepository extends JpaRepository<RaceRegistration, UUID> {

    // Usado por RaceService para la regla "mínimo 2 APPROVED para arrancar" y por
    // RegistrationService para el tope de Race.maxParticipants al aprobar.
    long countByRace_IdAndStatus(UUID raceId, RegistrationStatus status);

    @Query("""
            SELECT rr FROM RaceRegistration rr
            WHERE rr.race.id = :raceId
              AND (:status IS NULL OR rr.status = :status)
            """)
    Page<RaceRegistration> search(@Param("raceId") UUID raceId,
                                   @Param("status") RegistrationStatus status,
                                   Pageable pageable);

    boolean existsByRace_IdAndCompetitor_IdAndStatusIn(UUID raceId, UUID competitorId, Collection<RegistrationStatus> statuses);

    boolean existsByRace_IdAndTeam_IdAndStatusIn(UUID raceId, UUID teamId, Collection<RegistrationStatus> statuses);

    boolean existsByRace_IdAndLaneAndIdNot(UUID raceId, Integer lane, UUID id);

    boolean existsByRace_IdAndStartPositionAndIdNot(UUID raceId, Integer startPosition, UUID id);
}
