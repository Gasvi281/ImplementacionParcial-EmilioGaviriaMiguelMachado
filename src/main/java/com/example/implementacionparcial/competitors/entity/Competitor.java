package com.example.implementacionparcial.competitors.entity;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * Minimal placeholder entity: this domain's full CRUD (dto/mapper/service/controller) is owned by
 * whoever implements {@code feature/competitors}. It exists here only so {@code registrations} can
 * validate competitor eligibility and team membership without depending on unfinished code.
 */
@Entity
@Table(name = "competitors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Competitor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompetitorType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private CompetitorStatus status = CompetitorStatus.ACTIVE;

    // Lado "N" de la relación 1-a-N con Team; el dueño real de este dominio puede
    // resolverla vía TeamMember si el enunciado lo requiere.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    private Team team;
}
