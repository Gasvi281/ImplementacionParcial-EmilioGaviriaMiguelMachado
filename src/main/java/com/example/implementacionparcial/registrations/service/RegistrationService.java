package com.example.implementacionparcial.registrations.service;

import com.example.implementacionparcial.auditlog.service.AuditLogService;
import com.example.implementacionparcial.common.exceptions.BadRequestException;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.common.security.CurrentUser;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
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
import com.example.implementacionparcial.registrations.mapper.RegistrationMapper;
import com.example.implementacionparcial.registrations.repository.IRaceRegistrationRepository;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.service.TeamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final Set<RegistrationStatus> ACTIVE_STATUSES =
            EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED);

    private final IRaceRegistrationRepository registrationRepository;
    private final IRaceRepository raceRepository;
    private final CompetitorService competitorService;
    private final TeamService teamService;
    private final AuditLogService auditLogService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public Page<RegistrationResponse> getAllForRace(UUID raceId, RegistrationStatus status, Pageable pageable) {
        if (!raceRepository.existsById(raceId)) {
            throw ResourceNotFoundException.of("Race", raceId);
        }
        return registrationRepository.search(raceId, status, pageable).map(RegistrationMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RegistrationResponse getById(UUID id) {
        return RegistrationMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public RegistrationResponse create(UUID raceId, RegistrationRequest request) {
        if (!request.isValid()) {
            throw new BadRequestException("Exactly one of competitorId or teamId must be provided");
        }

        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Race", raceId));
        validateRaceIsOpenForRegistration(race);

        Competitor competitor = null;
        Team team = null;

        if (request.competitorId() != null) {
            validateTypeAccepts(race, RaceType.INDIVIDUAL);
            competitor = competitorService.getEligibleOrThrow(request.competitorId());
            validateIndividualNotAlreadyRegistered(raceId, competitor);
        } else {
            validateTypeAccepts(race, RaceType.TEAM);
            team = teamService.getEligibleOrThrow(request.teamId());
            validateTeamNotAlreadyRegistered(raceId, team);
        }

        RaceRegistration registration = RegistrationMapper.toEntity(request);
        registration.setRace(race);
        registration.setCompetitor(competitor);
        registration.setTeam(team);
        registration.setRegisteredByUserId(currentUser.id());

        RaceRegistration saved = registrationRepository.save(registration);
        auditLogService.record(currentUser.id(), "CREATE", "RaceRegistration", saved.getId(), null);
        return RegistrationMapper.toResponse(saved);
    }

    @Transactional
    public RegistrationResponse approve(UUID id, RegistrationUpdateRequest request) {
        RaceRegistration registration = findOrThrow(id);
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new ConflictException("Only PENDING registrations can be approved");
        }

        RegistrationMapper.applyApproval(registration, request);

        Race race = registration.getRace();
        long approvedCount = registrationRepository.countByRace_IdAndStatus(race.getId(), RegistrationStatus.APPROVED);
        if (approvedCount >= race.getMaxParticipants()) {
            throw new ConflictException("Race has reached its maximum number of participants");
        }
        if (registration.getLane() != null && registrationRepository
                .existsByRace_IdAndLaneAndIdNot(race.getId(), registration.getLane(), registration.getId())) {
            throw new ConflictException("Lane %d is already assigned in this race".formatted(registration.getLane()));
        }
        if (registration.getStartPosition() != null && registrationRepository
                .existsByRace_IdAndStartPositionAndIdNot(race.getId(), registration.getStartPosition(), registration.getId())) {
            throw new ConflictException(
                    "Start position %d is already assigned in this race".formatted(registration.getStartPosition()));
        }

        registration.setStatus(RegistrationStatus.APPROVED);
        RaceRegistration saved = registrationRepository.save(registration);
        auditLogService.record(currentUser.id(), "APPROVE", "RaceRegistration", saved.getId(), null);
        return RegistrationMapper.toResponse(saved);
    }

    @Transactional
    public RegistrationResponse reject(UUID id, RegistrationRejectionRequest request) {
        RaceRegistration registration = findOrThrow(id);
        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new ConflictException("Only PENDING registrations can be rejected");
        }

        registration.setNotes(request.notes());
        registration.setStatus(RegistrationStatus.REJECTED);
        RaceRegistration saved = registrationRepository.save(registration);
        auditLogService.record(currentUser.id(), "REJECT", "RaceRegistration", saved.getId(), null);
        return RegistrationMapper.toResponse(saved);
    }

    @Transactional
    public void cancel(UUID id) {
        RaceRegistration registration = findOrThrow(id);
        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new ConflictException("Registration is already cancelled");
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepository.save(registration);
        auditLogService.record(currentUser.id(), "CANCEL", "RaceRegistration", id, null);
    }

    private RaceRegistration findOrThrow(UUID id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("RaceRegistration", id));
    }

    private void validateRaceIsOpenForRegistration(Race race) {
        if (race.getStatus() != RaceStatus.OPEN_FOR_REGISTRATION) {
            throw new ConflictException("Race is not open for registration");
        }
        if (!LocalDateTime.now().isBefore(race.getRegistrationDeadline())) {
            throw new ConflictException("Registration deadline for this race has already passed");
        }
    }

    private void validateTypeAccepts(Race race, RaceType requested) {
        boolean accepted = race.getType() == RaceType.MIXED || race.getType() == requested;
        if (!accepted) {
            throw new BadRequestException(
                    "Race of type %s does not accept %s registrations".formatted(race.getType(), requested));
        }
    }

    private void validateIndividualNotAlreadyRegistered(UUID raceId, Competitor competitor) {
        if (registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(raceId, competitor.getId(), ACTIVE_STATUSES)) {
            throw new ConflictException("Competitor is already registered for this race");
        }

        UUID teamId = teamService.findCurrentActiveTeamId(competitor.getId());

        if (teamId != null && registrationRepository
                .existsByRace_IdAndTeam_IdAndStatusIn(raceId, teamId, ACTIVE_STATUSES)) {
            throw new ConflictException("Competitor is already registered as part of a team in this race");
        }
    }

    private void validateTeamNotAlreadyRegistered(UUID raceId, Team team) {
        if (registrationRepository.existsByRace_IdAndTeam_IdAndStatusIn(raceId, team.getId(), ACTIVE_STATUSES)) {
            throw new ConflictException("Team is already registered for this race");
        }

        List<Competitor> members = competitorService.findByTeam(team.getId());
        if (members.isEmpty()) {
            throw new BadRequestException("Team has no members to register");
        }
        for (Competitor member : members) {
            if (member.getStatus() != CompetitorStatus.ACTIVE) {
                throw new ConflictException(
                        "Team member '%s' is not eligible (status: %s)".formatted(member.getId(), member.getStatus()));
            }
            if (registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(raceId, member.getId(), ACTIVE_STATUSES)) {
                throw new ConflictException(
                        "Team member '%s' is already registered individually in this race".formatted(member.getId()));
            }
        }
    }
}
