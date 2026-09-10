package com.example.implementacionparcial.races.service;

import com.example.implementacionparcial.auditlog.service.AuditLogService;
import com.example.implementacionparcial.common.exceptions.BadRequestException;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.common.security.CurrentUser;
import com.example.implementacionparcial.races.dto.RaceRequest;
import com.example.implementacionparcial.races.dto.RaceResponse;
import com.example.implementacionparcial.races.dto.RaceUpdateRequest;
import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.races.repository.IRaceRepository;
import com.example.implementacionparcial.registrations.entity.RegistrationStatus;
import com.example.implementacionparcial.registrations.repository.IRaceRegistrationRepository;
import com.example.implementacionparcial.results.repository.IRaceResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
class RaceServiceTest {

    @Mock
    private IRaceRepository raceRepository;
    @Mock
    private IRaceRegistrationRepository raceRegistrationRepository;
    @Mock
    private IRaceResultRepository raceResultRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private RaceService raceService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenient().when(currentUser.id()).thenReturn(userId);
        lenient().when(raceRepository.save(any(Race.class))).thenAnswer(invocation -> {
            Race race = invocation.getArgument(0);
            if (race.getId() == null) {
                race.setId(UUID.randomUUID());
            }
            return race;
        });
    }

    private RaceRequest validRequest() {
        return new RaceRequest(
                "Dune Dash", "desc",
                LocalDateTime.now().plusDays(10),
                "Oasis Camp", "Sunset Ridge",
                5000.0, 20, RaceType.INDIVIDUAL,
                LocalDateTime.now().plusDays(5));
    }

    private Race raceWithStatus(RaceStatus status) {
        return Race.builder()
                .id(UUID.randomUUID())
                .name("Dune Dash")
                .scheduledAt(LocalDateTime.now().plusDays(10))
                .startLocation("A")
                .endLocation("B")
                .distanceMeters(5000.0)
                .maxParticipants(20)
                .type(RaceType.INDIVIDUAL)
                .status(status)
                .organizerId(userId)
                .registrationDeadline(LocalDateTime.now().plusDays(5))
                .build();
    }

    @Test
    void create_savesRaceAsDraftOwnedByCurrentUser() {
        RaceResponse response = raceService.create(validRequest());

        assertThat(response.status()).isEqualTo(RaceStatus.DRAFT);
        assertThat(response.organizerId()).isEqualTo(userId);
        verify(auditLogService).record(eq(userId), eq("CREATE"), eq("Race"), any(UUID.class), eq(null));
    }

    @Test
    void create_rejectsWhenDeadlineIsNotBeforeScheduledAt() {
        LocalDateTime scheduledAt = LocalDateTime.now().plusDays(5);
        RaceRequest request = new RaceRequest(
                "Dune Dash", "desc", scheduledAt, "A", "B",
                5000.0, 20, RaceType.INDIVIDUAL, scheduledAt.plusDays(1));

        assertThatThrownBy(() -> raceService.create(request))
                .isInstanceOf(BadRequestException.class);
        verify(raceRepository, never()).save(any());
    }

    @Test
    void getById_throwsNotFoundWhenMissing() {
        UUID id = UUID.randomUUID();
        when(raceRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> raceService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_rejectsEmptyPatch() {
        RaceUpdateRequest empty = new RaceUpdateRequest(null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> raceService.update(UUID.randomUUID(), empty))
                .isInstanceOf(BadRequestException.class);
        verify(raceRepository, never()).findById(any());
    }

    @Test
    void update_rejectsWhenRaceIsCompleted() {
        Race race = raceWithStatus(RaceStatus.COMPLETED);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        RaceUpdateRequest patch = new RaceUpdateRequest("New name", null, null, null, null, null, null, null);

        assertThatThrownBy(() -> raceService.update(race.getId(), patch))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void update_appliesPatchAndPersists() {
        Race race = raceWithStatus(RaceStatus.DRAFT);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        RaceUpdateRequest patch = new RaceUpdateRequest("New name", null, null, null, null, null, null, null);

        RaceResponse response = raceService.update(race.getId(), patch);

        assertThat(response.name()).isEqualTo("New name");
        verify(auditLogService).record(eq(userId), eq("UPDATE"), eq("Race"), eq(race.getId()), eq(null));
    }

    @Test
    void updateStatus_rejectsBackwardTransition() {
        Race race = raceWithStatus(RaceStatus.CLOSED_FOR_REGISTRATION);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> raceService.updateStatus(race.getId(), RaceStatus.DRAFT))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_rejectsTransitionFromTerminalStatus() {
        Race race = raceWithStatus(RaceStatus.COMPLETED);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> raceService.updateStatus(race.getId(), RaceStatus.CANCELLED))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_rejectsInProgressWithFewerThanTwoApprovedRegistrations() {
        Race race = raceWithStatus(RaceStatus.CLOSED_FOR_REGISTRATION);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(raceRegistrationRepository.countByRace_IdAndStatus(race.getId(), RegistrationStatus.APPROVED))
                .thenReturn(1L);

        assertThatThrownBy(() -> raceService.updateStatus(race.getId(), RaceStatus.IN_PROGRESS))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_allowsInProgressWithAtLeastTwoApprovedRegistrations() {
        Race race = raceWithStatus(RaceStatus.CLOSED_FOR_REGISTRATION);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(raceRegistrationRepository.countByRace_IdAndStatus(race.getId(), RegistrationStatus.APPROVED))
                .thenReturn(2L);

        RaceResponse response = raceService.updateStatus(race.getId(), RaceStatus.IN_PROGRESS);

        assertThat(response.status()).isEqualTo(RaceStatus.IN_PROGRESS);
    }

    @Test
    void updateStatus_rejectsCompletedWithoutOfficialResults() {
        Race race = raceWithStatus(RaceStatus.IN_PROGRESS);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(raceResultRepository.existsByRace_IdAndFinalPosition(race.getId(), 1)).thenReturn(false);

        assertThatThrownBy(() -> raceService.updateStatus(race.getId(), RaceStatus.COMPLETED))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateStatus_allowsCompletedWithOfficialResults() {
        Race race = raceWithStatus(RaceStatus.IN_PROGRESS);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));
        when(raceResultRepository.existsByRace_IdAndFinalPosition(race.getId(), 1)).thenReturn(true);

        RaceResponse response = raceService.updateStatus(race.getId(), RaceStatus.COMPLETED);

        assertThat(response.status()).isEqualTo(RaceStatus.COMPLETED);
    }

    @Test
    void cancel_transitionsDraftRaceToCancelled() {
        Race race = raceWithStatus(RaceStatus.DRAFT);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));

        raceService.cancel(race.getId());

        assertThat(race.getStatus()).isEqualTo(RaceStatus.CANCELLED);
        verify(auditLogService).record(eq(userId), eq("CANCEL"), eq("Race"), eq(race.getId()), eq(null));
    }

    @Test
    void cancel_rejectsWhenRaceAlreadyCompleted() {
        Race race = raceWithStatus(RaceStatus.COMPLETED);
        when(raceRepository.findById(race.getId())).thenReturn(Optional.of(race));

        assertThatThrownBy(() -> raceService.cancel(race.getId()))
                .isInstanceOf(ConflictException.class);
    }
}
