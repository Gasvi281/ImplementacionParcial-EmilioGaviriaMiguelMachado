package com.example.implementacionparcial.teams.entity;

import com.example.implementacionparcial.competitors.entity.Competitor;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "teamMembers")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "competitor_id", nullable = false)
    private Competitor competitor;

    @Builder.Default
    @Column(nullable = false)
    private Date joinedAt = new Date();
}
