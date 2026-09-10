package com.example.implementacionparcial.competitors.service;

import com.example.implementacionparcial.auditlog.service.AuditLogService;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.common.security.CurrentUser;
import com.example.implementacionparcial.competitors.dto.CompetitorRequest;
import com.example.implementacionparcial.competitors.dto.CompetitorResponse;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.mapper.CompetitorMapper;
import com.example.implementacionparcial.competitors.repository.ICompetitorRepository;
import com.example.implementacionparcial.teams.entity.TeamMember;
import com.example.implementacionparcial.teams.repository.ITeamMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompetitorService {

    private final ICompetitorRepository competitorRepository;
    private final ITeamMemberRepository teamMemberRepository;
    private final AuditLogService auditLogService;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<CompetitorResponse> getCompetitors() {
        return competitorRepository.findAll()
                .stream()
                .map(CompetitorMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompetitorResponse getById(UUID id) {
        Competitor competitor = findCompetitorOrThrow(id);
        return CompetitorMapper.toResponse(competitor);
    }

    @Transactional
    public CompetitorResponse createCompetitor(CompetitorRequest request) {
        validateNicknameNotDuplicated(request.nickname(), null);

        Competitor competitor = CompetitorMapper.toEntity(request);
        Competitor saved = competitorRepository.save(competitor);

        auditLogService.record(currentUser.id(), "CREATE", "Competitor", saved.getId(), null);
        return CompetitorMapper.toResponse(saved);
    }

    @Transactional
    public CompetitorResponse updateCompetitor(UUID id, CompetitorRequest request) {
        Competitor competitor = findCompetitorOrThrow(id);
        validateNicknameNotDuplicated(request.nickname(), id);

        competitor.setName(request.name());
        competitor.setNickname(request.nickname());
        competitor.setCompetitorType(request.competitorType());
        competitor.setAge(request.age());
        competitor.setHeight(request.height());
        competitor.setWeight(request.weight());
        competitor.setPlaceOfOrigin(request.placeOfOrigin());

        Competitor updated = competitorRepository.save(competitor);
        auditLogService.record(currentUser.id(),"UPDATE","Competitor",updated.getId(), null);
        return CompetitorMapper.toResponse(updated);
    }

    @Transactional
    public CompetitorResponse changeStatus(UUID id, CompetitorStatus newStatus) {
        Competitor competitor = findCompetitorOrThrow(id);
        competitor.setCompetitorStatus(newStatus);

        Competitor updated = competitorRepository.save(competitor);
        auditLogService.record(currentUser.id(), "STATUS_CHANGE", "Competitor", updated.getId(), newStatus.name());
        return CompetitorMapper.toResponse(updated);
    }

    @Transactional
    public void deleteCompetitor(UUID id) {
        Competitor competitor = findCompetitorOrThrow(id);

        if (competitor.getCompetitorStatus() != CompetitorStatus.RETIRED) {
            throw new ConflictException("Only RETIRED competitors can be permanently deleted");
        }

        competitorRepository.delete(competitor);
        auditLogService.record(currentUser.id(), "DELETE", "Competitor", id, null);
    }

    @Transactional(readOnly = true)
    public Competitor getEligibleOrThrow(UUID id) {
        Competitor competitor = competitorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", id));
        if (competitor.getCompetitorStatus() != CompetitorStatus.ACTIVE) {
            throw new ConflictException("Competitor '%s' is not eligible (status: %s)"
                    .formatted(competitor.getId(), competitor.getCompetitorStatus()));
        }
        return competitor;
    }

    @Transactional(readOnly = true)
    public List<Competitor> findByTeam(UUID teamId) {
        return teamMemberRepository.findByTeamId(teamId)
                .stream()
                .map(TeamMember::getCompetitor)
                .toList();
    }

    private Competitor findCompetitorOrThrow(UUID id) {
        return competitorRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Competitor", id));
    }

    private void validateNicknameNotDuplicated(String nickname, UUID id) {
        boolean exists = (id == null)
                ? competitorRepository.existsByNickname(nickname)
                : competitorRepository.existsByNicknameAndIdNot(nickname, id);

        if (exists) {
            throw new ConflictException("Nickname already exists: " + nickname);
        }
    }
}