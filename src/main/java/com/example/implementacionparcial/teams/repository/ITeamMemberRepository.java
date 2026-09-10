package com.example.implementacionparcial.teams.repository;

import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ITeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    Optional<TeamMember> findByTeamIdAndCompetitorId(UUID teamId, UUID competitorId);
    boolean existsByTeamIdAndCompetitorId(UUID teamId, UUID competitorId);
    boolean existsByCompetitorIdAndTeam_Status(UUID competitorId, String status);

    TeamMember team(Team team);
}
