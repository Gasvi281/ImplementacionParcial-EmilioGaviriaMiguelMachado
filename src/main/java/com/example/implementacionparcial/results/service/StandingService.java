package com.example.implementacionparcial.results.service;

import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.mapper.CompetitorMapper;
import com.example.implementacionparcial.results.dto.StandingResponse;
import com.example.implementacionparcial.results.entity.RaceResult;
import com.example.implementacionparcial.results.entity.ResultStatus;
import com.example.implementacionparcial.results.mapper.ResultMapper;
import com.example.implementacionparcial.results.repository.IRaceResultRepository;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.mapper.TeamMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * No hay entidad separada para "Standing": los standings se calculan a partir de
 * {@link RaceResult}, nunca se persisten (evita inconsistencias entre la tabla de resultados y
 * una tabla de puntajes desnormalizada — ver {@code .claude/rules/results.md}). Este service solo
 * lee de {@link IRaceResultRepository}, no escribe nada.
 */
@Service
@RequiredArgsConstructor
public class StandingService {

    private final IRaceResultRepository resultRepository;

    @Transactional(readOnly = true)
    public Page<StandingResponse> getGlobal(Pageable pageable) {
        return paginate(rank(aggregate(result -> true)), pageable);
    }

    @Transactional(readOnly = true)
    public Page<StandingResponse> getCompetitorStandings(Pageable pageable) {
        return paginate(rank(aggregate(result -> result.getCompetitor() != null)), pageable);
    }

    @Transactional(readOnly = true)
    public Page<StandingResponse> getTeamStandings(Pageable pageable) {
        return paginate(rank(aggregate(result -> result.getTeam() != null)), pageable);
    }

    private List<Aggregate> aggregate(Predicate<RaceResult> filter) {
        Map<UUID, Aggregate> byParticipant = new LinkedHashMap<>();

        for (RaceResult result : resultRepository.findAllForStandings()) {
            if (!filter.test(result)) {
                continue;
            }
            UUID key = result.getCompetitor() != null ? result.getCompetitor().getId() : result.getTeam().getId();
            Aggregate aggregate = byParticipant.computeIfAbsent(
                    key, k -> new Aggregate(result.getCompetitor(), result.getTeam()));
            aggregate.accumulate(result);
        }
        return new ArrayList<>(byParticipant.values());
    }

    /**
     * Orden: puntos desc -> victorias desc -> tiempo total asc. El rank se asigna sobre la lista
     * completa ya ordenada, antes de paginar, así la página 2 sigue empezando en el puesto 21.
     */
    private List<StandingResponse> rank(List<Aggregate> aggregates) {
        aggregates.sort(
                Comparator.comparingInt((Aggregate a) -> a.totalPoints).reversed()
                        .thenComparing(Comparator.comparingInt((Aggregate a) -> a.wins).reversed())
                        .thenComparing(a -> a.totalTimeSeconds == null ? Double.MAX_VALUE : a.totalTimeSeconds));

        List<StandingResponse> ranked = new ArrayList<>();
        int rank = 1;
        for (Aggregate aggregate : aggregates) {
            ranked.add(aggregate.toResponse(rank++));
        }
        return ranked;
    }

    private Page<StandingResponse> paginate(List<StandingResponse> all, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= all.size()) {
            return new PageImpl<>(List.of(), pageable, all.size());
        }
        int end = Math.min(start + pageable.getPageSize(), all.size());
        return new PageImpl<>(all.subList(start, end), pageable, all.size());
    }

    private static final class Aggregate {
        private final Competitor competitor;
        private final Team team;
        private int racesParticipated;
        private int wins;
        private int podiums;
        private int totalPoints;
        private Integer bestPosition;
        private Double totalTimeSeconds;

        private Aggregate(Competitor competitor, Team team) {
            this.competitor = competitor;
            this.team = team;
        }

        private void accumulate(RaceResult result) {
            racesParticipated++;
            totalPoints += ResultMapper.pointsFor(result.getFinalPosition(), result.getStatus());

            if (result.getStatus() == ResultStatus.FINISHED && result.getFinalPosition() != null) {
                int position = result.getFinalPosition();
                if (position == 1) {
                    wins++;
                }
                if (position <= 3) {
                    podiums++;
                }
                if (bestPosition == null || position < bestPosition) {
                    bestPosition = position;
                }
                if (result.getCompletionTimeSeconds() != null) {
                    double penalty = result.getPenaltyTimeSeconds() == null ? 0.0 : result.getPenaltyTimeSeconds();
                    totalTimeSeconds = (totalTimeSeconds == null ? 0.0 : totalTimeSeconds)
                            + result.getCompletionTimeSeconds() + penalty;
                }
            }
        }

        private StandingResponse toResponse(int rank) {
            return new StandingResponse(
                    rank,
                    competitor == null ? null : CompetitorMapper.toSummary(competitor),
                    team == null ? null : TeamMapper.toSummary(team),
                    racesParticipated, wins, podiums, totalPoints, bestPosition, totalTimeSeconds);
        }
    }
}
