package com.example.implementacionparcial.results.service;

import com.example.implementacionparcial.auditlog.service.AuditLogService;
import com.example.implementacionparcial.common.exceptions.BadRequestException;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.common.security.CurrentUser;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.competitors.repository.ICompetitorRepository;
import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.races.repository.IRaceRepository;
import com.example.implementacionparcial.registrations.repository.IRaceRegistrationRepository;
import com.example.implementacionparcial.results.dto.ResultRequest;
import com.example.implementacionparcial.results.dto.ResultResponse;
import com.example.implementacionparcial.results.dto.ResultUpdateRequest;
import com.example.implementacionparcial.results.entity.RaceResult;
import com.example.implementacionparcial.results.entity.ResultStatus;
import com.example.implementacionparcial.results.repository.IRaceResultRepository;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import com.example.implementacionparcial.teams.repository.ITeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResultServiceTest {

    @Mock
    private IRaceResultRepository resultRepository;
    @Mock
    private IRaceRepository raceRepository;
    @Mock
    private IRaceRegistrationRepository registrationRepository;
    @Mock
    private ICompetitorRepository competitorRepository;
    @Mock
    private ITeamRepository teamRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private ResultService resultService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(currentUser.id()).thenReturn(userId);
        lenient().when(resultRepository.save(any(RaceResult.class))).thenAnswer(invocation -> {
            RaceResult result = invocation.getArgument(0);
            if (result.getId() == null) {
                result.setId(UUID.randomUUID());
            }
            return result;
        });
    }

    private Race inProgressRace() {
        return Race.builder()
                .id(UUID.randomUUID())
                .name("Dune Dash")
                .type(RaceType.INDIVIDUAL)
                .status(RaceStatus.IN_PROGRESS)
                .build();
    }

    private Competitor activeCompetitor() {
        return Competitor.builder()
                .id(UUID.randomUUID())
                .name("Speedy")
                .nickname("speedy-" + UUID.randomUUID())
                .type(CompetitorType.CAMEL)
                .status(CompetitorStatus.ACTIVE)
                .build();
    }

    private Team activeTeam() {
        return Team.builder().id(UUID.randomUUID()).name("Dune Runners").status(TeamStatus.ACTIVE).build();
    }

    private void stubApprovedCompetitor(UUID raceId, UUID competitorId) {
        when(registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(eq(raceId), eq(competitorId), any()))
                .thenReturn(true);
    }

    private void stubApprovedTeam(UUID raceId, UUID teamId) {
        when(registrationRepository.existsByRace_IdAndTeam_IdAndStatusIn(eq(raceId), eq(teamId), any()))
                .thenReturn(true);
    }

    // ----- create -----

    @Test
    void create_rejectsWhenBothCompetitorAndTeamProvided() {
        ResultRequest request = new ResultRequest(
                UUID.randomUUID(), UUID.randomUUID(), ResultStatus.FINISHED, 1, 1, 100.0, 0.0, null);

        assertThatThrownBy(() -> resultService.create(UUID.randomUUID(), request))
                .isInstanceOf(BadRequestException.class);
        verify(raceRepository, never()).findById(any());
    }

    @Test
    void create_rejectsWhenNeitherCompetitorNorTeamProvided() {
        ResultRequest request = new ResultRequest(null, null, ResultStatus.FINISHED, 1, 1, 100.0, 0.0, null);

        assertThatThrownBy(() -> resultService.create(UUID.randomUUID(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_throwsNotFoundWhenRaceMissing() {
        UUID raceId = UUID.randomUUID();
        when(raceRepository.findById(raceId)).thenReturn(Optional.empty());
        ResultRequest request = new ResultRequest(UUID.randomUUID(), null, ResultStatus.FINISHED, 1, 1, 100.0, 0.0, null);

        assertThatThrownBy(() -> resultService.create(raceId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_rejectsWhenRaceIsNotInProgress() {
        Race race = inProgressRace();
        race.setStatus(RaceStatus.OPEN_FOR_REGISTRATION);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        ResultRequest request = new ResultRequest(UUID.randomUUID(), null, ResultStatus.FINISHED, 1, 1, 100.0, 0.0, null);

        assertThatThrownBy(() -> resultService.create(race.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_throwsNotFoundWhenCompetitorMissing() {
        Race race = inProgressRace();
        UUID competitorId = UUID.randomUUID();
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitorId)).thenReturn(Optional.empty());
        ResultRequest request = new ResultRequest(competitorId, null, ResultStatus.FINISHED, 1, 1, 100.0, 0.0, null);

        assertThatThrownBy(() -> resultService.create(race.getId(), request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_rejectsWhenRegistrationIsNotApproved() {
        Race race = inProgressRace();
        Competitor competitor = activeCompetitor();
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));
        when(registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(eq(race.getId()), eq(competitor.getId()), any()))
                .thenReturn(false);
        ResultRequest request = new ResultRequest(competitor.getId(), null, ResultStatus.FINISHED, 1, 1, 100.0, 0.0, null);

        assertThatThrownBy(() -> resultService.create(race.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsWhenCompetitorAlreadyHasAResultForTheRace() {
        Race race = inProgressRace();
        Competitor competitor = activeCompetitor();
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));
        stubApprovedCompetitor(race.getId(), competitor.getId());
        when(resultRepository.existsByRace_IdAndCompetitor_Id(race.getId(), competitor.getId())).thenReturn(true);
        ResultRequest request = new ResultRequest(competitor.getId(), null, ResultStatus.FINISHED, 1, 1, 100.0, 0.0, null);

        assertThatThrownBy(() -> resultService.create(race.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsNonPositiveCompletionTime() {
        Race race = inProgressRace();
        Competitor competitor = activeCompetitor();
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));
        stubApprovedCompetitor(race.getId(), competitor.getId());
        ResultRequest request = new ResultRequest(competitor.getId(), null, ResultStatus.FINISHED, 1, 1, -5.0, 0.0, null);

        assertThatThrownBy(() -> resultService.create(race.getId(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_rejectsFinishedWithoutFinalPosition() {
        Race race = inProgressRace();
        Competitor competitor = activeCompetitor();
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));
        stubApprovedCompetitor(race.getId(), competitor.getId());
        ResultRequest request = new ResultRequest(competitor.getId(), null, ResultStatus.FINISHED, 1, null, 100.0, 0.0, null);

        assertThatThrownBy(() -> resultService.create(race.getId(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_rejectsDuplicateFinalPositionAmongFinishers() {
        Race race = inProgressRace();
        Competitor competitor = activeCompetitor();
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));
        stubApprovedCompetitor(race.getId(), competitor.getId());
        when(resultRepository.existsByRace_IdAndFinalPositionAndStatus(race.getId(), 2, ResultStatus.FINISHED))
                .thenReturn(true);
        ResultRequest request = new ResultRequest(competitor.getId(), null, ResultStatus.FINISHED, 2, 2, 100.0, 0.0, null);

        assertThatThrownBy(() -> resultService.create(race.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsDisqualifiedAsWinner() {
        Race race = inProgressRace();
        Competitor competitor = activeCompetitor();
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));
        stubApprovedCompetitor(race.getId(), competitor.getId());
        ResultRequest request = new ResultRequest(competitor.getId(), null, ResultStatus.DISQUALIFIED, 1, 1, null, 0.0, null);

        assertThatThrownBy(() -> resultService.create(race.getId(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_rejectsSecondWinnerForTheSameRace() {
        Race race = inProgressRace();
        Competitor competitor = activeCompetitor();
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));
        stubApprovedCompetitor(race.getId(), competitor.getId());
        when(resultRepository.existsByRace_IdAndFinalPosition(race.getId(), 1)).thenReturn(true);
        ResultRequest request = new ResultRequest(competitor.getId(), null, ResultStatus.FINISHED, 1, 1, 100.0, 0.0, null);

        assertThatThrownBy(() -> resultService.create(race.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_savesValidFinishedResultAndRecordsAudit() {
        Race race = inProgressRace();
        Competitor competitor = activeCompetitor();
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorRepository.findById(competitor.getId())).thenReturn(Optional.of(competitor));
        stubApprovedCompetitor(race.getId(), competitor.getId());
        ResultRequest request = new ResultRequest(
                competitor.getId(), null, ResultStatus.FINISHED, 1, 1, 100.0, 5.0, "great run");

        ResultResponse response = resultService.create(race.getId(), request);

        assertThat(response.status()).isEqualTo(ResultStatus.FINISHED);
        assertThat(response.finalPosition()).isEqualTo(1);
        assertThat(response.totalTimeSeconds()).isEqualTo(105.0);
        assertThat(response.points()).isEqualTo(10);
        assertThat(response.competitor().id()).isEqualTo(competitor.getId());
        verify(auditLogService).record(eq(userId), eq("CREATE"), eq("RaceResult"), any(UUID.class), eq(null));
    }

    @Test
    void create_savesValidTeamResult() {
        Race race = inProgressRace();
        race.setType(RaceType.TEAM);
        Team team = activeTeam();
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(teamRepository.findById(team.getId())).thenReturn(Optional.of(team));
        stubApprovedTeam(race.getId(), team.getId());
        ResultRequest request = new ResultRequest(null, team.getId(), ResultStatus.DID_NOT_FINISH, 3, null, null, 0.0, null);

        ResultResponse response = resultService.create(race.getId(), request);

        assertThat(response.team().id()).isEqualTo(team.getId());
        assertThat(response.competitor()).isNull();
        assertThat(response.points()).isEqualTo(0);
    }

    // ----- update -----

    @Test
    void update_rejectsEmptyPatch() {
        ResultUpdateRequest empty = new ResultUpdateRequest(null, null, null, null, null, null);

        assertThatThrownBy(() -> resultService.update(UUID.randomUUID(), empty))
                .isInstanceOf(BadRequestException.class);
        verify(resultRepository, never()).findById(any());
    }

    @Test
    void update_throwsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(resultRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultService.update(id, new ResultUpdateRequest(null, null, 2, null, null, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_appliesPatchAndPersists() {
        Race race = inProgressRace();
        RaceResult existing = RaceResult.builder()
                .id(UUID.randomUUID())
                .race(race)
                .competitor(activeCompetitor())
                .status(ResultStatus.DID_NOT_FINISH)
                .penaltyTimeSeconds(0.0)
                .recordedByUserId(userId)
                .build();
        when(resultRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        ResultResponse response = resultService.update(existing.getId(),
                new ResultUpdateRequest(ResultStatus.FINISHED, null, 2, 120.0, 0.0, "corrected"));

        assertThat(response.status()).isEqualTo(ResultStatus.FINISHED);
        assertThat(response.finalPosition()).isEqualTo(2);
        assertThat(response.notes()).isEqualTo("corrected");
        verify(auditLogService).record(eq(userId), eq("UPDATE"), eq("RaceResult"), eq(existing.getId()), any(String.class));
    }

    @Test
    void update_rejectsBecomingSecondWinner() {
        Race race = inProgressRace();
        RaceResult existing = RaceResult.builder()
                .id(UUID.randomUUID())
                .race(race)
                .competitor(activeCompetitor())
                .status(ResultStatus.FINISHED)
                .finalPosition(2)
                .completionTimeSeconds(110.0)
                .penaltyTimeSeconds(0.0)
                .recordedByUserId(userId)
                .build();
        when(resultRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(resultRepository.existsByRace_IdAndFinalPositionAndIdNot(race.getId(), 1, existing.getId())).thenReturn(true);

        assertThatThrownBy(() -> resultService.update(
                existing.getId(), new ResultUpdateRequest(null, null, 1, null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    // ----- reads -----

    @Test
    void getById_throwsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(resultRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllForRace_throwsNotFoundWhenRaceMissing() {
        UUID raceId = UUID.randomUUID();
        when(raceRepository.existsById(raceId)).thenReturn(false);

        assertThatThrownBy(() -> resultService.getAllForRace(raceId, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
