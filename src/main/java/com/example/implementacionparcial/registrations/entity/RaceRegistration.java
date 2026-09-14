package com.example.implementacionparcial.registrations.entity;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Exactamente uno de {@code competitor}/{@code team} debe estar presente, nunca ambos ni ninguno.
 * Esa invariante se valida en {@link com.example.implementacionparcial.registrations.dto.RegistrationRequest#isValid()}
 * y se aplica en {@code RegistrationService}, no aquí.
 */
@Entity
@Table(name = "race_registrations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaceRegistration {

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RegistrationStatus status = RegistrationStatus.PENDING;

    // Asignados solo al aprobar; null mientras la inscripción esté PENDING/REJECTED/CANCELLED.
    @Column
    private Integer lane;

    @Column(name = "start_position")
    private Integer startPosition;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    private UUID registeredByUserId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime registeredAt;

    @PrePersist
    void onCreate() {
        registeredAt = LocalDateTime.now();
    }
}
