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
import com.example.implementacionparcial.races.mapper.RaceMapper;
import com.example.implementacionparcial.races.repository.IRaceRepository;
import com.example.implementacionparcial.registrations.entity.RegistrationStatus;
import com.example.implementacionparcial.registrations.repository.IRaceRegistrationRepository;
import com.example.implementacionparcial.results.repository.IRaceResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RaceService {

    private static final int MIN_PARTICIPANTS_TO_START = 2;

    private final IRaceRepository raceRepository;
    private final IRaceRegistrationRepository raceRegistrationRepository;
    private final IRaceResultRepository raceResultRepository;
    private final AuditLogService auditLogService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public Page<RaceResponse> getAll(RaceStatus status, RaceType type, String name, Pageable pageable) {
        return raceRepository.search(status, type, name, pageable).map(RaceMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public RaceResponse getById(UUID id) {
        return RaceMapper.toResponse(findOrThrow(id));
    }

    @Transactional
    public RaceResponse create(RaceRequest request) {
        validateDeadlineBeforeSchedule(request.registrationDeadline(), request.scheduledAt());

        Race race = RaceMapper.toEntity(request);
        race.setOrganizerId(currentUser.id());
        Race saved = raceRepository.save(race);

        auditLogService.record(currentUser.id(), "CREATE", "Race", saved.getId(), null);
        return RaceMapper.toResponse(saved);
    }

    @Transactional
    public RaceResponse update(UUID id, RaceUpdateRequest request) {
        if (request.isEmpty()) {
            throw new BadRequestException("At least one field must be provided to update a race");
        }

        Race race = findOrThrow(id);
        if (race.getStatus() == RaceStatus.COMPLETED) {
            throw new ConflictException("A completed race cannot be edited");
        }

        RaceMapper.applyUpdate(race, request);
        validateDeadlineBeforeSchedule(race.getRegistrationDeadline(), race.getScheduledAt());

        Race saved = raceRepository.save(race);
        auditLogService.record(currentUser.id(), "UPDATE", "Race", saved.getId(), null);
        return RaceMapper.toResponse(saved);
    }

    @Transactional
    public RaceResponse updateStatus(UUID id, RaceStatus targetStatus) {
        Race race = findOrThrow(id);
        validateStatusTransition(race, targetStatus);

        race.setStatus(targetStatus);
        Race saved = raceRepository.save(race);
        auditLogService.record(currentUser.id(), "STATUS_CHANGE", "Race", saved.getId(), targetStatus.name());
        return RaceMapper.toResponse(saved);
    }

    @Transactional
    public void cancel(UUID id) {
        Race race = findOrThrow(id);
        validateStatusTransition(race, RaceStatus.CANCELLED);

        race.setStatus(RaceStatus.CANCELLED);
        raceRepository.save(race);
        auditLogService.record(currentUser.id(), "CANCEL", "Race", id, null);
    }

    private Race findOrThrow(UUID id) {
        return raceRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Race", id));
    }

    private void validateDeadlineBeforeSchedule(LocalDateTime registrationDeadline,
                                                 LocalDateTime scheduledAt) {
        if (registrationDeadline != null && scheduledAt != null && !registrationDeadline.isBefore(scheduledAt)) {
            throw new BadRequestException("registrationDeadline must be strictly before scheduledAt");
        }
    }

    private void validateStatusTransition(Race race, RaceStatus target) {
        RaceStatus current = race.getStatus();

        if (current == target) {
            throw new ConflictException("Race is already in status " + target);
        }
        if (current == RaceStatus.COMPLETED || current == RaceStatus.CANCELLED) {
            throw new ConflictException("A race in status " + current + " cannot transition to any other status");
        }
        if (target != RaceStatus.CANCELLED && target.ordinal() <= current.ordinal()) {
            throw new ConflictException("Race status cannot move from " + current + " back to " + target);
        }
        if (target == RaceStatus.IN_PROGRESS) {
            long approved = raceRegistrationRepository.countByRace_IdAndStatus(race.getId(), RegistrationStatus.APPROVED);
            if (approved < MIN_PARTICIPANTS_TO_START) {
                throw new ConflictException("Race needs at least %d APPROVED registrations to start"
                        .formatted(MIN_PARTICIPANTS_TO_START));
            }
        }
        if (target == RaceStatus.COMPLETED) {
            // "Resultado oficial" = existe un ganador (finalPosition == 1) registrado para la
            // carrera; ver .claude/rules/results.md (no hay un status OFFICIAL en RaceResult).
            boolean hasOfficialResults = raceResultRepository.existsByRace_IdAndFinalPosition(race.getId(), 1);
            if (!hasOfficialResults) {
                throw new ConflictException("Race cannot be completed without official results registered");
            }
        }
    }
}
