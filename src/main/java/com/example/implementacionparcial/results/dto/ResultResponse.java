package com.example.implementacionparcial.results.dto;

import com.example.implementacionparcial.competitors.dto.CompetitorSummaryResponse;
import com.example.implementacionparcial.races.dto.RaceSummaryResponse;
import com.example.implementacionparcial.results.entity.ResultStatus;
import com.example.implementacionparcial.teams.dto.TeamSummaryResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record ResultResponse(
        UUID id,
        RaceSummaryResponse race,
        CompetitorSummaryResponse competitor,
        TeamSummaryResponse team,
        ResultStatus status,
        Integer startPosition,
        Integer finalPosition,
        Double completionTimeSeconds,
        Double penaltyTimeSeconds,
        Double totalTimeSeconds, // completionTimeSeconds + penaltyTimeSeconds, null si no hay completionTime
        Integer points, // tabla de puntos de .claude/rules/results.md
        String notes,
        UUID recordedByUserId,
        LocalDateTime recordedAt,
        LocalDateTime updatedAt
) {
}
