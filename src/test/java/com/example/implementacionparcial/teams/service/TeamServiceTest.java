package com.example.implementacionparcial.teams.service;

import com.example.implementacionparcial.auditlog.service.AuditLogService;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.common.security.CurrentUser;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.competitors.repository.ICompetitorRepository;
import com.example.implementacionparcial.teams.dto.TeamMemberResponse;
import com.example.implementacionparcial.teams.dto.TeamRequest;
import com.example.implementacionparcial.teams.dto.TeamResponse;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamMember;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import com.example.implementacionparcial.teams.repository.ITeamMemberRepository;
import com.example.implementacionparcial.teams.repository.ITeamRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private ICompetitorRepository competitorRepository;
    @Mock private ITeamRepository teamRepository;
    @Mock private ITeamMemberRepository teamMemberRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private CurrentUser currentUser;

    @InjectMocks
    private TeamService teamService;

    private final UUID userId = UUID.randomUUID();

    private Team sampleTeam(UUID id, int maxMembers) {
        return Team.builder()
                .id(id)
                .name("Enanos del valle")
                .description("Un equipo conformado por jugadores retirados")
                .coach("Chiqui Tapia")
                .maxMembers(maxMembers)
                .status(TeamStatus.ACTIVE)
                .build();
    }

    private Competitor sampleCompetitor(UUID id) {
        return Competitor.builder()
                .id(id).name("Javier").nickname("javi123")
                .competitorType(CompetitorType.DWARF).age(30).height(1.2f).weight(60f)
                .placeOfOrigin("Medellín").competitorStatus(CompetitorStatus.ACTIVE)
                .build();
    }

    @Test
    void createTeam_throwsConflict_whenNameDuplicated() {
        TeamRequest request = new TeamRequest("Enanos del valle", "desc de al menos treinta caracteres largos", "Chiqui Tapia", 8);
        when(teamRepository.existsByName(request.name())).thenReturn(true);

        assertThatThrownBy(() -> teamService.createTeam(request))
                .isInstanceOf(ConflictException.class);
        verify(teamRepository, never()).save(any());
    }

    @Test
    void createTeam_mapsMaxMembersCorrectly_recordsAudit() {
        TeamRequest request = new TeamRequest("Enanos del valle", "desc de al menos treinta caracteres largos", "Chiqui Tapia", 8);
        when(teamRepository.existsByName(request.name())).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currentUser.id()).thenReturn(userId);

        TeamResponse response = teamService.createTeam(request);

        assertThat(response.maxMembers()).isEqualTo(8); // falla si Bug 1 sigue presente
        verify(auditLogService).record(eq(userId), eq("CREATE"), eq("Team"), any(UUID.class), eq(null));
    }

    @Test
    void addTeamMember_throwsConflict_whenTeamIsFull() {
        UUID teamId = UUID.randomUUID();
        Team fullTeam = sampleTeam(teamId, 0); // maxMembers=0, members vacío -> 0 >= 0
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(fullTeam));
        when(competitorRepository.findById(any())).thenReturn(Optional.of(sampleCompetitor(UUID.randomUUID())));

        assertThatThrownBy(() -> teamService.addTeamMember(teamId, UUID.randomUUID()))
                .isInstanceOf(ConflictException.class);
        verify(teamMemberRepository, never()).save(any());
    }

    @Test
    void addTeamMember_throwsConflict_whenAlreadyMemberOfSameTeam() {
        UUID teamId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        Team team = sampleTeam(teamId, 5);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(competitorRepository.findById(competitorId)).thenReturn(Optional.of(sampleCompetitor(competitorId)));
        when(teamMemberRepository.existsByTeamIdAndCompetitorId(teamId, competitorId)).thenReturn(true);

        assertThatThrownBy(() -> teamService.addTeamMember(teamId, competitorId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void addTeamMember_throwsConflict_whenCompetitorAlreadyInActiveTeam() {
        UUID teamId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        Team team = sampleTeam(teamId, 5);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(competitorRepository.findById(competitorId)).thenReturn(Optional.of(sampleCompetitor(competitorId)));
        when(teamMemberRepository.existsByTeamIdAndCompetitorId(teamId, competitorId)).thenReturn(false);
        when(teamMemberRepository.existsByCompetitorIdAndTeam_Status(competitorId, TeamStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> teamService.addTeamMember(teamId, competitorId))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void addTeamMember_success_returnsCorrectIdsAndRecordsAudit() {
        UUID teamId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        Team team = sampleTeam(teamId, 5);
        Competitor competitor = sampleCompetitor(competitorId);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(competitorRepository.findById(competitorId)).thenReturn(Optional.of(competitor));
        when(teamMemberRepository.existsByTeamIdAndCompetitorId(teamId, competitorId)).thenReturn(false);
        when(teamMemberRepository.existsByCompetitorIdAndTeam_Status(competitorId, TeamStatus.ACTIVE)).thenReturn(false);
        when(teamMemberRepository.save(any(TeamMember.class))).thenAnswer(inv -> {
            TeamMember tm = inv.getArgument(0);
            tm.setId(UUID.randomUUID());
            return tm;
        });
        when(currentUser.id()).thenReturn(userId);

        TeamMemberResponse response = teamService.addTeamMember(teamId, competitorId);

        assertThat(response.team_id()).isEqualTo(teamId);           // falla si Bug 2 sigue presente
        assertThat(response.competitor_id()).isEqualTo(competitorId); // falla si Bug 2 sigue presente
        verify(auditLogService).record(eq(userId), eq("CREATE"), eq("Team Member"), any(UUID.class), any(String.class));
    }

    @Test
    void removeTeamMember_throwsResourceNotFound_whenMembershipMissing() {
        UUID teamId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        when(teamMemberRepository.findByTeamIdAndCompetitorId(teamId, competitorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService.removeTeamMember(teamId, competitorId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(teamMemberRepository, never()).delete(any());
    }
}