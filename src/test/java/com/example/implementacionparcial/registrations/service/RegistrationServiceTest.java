package com.example.implementacionparcial.registrations.service;

import com.example.implementacionparcial.auditlog.service.AuditLogService;
import com.example.implementacionparcial.common.exceptions.BadRequestException;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.common.security.CurrentUser;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.competitors.service.CompetitorService;
import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.races.repository.IRaceRepository;
import com.example.implementacionparcial.registrations.dto.RegistrationRejectionRequest;
import com.example.implementacionparcial.registrations.dto.RegistrationRequest;
import com.example.implementacionparcial.registrations.dto.RegistrationResponse;
import com.example.implementacionparcial.registrations.dto.RegistrationUpdateRequest;
import com.example.implementacionparcial.registrations.entity.RaceRegistration;
import com.example.implementacionparcial.registrations.entity.RegistrationStatus;
import com.example.implementacionparcial.registrations.repository.IRaceRegistrationRepository;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import com.example.implementacionparcial.teams.service.TeamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
class RegistrationServiceTest {

    @Mock
    private IRaceRegistrationRepository registrationRepository;
    @Mock
    private IRaceRepository raceRepository;
    @Mock
    private CompetitorService competitorService;
    @Mock
    private TeamService teamService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private RegistrationService registrationService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(currentUser.id()).thenReturn(userId);
        lenient().when(registrationRepository.save(any(RaceRegistration.class))).thenAnswer(invocation -> {
            RaceRegistration registration = invocation.getArgument(0);
            if (registration.getId() == null) {
                registration.setId(UUID.randomUUID());
            }
            return registration;
        });
    }

    private Race openRace(RaceType type, int maxParticipants) {
        return Race.builder()
                .id(UUID.randomUUID())
                .name("Dune Dash")
                .scheduledAt(LocalDateTime.now().plusDays(10))
                .type(type)
                .status(RaceStatus.OPEN_FOR_REGISTRATION)
                .maxParticipants(maxParticipants)
                .registrationDeadline(LocalDateTime.now().plusDays(5))
                .build();
    }

    private Competitor activeCompetitor(Team team) {
        return Competitor.builder()
                .id(UUID.randomUUID())
                .name("Speedy")
                .nickname("speedy-" + UUID.randomUUID())
                .competitorType(CompetitorType.CAMEL)
                .competitorStatus(CompetitorStatus.ACTIVE)
                //.team(team)
                .build();
    }

    private Team activeTeam() {
        return Team.builder().id(UUID.randomUUID()).name("Dune Runners").status(TeamStatus.ACTIVE).build();
    }

    private RaceRegistration pendingRegistration(Race race) {
        return RaceRegistration.builder()
                .id(UUID.randomUUID())
                .race(race)
                .status(RegistrationStatus.PENDING)
                .registeredByUserId(userId)
                .registeredAt(LocalDateTime.now())
                .build();
    }

    // ----- create -----

    @Test
    void create_rejectsWhenBothCompetitorAndTeamProvided() {
        RegistrationRequest request = new RegistrationRequest(UUID.randomUUID(), UUID.randomUUID(), null);

        assertThatThrownBy(() -> registrationService.create(UUID.randomUUID(), request))
                .isInstanceOf(BadRequestException.class);
        verify(raceRepository, never()).findById(any());
    }

    @Test
    void create_rejectsWhenNeitherCompetitorNorTeamProvided() {
        RegistrationRequest request = new RegistrationRequest(null, null, null);

        assertThatThrownBy(() -> registrationService.create(UUID.randomUUID(), request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_throwsNotFoundWhenRaceMissing() {
        UUID raceId = UUID.randomUUID();
        when(raceRepository.findById(raceId)).thenReturn(Optional.empty());
        RegistrationRequest request = new RegistrationRequest(UUID.randomUUID(), null, null);

        assertThatThrownBy(() -> registrationService.create(raceId, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_rejectsWhenRaceIsNotOpenForRegistration() {
        Race race = openRace(RaceType.INDIVIDUAL, 10);
        race.setStatus(RaceStatus.DRAFT);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        RegistrationRequest request = new RegistrationRequest(UUID.randomUUID(), null, null);

        assertThatThrownBy(() -> registrationService.create(race.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsWhenRegistrationDeadlineHasPassed() {
        Race race = openRace(RaceType.INDIVIDUAL, 10);
        race.setRegistrationDeadline(LocalDateTime.now().minusDays(1));
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        RegistrationRequest request = new RegistrationRequest(UUID.randomUUID(), null, null);

        assertThatThrownBy(() -> registrationService.create(race.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsIndividualRegistrationOnTeamOnlyRace() {
        Race race = openRace(RaceType.TEAM, 10);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        RegistrationRequest request = new RegistrationRequest(UUID.randomUUID(), null, null);

        assertThatThrownBy(() -> registrationService.create(race.getId(), request))
                .isInstanceOf(BadRequestException.class);
        verify(competitorService, never()).getEligibleOrThrow(any());
    }

    @Test
    void create_rejectsTeamRegistrationOnIndividualOnlyRace() {
        Race race = openRace(RaceType.INDIVIDUAL, 10);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        RegistrationRequest request = new RegistrationRequest(null, UUID.randomUUID(), null);

        assertThatThrownBy(() -> registrationService.create(race.getId(), request))
                .isInstanceOf(BadRequestException.class);
        verify(teamService, never()).getEligibleOrThrow(any());
    }

    @Test
    void create_rejectsDuplicateIndividualRegistration() {
        Race race = openRace(RaceType.INDIVIDUAL, 10);
        Competitor competitor = activeCompetitor(null);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorService.getEligibleOrThrow(competitor.getId())).thenReturn(competitor);
        when(registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(eq(race.getId()), eq(competitor.getId()), any()))
                .thenReturn(true);
        RegistrationRequest request = new RegistrationRequest(competitor.getId(), null, null);

        assertThatThrownBy(() -> registrationService.create(race.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsIndividualAlreadyRegisteredAsTeamMember() {
        Race race = openRace(RaceType.MIXED, 10);
        Team team = activeTeam();
        Competitor competitor = activeCompetitor(team);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorService.getEligibleOrThrow(competitor.getId())).thenReturn(competitor);
        when(registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(eq(race.getId()), eq(competitor.getId()), any()))
                .thenReturn(false);
        when(registrationRepository.existsByRace_IdAndTeam_IdAndStatusIn(eq(race.getId()), eq(team.getId()), any()))
                .thenReturn(true);
        RegistrationRequest request = new RegistrationRequest(competitor.getId(), null, null);

        assertThatThrownBy(() -> registrationService.create(race.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_savesValidIndividualRegistration() {
        Race race = openRace(RaceType.INDIVIDUAL, 10);
        Competitor competitor = activeCompetitor(null);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(competitorService.getEligibleOrThrow(competitor.getId())).thenReturn(competitor);
        RegistrationRequest request = new RegistrationRequest(competitor.getId(), null, "notes");

        RegistrationResponse response = registrationService.create(race.getId(), request);

        assertThat(response.status()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(response.competitor().id()).isEqualTo(competitor.getId());
        verify(auditLogService).record(eq(userId), eq("CREATE"), eq("RaceRegistration"), any(UUID.class), eq(null));
    }

    @Test
    void create_rejectsTeamWithIneligibleMember() {
        Race race = openRace(RaceType.TEAM, 10);
        Team team = activeTeam();
        Competitor injured = Competitor.builder()
                .id(UUID.randomUUID()).name("Hurt").nickname("hurt").competitorType(CompetitorType.DWARF)
                .competitorStatus(CompetitorStatus.INJURED)
                //.team(team)
                .build();
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(teamService.getEligibleOrThrow(team.getId())).thenReturn(team);
        when(competitorService.findByTeam(team.getId())).thenReturn(List.of(injured));
        RegistrationRequest request = new RegistrationRequest(null, team.getId(), null);

        assertThatThrownBy(() -> registrationService.create(race.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_rejectsTeamMemberAlreadyRegisteredIndividually() {
        Race race = openRace(RaceType.TEAM, 10);
        Team team = activeTeam();
        Competitor member = activeCompetitor(team);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(teamService.getEligibleOrThrow(team.getId())).thenReturn(team);
        when(competitorService.findByTeam(team.getId())).thenReturn(List.of(member));
        when(registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(eq(race.getId()), eq(member.getId()), any()))
                .thenReturn(true);
        RegistrationRequest request = new RegistrationRequest(null, team.getId(), null);

        assertThatThrownBy(() -> registrationService.create(race.getId(), request))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void create_savesValidTeamRegistration() {
        Race race = openRace(RaceType.TEAM, 10);
        Team team = activeTeam();
        Competitor member = activeCompetitor(team);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(teamService.getEligibleOrThrow(team.getId())).thenReturn(team);
        when(competitorService.findByTeam(team.getId())).thenReturn(List.of(member));
        RegistrationRequest request = new RegistrationRequest(null, team.getId(), null);

        RegistrationResponse response = registrationService.create(race.getId(), request);

        assertThat(response.team().id()).isEqualTo(team.getId());
        assertThat(response.competitor()).isNull();
    }

    // ----- approve -----

    @Test
    void approve_rejectsWhenRegistrationNotPending() {
        RaceRegistration registration = pendingRegistration(openRace(RaceType.INDIVIDUAL, 10));
        registration.setStatus(RegistrationStatus.APPROVED);
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> registrationService.approve(registration.getId(), new RegistrationUpdateRequest(1, 1, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void approve_rejectsWhenMaxParticipantsReached() {
        Race race = openRace(RaceType.INDIVIDUAL, 2);
        RaceRegistration registration = pendingRegistration(race);
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
        when(registrationRepository.countByRace_IdAndStatus(race.getId(), RegistrationStatus.APPROVED)).thenReturn(2L);

        assertThatThrownBy(() -> registrationService.approve(registration.getId(), new RegistrationUpdateRequest(null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void approve_rejectsDuplicateLaneInSameRace() {
        Race race = openRace(RaceType.INDIVIDUAL, 10);
        RaceRegistration registration = pendingRegistration(race);
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
        when(registrationRepository.countByRace_IdAndStatus(race.getId(), RegistrationStatus.APPROVED)).thenReturn(0L);
        when(registrationRepository.existsByRace_IdAndLaneAndIdNot(race.getId(), 3, registration.getId())).thenReturn(true);

        assertThatThrownBy(() -> registrationService.approve(registration.getId(), new RegistrationUpdateRequest(3, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void approve_succeedsAndAssignsLaneAndStartPosition() {
        Race race = openRace(RaceType.INDIVIDUAL, 10);
        RaceRegistration registration = pendingRegistration(race);
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));
        when(registrationRepository.countByRace_IdAndStatus(race.getId(), RegistrationStatus.APPROVED)).thenReturn(0L);

        RegistrationResponse response = registrationService.approve(registration.getId(), new RegistrationUpdateRequest(3, 1, null));

        assertThat(response.status()).isEqualTo(RegistrationStatus.APPROVED);
        assertThat(response.lane()).isEqualTo(3);
        assertThat(response.startPosition()).isEqualTo(1);
        verify(auditLogService).record(eq(userId), eq("APPROVE"), eq("RaceRegistration"), eq(registration.getId()), eq(null));
    }

    // ----- reject -----

    @Test
    void reject_rejectsWhenRegistrationNotPending() {
        RaceRegistration registration = pendingRegistration(openRace(RaceType.INDIVIDUAL, 10));
        registration.setStatus(RegistrationStatus.CANCELLED);
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> registrationService.reject(registration.getId(), new RegistrationRejectionRequest("no room")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void reject_setsStatusAndNotes() {
        RaceRegistration registration = pendingRegistration(openRace(RaceType.INDIVIDUAL, 10));
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));

        RegistrationResponse response = registrationService.reject(registration.getId(), new RegistrationRejectionRequest("incomplete data"));

        assertThat(response.status()).isEqualTo(RegistrationStatus.REJECTED);
        assertThat(response.notes()).isEqualTo("incomplete data");
        verify(auditLogService).record(eq(userId), eq("REJECT"), eq("RaceRegistration"), eq(registration.getId()), eq(null));
    }

    // ----- cancel -----

    @Test
    void cancel_transitionsToCancelled() {
        RaceRegistration registration = pendingRegistration(openRace(RaceType.INDIVIDUAL, 10));
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));

        registrationService.cancel(registration.getId());

        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        verify(auditLogService).record(eq(userId), eq("CANCEL"), eq("RaceRegistration"), eq(registration.getId()), eq(null));
    }

    @Test
    void cancel_rejectsWhenAlreadyCancelled() {
        RaceRegistration registration = pendingRegistration(openRace(RaceType.INDIVIDUAL, 10));
        registration.setStatus(RegistrationStatus.CANCELLED);
        when(registrationRepository.findById(registration.getId())).thenReturn(Optional.of(registration));

        assertThatThrownBy(() -> registrationService.cancel(registration.getId()))
                .isInstanceOf(ConflictException.class);
    }

    // ----- reads -----

    @Test
    void getById_throwsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(registrationRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllForRace_throwsNotFoundWhenRaceMissing() {
        UUID raceId = UUID.randomUUID();
        when(raceRepository.existsById(raceId)).thenReturn(false);

        assertThatThrownBy(() -> registrationService.getAllForRace(raceId, null, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
