package com.example.implementacionparcial.competitors.service;

import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.repository.ICompetitorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Minimal placeholder service: this domain's read/write side (dto/mapper/controller) is owned by
 * whoever implements {@code feature/competitors}. {@code getEligibleOrThrow} and {@code findByTeam}
 * are already the final contract other domains (like {@code registrations}) depend on, the same way
 * {@code CowService} reuses {@code OwnerService.getActiveEntityOrThrow(...)} in the reference repo.
 */
@Service
@RequiredArgsConstructor
public class CompetitorService {

    private final ICompetitorRepository competitorRepository;

    @Transactional(readOnly = true)
    public Competitor getEligibleOrThrow(UUID id) {
        Competitor competitor = competitorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", id));
        if (competitor.getStatus() != CompetitorStatus.ACTIVE) {
            throw new ConflictException("Competitor '%s' is not eligible (status: %s)"
                    .formatted(competitor.getId(), competitor.getStatus()));
        }
        return competitor;
    }

    @Transactional(readOnly = true)
    public List<Competitor> findByTeam(UUID teamId) {
        return competitorRepository.findAllByTeam_Id(teamId);
    }
}
