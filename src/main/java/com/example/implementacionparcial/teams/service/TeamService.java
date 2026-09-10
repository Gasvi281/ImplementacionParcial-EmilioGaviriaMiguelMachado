package com.example.implementacionparcial.teams.service;

import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import com.example.implementacionparcial.teams.repository.ITeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Minimal placeholder service: this domain's read/write side (dto/mapper/controller) is owned by
 * whoever implements {@code feature/teams}. {@code getEligibleOrThrow} is already the final
 * contract other domains (like {@code registrations}) depend on.
 */
@Service
@RequiredArgsConstructor
public class TeamService {

    private final ITeamRepository teamRepository;

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
}
