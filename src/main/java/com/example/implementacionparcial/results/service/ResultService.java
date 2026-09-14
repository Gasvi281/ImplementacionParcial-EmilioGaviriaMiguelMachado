package com.example.implementacionparcial.results.service;

import com.example.implementacionparcial.auditlog.service.AuditLogService;
import com.example.implementacionparcial.common.exceptions.BadRequestException;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.common.security.CurrentUser;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.repository.ICompetitorRepository;
import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.repository.IRaceRepository;
import com.example.implementacionparcial.registrations.entity.RegistrationStatus;
import com.example.implementacionparcial.registrations.repository.IRaceRegistrationRepository;
import com.example.implementacionparcial.results.dto.ResultRequest;
import com.example.implementacionparcial.results.dto.ResultResponse;
import com.example.implementacionparcial.results.dto.ResultUpdateRequest;
import com.example.implementacionparcial.results.entity.RaceResult;
import com.example.implementacionparcial.results.entity.ResultStatus;
import com.example.implementacionparcial.results.mapper.ResultMapper;
import com.example.implementacionparcial.results.repository.IRaceResultRepository;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.repository.ITeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResultService {

    private static final Set<RegistrationStatus> APPROVED_ONLY = EnumSet.of(RegistrationStatus.APPROVED);

    private final IRaceResultRepository resultRepository;
    private final IRaceRepository raceRepository;
    private final IRaceRegistrationRepository registrationRepository;
    private final ICompetitorRepository competitorRepository;
    private final ITeamRepository teamRepository;
    private final AuditLogService auditLogService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public Page<ResultResponse> getAllForRace(UUID raceId, ResultStatus status, Pageable pageable) {
        if (!raceRepository.existsById(raceId)) {
            throw ResourceNotFoundException.of("Race", raceId);
        }
        return resultRepository.search(raceId, status, pageable).map(ResultMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ResultResponse getById(UUID id) {
        return ResultMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public ResultResponse create(UUID raceId, ResultRequest request) {
        if (!request.isValid()) {
            throw new BadRequestException("Exactly one of competitorId or teamId must be provided");
        }

        Race race = raceRepository.findById(raceId)
                .orElseThrow(() -> ResourceNotFoundException.of("Race", raceId));
        if (race.getStatus() != RaceStatus.IN_PROGRESS) {
            throw new ConflictException("A result can only be recorded while the race is IN_PROGRESS");
        }

        Competitor competitor = null;
        Team team = null;

        if (request.competitorId() != null) {
            competitor = competitorRepository.findById(request.competitorId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Competitor", request.competitorId()));
            validateApprovedRegistration(raceId, request.competitorId(), null);
            if (resultRepository.existsByRace_IdAndCompetitor_Id(raceId, competitor.getId())) {
                throw new ConflictException("This competitor already has a result recorded for this race");
            }
        } else {
            team = teamRepository.findById(request.teamId())
                    .orElseThrow(() -> ResourceNotFoundException.of("Team", request.teamId()));
            validateApprovedRegistration(raceId, null, request.teamId());
            if (resultRepository.existsByRace_IdAndTeam_Id(raceId, team.getId())) {
                throw new ConflictException("This team already has a result recorded for this race");
            }
        }

        RaceResult candidate = ResultMapper.toEntity(request);
        candidate.setRace(race);
        candidate.setCompetitor(competitor);
        candidate.setTeam(team);
        candidate.setRecordedByUserId(currentUser.id());

        validateInvariants(raceId, candidate, null);

        RaceResult saved = resultRepository.save(candidate);
        auditLogService.record(currentUser.id(), "CREATE", "RaceResult", saved.getId(), null);
        return ResultMapper.toResponse(saved);
    }

    @Transactional
    public ResultResponse update(UUID id, ResultUpdateRequest request) {
        if (request.isEmpty()) {
            throw new BadRequestException("At least one field must be provided to update a result");
        }

        RaceResult result = findOrThrow(id);
        UUID raceId = result.getRace().getId();
        String oldValue = describe(result);

        ResultMapper.applyUpdate(result, request);
        validateInvariants(raceId, result, result.getId());

        RaceResult saved = resultRepository.save(result);
        // Los standings se recalculan on-the-fly desde StandingService: no hay nada más que
        // "reconciliar" acá, pero el audit log sí debe dejar constancia del antes/después.
        auditLogService.record(currentUser.id(), "UPDATE", "RaceResult", saved.getId(),
                "old: [%s], new: [%s]".formatted(oldValue, describe(saved)));
        return ResultMapper.toResponse(saved);
    }

    private RaceResult findOrThrow(UUID id) {
        return resultRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("RaceResult", id));
    }

    private void validateApprovedRegistration(UUID raceId, UUID competitorId, UUID teamId) {
        boolean approved = competitorId != null
                ? registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(raceId, competitorId, APPROVED_ONLY)
                : registrationRepository.existsByRace_IdAndTeam_IdAndStatusIn(raceId, teamId, APPROVED_ONLY);
        if (!approved) {
            throw new ConflictException("Only participants with an APPROVED registration can receive a result");
        }
    }

    /**
     * Corre tanto en {@code create} (excludeId == null) como en {@code update} (excludeId == id
     * del resultado editado), así un PATCH no puede colar un estado que el POST rechaza.
     */
    private void validateInvariants(UUID raceId, RaceResult candidate, UUID excludeId) {
        Double completionTime = candidate.getCompletionTimeSeconds();
        if (completionTime != null && completionTime <= 0) {
            throw new BadRequestException("completionTimeSeconds must be positive");
        }
        if (candidate.getPenaltyTimeSeconds() != null && candidate.getPenaltyTimeSeconds() < 0) {
            throw new BadRequestException("penaltyTimeSeconds cannot be negative");
        }

        ResultStatus status = candidate.getStatus();
        Integer finalPosition = candidate.getFinalPosition();

        if (status == ResultStatus.FINISHED) {
            if (finalPosition == null || finalPosition < 1) {
                throw new BadRequestException("finalPosition is mandatory and must be >= 1 for a FINISHED result");
            }
            if (completionTime == null) {
                throw new BadRequestException("completionTimeSeconds is mandatory for a FINISHED result");
            }
            boolean duplicated = excludeId == null
                    ? resultRepository.existsByRace_IdAndFinalPositionAndStatus(raceId, finalPosition, ResultStatus.FINISHED)
                    : resultRepository.existsByRace_IdAndFinalPositionAndStatusAndIdNot(
                            raceId, finalPosition, ResultStatus.FINISHED, excludeId);
            if (duplicated) {
                throw new ConflictException(
                        "Final position %d is already taken by another finisher in this race".formatted(finalPosition));
            }
        }

        if (status == ResultStatus.DISQUALIFIED && finalPosition != null && finalPosition == 1) {
            throw new BadRequestException("A disqualified participant cannot be the winner");
        }

        if (status == ResultStatus.DID_NOT_START && completionTime != null) {
            throw new BadRequestException("A participant that did not start cannot have a completionTimeSeconds");
        }

        // Solo puede haber un ganador oficial (finalPosition == 1) por carrera, sin importar el
        // status (DISQUALIFIED ya quedó descartado arriba).
        if (finalPosition != null && finalPosition == 1) {
            boolean winnerExists = excludeId == null
                    ? resultRepository.existsByRace_IdAndFinalPosition(raceId, 1)
                    : resultRepository.existsByRace_IdAndFinalPositionAndIdNot(raceId, 1, excludeId);
            if (winnerExists) {
                throw new ConflictException("This race already has an official winner");
            }
        }
    }

    private String describe(RaceResult result) {
        return "status=%s, finalPosition=%s, completionTimeSeconds=%s".formatted(
                result.getStatus(), result.getFinalPosition(), result.getCompletionTimeSeconds());
    }
}
