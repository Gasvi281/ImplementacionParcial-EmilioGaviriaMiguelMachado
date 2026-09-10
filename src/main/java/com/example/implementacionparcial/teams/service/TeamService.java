package com.example.implementacionparcial.teams.service;

import com.example.implementacionparcial.auditlog.service.AuditLogService;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.common.security.CurrentUser;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.repository.ICompetitorRepository;
import com.example.implementacionparcial.teams.dto.TeamMemberResponse;
import com.example.implementacionparcial.teams.dto.TeamRequest;
import com.example.implementacionparcial.teams.dto.TeamResponse;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamMember;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import com.example.implementacionparcial.teams.mapper.TeamMapper;
import com.example.implementacionparcial.teams.mapper.TeamMemberMapper;
import com.example.implementacionparcial.teams.repository.ITeamMemberRepository;
import com.example.implementacionparcial.teams.repository.ITeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamService {

    private final ICompetitorRepository competitorRepository;
    private final ITeamRepository teamRepository;
    private final ITeamMemberRepository teamMemberRepository;
    private final AuditLogService auditLogService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeams() {
        return teamRepository.findAll()
                .stream()
                .map(TeamMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID id) {
        Team team = findTeamOrThrow(id);
        return TeamMapper.toResponse(team);
    }

    @Transactional
    public TeamResponse createTeam(TeamRequest request) {
        validateNameNotDuplicated(request.name(), null);

        Team team = TeamMapper.toEntity(request);
        Team saved = teamRepository.save(team);

        auditLogService.record(currentUser.id(), "CREATE","Team", saved.getId(), null);
        return TeamMapper.toResponse(saved);
    }

    @Transactional
    public TeamResponse update(TeamRequest request, UUID id) {
        Team team = findTeamOrThrow(id);
        validateNameNotDuplicated(request.name(), id);

        team.setName(request.name());
        team.setDescription(request.description());
        team.setCoach(request.coach());
        team.setMaxMembers(request.maxMembers());

        Team updated = teamRepository.save(team);
        auditLogService.record(currentUser.id(), "UPDATE", "Team",updated.getId(), null);
        return TeamMapper.toResponse(updated);
    }

    @Transactional
    public TeamResponse deactivate(UUID id) {
        Team team = findTeamOrThrow(id);
        team.setStatus(TeamStatus.DISBANDED);

        Team deactivated = teamRepository.save(team);
        auditLogService.record(currentUser.id(), "DEACTIVATE", "Team", deactivated.getId(), TeamStatus.DISBANDED.name());
        return TeamMapper.toResponse(deactivated);
    }

    @Transactional
    public TeamMemberResponse addTeamMember(UUID teamId, UUID competitorId) {
        Team team = findTeamOrThrow(teamId);
        Competitor member = findCompetitorOrThrow(competitorId);

        if (team.getMembers().size() >= team.getMaxMembers()) {
            throw new ConflictException("Team with id " + teamId + " is already full");
        }

        if (teamMemberRepository.existsByTeamIdAndCompetitorId(teamId, competitorId)) {
            throw new ConflictException("Member " + competitorId + " already exists in team " + teamId);
        }

        if (teamMemberRepository.existsByCompetitorIdAndTeam_Status(competitorId, TeamStatus.ACTIVE)) {
            throw new ConflictException("Competitor " + competitorId + " already exists in an active team");
        }

        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .competitor(member)
                .build();

        TeamMember saved = teamMemberRepository.save(teamMember);
        auditLogService.record(currentUser.id(), "CREATE",
                "Team Member",
                saved.getId(),
                "Created with competitor " + competitorId + " for team " + teamId);
        return TeamMemberMapper.toResponse(saved);
    }

    @Transactional
    public void removeTeamMember(UUID teamId, UUID competitorId) {
        TeamMember teamMember = teamMemberRepository.findByTeamIdAndCompetitorId(teamId, competitorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Competitor " + competitorId + " is not a member of team " + teamId));

        teamMemberRepository.delete(teamMember);
        auditLogService.record(currentUser.id(),
                "DELETE",
                "Team member",
                teamMember.getId(), "Deleted competitor " + competitorId + " from team " + teamId);
    }

    @Transactional(readOnly = true)
    public Team getEligibleOrThrow(UUID id) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Team", id));
        if (team.getStatus() == TeamStatus.SUSPENDED) {
            throw new ConflictException("Team '%s' is not eligible (status: %s)"
                    .formatted(team.getId(), team.getStatus()));
        }
        return team;
    }

    @Transactional(readOnly = true)
    public UUID findCurrentActiveTeamId(UUID competitorId){
        TeamMember member = teamMemberRepository.findByCompetitorIdAndTeam_Status(competitorId, TeamStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Competitor " + competitorId + " doesn't exist in an active team"));
        return member.getTeam().getId();
    }

    private Team findTeamOrThrow(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Team", id));
    }

    private Competitor findCompetitorOrThrow(UUID id) {
        return competitorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", id));
    }

    private void validateNameNotDuplicated(String name, UUID id) {
        boolean exists = (id == null)
                ? teamRepository.existsByName(name)
                : teamRepository.existsByNameAndIdNot(name, id);

        if (exists) {
            throw new ConflictException("Name already exists: " + name);
        }
    }
}