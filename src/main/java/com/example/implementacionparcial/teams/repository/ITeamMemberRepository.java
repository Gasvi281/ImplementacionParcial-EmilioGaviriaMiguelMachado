package com.example.implementacionparcial.teams.repository;

import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamMember;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ITeamMemberRepository extends JpaRepository<TeamMember, UUID> {
    Optional<TeamMember> findByTeamIdAndCompetitorId(UUID teamId, UUID competitorId);
    boolean existsByTeamIdAndCompetitorId(UUID teamId, UUID competitorId);
    boolean existsByCompetitorIdAndTeam_Status(UUID competitorId, TeamStatus status);
    List<TeamMember> findByTeamId(UUID teamId);
    Optional<TeamMember> findByCompetitorIdAndTeam_Status(UUID competitorId, TeamStatus status);
}