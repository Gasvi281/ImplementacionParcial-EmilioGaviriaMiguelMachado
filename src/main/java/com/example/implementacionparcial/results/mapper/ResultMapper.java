package com.example.implementacionparcial.results.mapper;

import com.example.implementacionparcial.competitors.mapper.CompetitorMapper;
import com.example.implementacionparcial.races.mapper.RaceMapper;
import com.example.implementacionparcial.results.dto.ResultRequest;
import com.example.implementacionparcial.results.dto.ResultResponse;
import com.example.implementacionparcial.results.dto.ResultSummaryResponse;
import com.example.implementacionparcial.results.dto.ResultUpdateRequest;
import com.example.implementacionparcial.results.entity.RaceResult;
import com.example.implementacionparcial.results.entity.ResultStatus;
import com.example.implementacionparcial.teams.mapper.TeamMapper;

public final class ResultMapper {

    private ResultMapper() {
    }

    public static RaceResult toEntity(ResultRequest request) {
        return RaceResult.builder()
                .status(request.status())
                .startPosition(request.startPosition())
                .finalPosition(request.finalPosition())
                .completionTimeSeconds(request.completionTimeSeconds())
                .penaltyTimeSeconds(request.penaltyTimeSeconds() == null ? 0.0 : request.penaltyTimeSeconds())
                .notes(request.notes())
                .build();
        // race/competitor/team/recordedByUserId se resuelven en el service
        // (necesitan repositorios/services de otros dominios).
    }

    public static void applyUpdate(RaceResult result, ResultUpdateRequest request) {
        if (request.status() != null) {
            result.setStatus(request.status());
        }
        if (request.startPosition() != null) {
            result.setStartPosition(request.startPosition());
        }
        if (request.finalPosition() != null) {
            result.setFinalPosition(request.finalPosition());
        }
        if (request.completionTimeSeconds() != null) {
            result.setCompletionTimeSeconds(request.completionTimeSeconds());
        }
        if (request.penaltyTimeSeconds() != null) {
            result.setPenaltyTimeSeconds(request.penaltyTimeSeconds());
        }
        if (request.notes() != null) {
            result.setNotes(request.notes());
        }
    }

    public static ResultResponse toResponse(RaceResult result) {
        return new ResultResponse(
                result.getId(),
                RaceMapper.toSummary(result.getRace()),
                result.getCompetitor() == null ? null : CompetitorMapper.toSummary(result.getCompetitor()),
                result.getTeam() == null ? null : TeamMapper.toSummary(result.getTeam()),
                result.getStatus(),
                result.getStartPosition(),
                result.getFinalPosition(),
                result.getCompletionTimeSeconds(),
                result.getPenaltyTimeSeconds(),
                totalTime(result),
                pointsFor(result.getFinalPosition(), result.getStatus()),
                result.getNotes(),
                result.getRecordedByUserId(),
                result.getRecordedAt(),
                result.getUpdatedAt());
    }

    public static ResultSummaryResponse toSummary(RaceResult result) {
        return new ResultSummaryResponse(
                result.getId(), result.getFinalPosition(), result.getStatus(), totalTime(result));
    }

    /**
     * Tabla de puntos literal de {@code .claude/rules/results.md}. Vive acá (mapper puro, sin
     * Spring) para que tanto {@code ResultResponse.points} como {@code StandingService} usen
     * exactamente el mismo cálculo sin que {@code StandingService} tenga que depender de otro
     * service (las reglas exigen que solo lea de {@code IRaceResultRepository}).
     */
    public static int pointsFor(Integer finalPosition, ResultStatus status) {
        if (status != ResultStatus.FINISHED || finalPosition == null) {
            return 0;
        }
        return switch (finalPosition) {
            case 1 -> 10;
            case 2 -> 7;
            case 3 -> 5;
            case 4 -> 3;
            case 5 -> 1;
            default -> 0;
        };
    }

    private static Double totalTime(RaceResult result) {
        if (result.getCompletionTimeSeconds() == null) {
            return null;
        }
        double penalty = result.getPenaltyTimeSeconds() == null ? 0.0 : result.getPenaltyTimeSeconds();
        return result.getCompletionTimeSeconds() + penalty;
    }
}
