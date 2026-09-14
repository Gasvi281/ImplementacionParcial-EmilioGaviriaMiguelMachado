package com.example.implementacionparcial.results.dto;

import com.example.implementacionparcial.competitors.dto.CompetitorSummaryResponse;
import com.example.implementacionparcial.teams.dto.TeamSummaryResponse;

/**
 * Fila calculada por {@code StandingService} a partir de {@code RaceResult}; nunca se persiste.
 * Exactamente uno de {@code competitor}/{@code team} viene no-nulo, según qué listado se pidió.
 */
public record StandingResponse(
        int rank,
        CompetitorSummaryResponse competitor,
        TeamSummaryResponse team,
        int racesParticipated,
        int wins,
        int podiums,
        int totalPoints,
        Integer bestPosition,
        Double totalTimeSeconds
) {
}
