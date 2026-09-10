package com.example.implementacionparcial.results.entity;

import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.teams.entity.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Exactamente uno de {@code competitor}/{@code team} debe estar presente, nunca ambos ni ninguno —
 * misma restricción de exclusividad que en
 * {@link com.example.implementacionparcial.registrations.entity.RaceRegistration}, validada en
 * {@link com.example.implementacionparcial.results.dto.ResultRequest#isValid()} y aplicada en
 * {@code ResultService}, no aquí.
 *
 * <p>No hay una entidad separada para "Standing": los standings se calculan a partir de esta
 * entidad en {@code StandingService}, no se persisten (ver {@code .claude/rules/results.md}).
 */
@Entity
@Table(name = "race_results")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "race_id", nullable = false)
    private Race race;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitor_id")
    private Competitor competitor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;

    @Column(name = "start_position")
    private Integer startPosition;

    // Nulo si el resultado es DISQUALIFIED/DID_NOT_FINISH/DID_NOT_START.
    @Column(name = "final_position")
    private Integer finalPosition;

    @Column(name = "completion_time_seconds")
    private Double completionTimeSeconds;

    @Column(name = "penalty_time_seconds", nullable = false)
    @Builder.Default
    private Double penaltyTimeSeconds = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultStatus status;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private UUID recordedByUserId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        recordedAt = LocalDateTime.now();
        updatedAt = recordedAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
