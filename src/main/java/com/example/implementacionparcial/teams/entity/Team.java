package com.example.implementacionparcial.teams.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "teams")
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    UUID id;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, length = 100)
    private String coach;

    @Column(nullable = false)
    private int maxMembers;

    @Builder.Default
    @Column(nullable = false)
    private Date creationDate = new Date();

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private TeamStatus status = TeamStatus.ACTIVE;

    @Builder.Default
    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<TeamMember> members = new ArrayList<>();
}
