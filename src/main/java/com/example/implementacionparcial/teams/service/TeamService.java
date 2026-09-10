package com.example.implementacionparcial.teams.service;

import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.repository.ICompetitorRepository;
import com.example.implementacionparcial.teams.dto.TeamMemberResponse;
import com.example.implementacionparcial.teams.dto.TeamRequest;
import com.example.implementacionparcial.teams.dto.TeamResponse;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamMember;
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

    @Transactional(readOnly = true)
    public List<TeamResponse> getTeams (){
        return teamRepository.findAll()
                .stream()
                .map(TeamMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById (UUID id){
        Team team = findTeamOrThrow(id);
        return TeamMapper.toResponse(team);
    }

    @Transactional
    public TeamResponse createTeam (TeamRequest request){
        validateNameNotDuplicated(request.name(), null);

        Team team = TeamMapper.toEntity(request);
        Team saved = teamRepository.save(team);

        log.info("Competitor created id={} with name={} and max capacity={}", saved.getId(), saved.getName(), saved.getMaxMembers());
        return TeamMapper.toResponse(saved);
    }

    @Transactional
    public TeamResponse update (TeamRequest request, UUID id){
        Team team = findTeamOrThrow(id);
        validateNameNotDuplicated(request.name(), id);

        team.setName(request.name());
        team.setDescription(request.description());
        team.setCoach(request.coach());
        team.setMaxMembers(request.maxMembers());

        Team updated = teamRepository.save(team);
        log.info("Competitor updated id={}", updated.getId());
        return TeamMapper.toResponse(updated);
    }

    @Transactional
    public TeamResponse deactivate (UUID id){
        Team team = findTeamOrThrow(id);

        team.setStatus("Inactive");

        Team deactivated = teamRepository.save(team);

        log.info("Team id={} has been deactivated", deactivated.getId());
        return TeamMapper.toResponse(deactivated);
    }

    @Transactional
    public TeamMemberResponse addTeamMember(UUID teamId, UUID competitorId){
        Team team = findTeamOrThrow(teamId);
        Competitor member = findCompetitorOrThrow(competitorId);

        if(team.getMembers().size() >= team.getMaxMembers()){
            throw new RuntimeException("Team with id " + teamId + " is already is full");
        }

        if(teamMemberRepository.existsByTeamIdAndCompetitorId(teamId, competitorId)){
            throw new RuntimeException("Member " + competitorId + " already exists in team " + teamId);
        }

        if(teamMemberRepository.existsByCompetitorIdAndTeam_Status(competitorId, "Active")){
            throw new RuntimeException("Competitor " + competitorId + " already exists in an active team");
        }

        TeamMember teamMember = TeamMember.builder()
                .team(team)
                .competitor(member)
                .build();

        TeamMember saved = teamMemberRepository.save(teamMember);

        return TeamMemberMapper.toResponse(saved);
    }

    @Transactional
    public void removeMember(UUID teamId, UUID competitorId){
        TeamMember teamMember = teamMemberRepository.findByTeamIdAndCompetitorId(teamId,competitorId)
                .orElseThrow(() -> new RuntimeException(
                        "Competitor " + competitorId + " is not a member of team " + teamId
                ));

        teamMemberRepository.delete(teamMember);
        log.info("Competitor id={} removed from team id={}", competitorId, teamId);
    }

    private Team findTeamOrThrow(UUID id) {
        return teamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Team not found with id: " + id));
    }

    private Competitor findCompetitorOrThrow(UUID id) {
        return competitorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + id));
    }

    private void validateNameNotDuplicated(String name, UUID id){
        boolean exists = (id == null)
                ? teamRepository.existsByName(name)
                : teamRepository.existsByNameAndIdNot(name, id);

        if (exists) {
            throw new RuntimeException("Name already exists: " + name);
        }
    }
}
