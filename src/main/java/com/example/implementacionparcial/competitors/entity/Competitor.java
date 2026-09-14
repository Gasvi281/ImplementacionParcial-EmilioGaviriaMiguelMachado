package com.example.implementacionparcial.competitors.entity;

import com.example.implementacionparcial.teams.entity.TeamMember;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "competitors")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Competitor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompetitorType competitorType;

    @Column(nullable = false)
    private int age;

    @Column(nullable = false)
    private float height;

    @Column(nullable = false)
    private float weight;

    @Column(nullable = false, length = 150)
    private String placeOfOrigin;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CompetitorStatus competitorStatus = CompetitorStatus.ACTIVE;

    @Builder.Default
    @Column(nullable = false)
    private Date registrationDate = new Date();

    @Builder.Default
    @OneToMany(mappedBy = "competitor", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TeamMember> memberships = new ArrayList<>();
}

