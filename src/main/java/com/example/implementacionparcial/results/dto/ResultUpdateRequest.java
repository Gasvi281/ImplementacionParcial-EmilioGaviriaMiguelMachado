package com.example.implementacionparcial.results.dto;

import com.example.implementacionparcial.results.entity.ResultStatus;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Body de {@code PATCH /api/results/{id}}: no incluye {@code competitorId}/{@code teamId} —
 * cambiar de participante es crear otro resultado, no editar este. Todo boxed/opcional para
 * distinguir "no lo mandaron" de "mandaron 0/null", como exige {@code CLAUDE.md}.
 */
public record ResultUpdateRequest(
        ResultStatus status,
        Integer startPosition,
        Integer finalPosition,
        @Positive(message = "completionTimeSeconds must be greater than 0") Double completionTimeSeconds,
        @PositiveOrZero(message = "penaltyTimeSeconds cannot be negative") Double penaltyTimeSeconds,
        String notes
) {
    public boolean isEmpty() {
        return status == null && startPosition == null && finalPosition == null
                && completionTimeSeconds == null && penaltyTimeSeconds == null && notes == null;
    }
}
