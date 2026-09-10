package com.example.implementacionparcial.competitors.service;

import com.example.implementacionparcial.competitors.dto.CompetitorRequest;
import com.example.implementacionparcial.competitors.dto.CompetitorResponse;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.mapper.CompetitorMapper;
import com.example.implementacionparcial.competitors.repository.ICompetitorRepository;
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

    @Transactional(readOnly = true)
    public List<CompetitorResponse> getCompetitors(){
        return competitorRepository.findAll()
                .stream()
                .map(CompetitorMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompetitorResponse getById(UUID id){
        Competitor competitor = findCompetitorOrThrow(id);
        return CompetitorMapper.toResponse(competitor);
    }

    @Transactional
    public CompetitorResponse createCompetitor(CompetitorRequest request) {
        validateNicknameNotDuplicated(request.nickname(), null);

        Competitor competitor = CompetitorMapper.toEntity(request);
        Competitor saved = competitorRepository.save(competitor);

        log.info("Competitor created id={} with nickname={}", saved.getId(), saved.getNickname());
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
        log.info("Competitor updated id={}", updated.getId());
        return CompetitorMapper.toResponse(updated);
    }

    @Transactional
    public CompetitorResponse changeStatus(UUID id, CompetitorStatus newStatus) {
        Competitor competitor = findCompetitorOrThrow(id);
        competitor.setCompetitorStatus(newStatus);

        Competitor updated = competitorRepository.save(competitor);
        log.info("Competitor id={} status changed to {}", updated.getId(), newStatus);
        return CompetitorMapper.toResponse(updated);
    }

    @Transactional
    public void deleteCompetitor(UUID id) {
        Competitor competitor = findCompetitorOrThrow(id);

        if (competitor.getCompetitorStatus() != CompetitorStatus.RETIRED) {
            throw new RuntimeException("Only RETIRED competitors can be permanently deleted");
        }

        competitorRepository.delete(competitor);
        log.info("Competitor id={} permanently deleted", id);
    }

    private Competitor findCompetitorOrThrow(UUID id) {
        return competitorRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Competitor not found with id: " + id));
    }

    private void validateNicknameNotDuplicated(String nickname, UUID id){
        boolean exists = (id == null)
                ? competitorRepository.existsByNickname(nickname)
                : competitorRepository.existsByNicknameAndIdNot(nickname, id);

        if (exists) {
            throw new RuntimeException("Nickname already exists: " + nickname);
        }
    }
}