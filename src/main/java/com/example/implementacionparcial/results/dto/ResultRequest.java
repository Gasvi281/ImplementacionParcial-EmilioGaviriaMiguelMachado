package com.example.implementacionparcial.results.dto;

import com.example.implementacionparcial.results.entity.ResultStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

public record ResultRequest(
        UUID competitorId,
        UUID teamId,
        @NotNull(message = "status is mandatory") ResultStatus status,
        Integer startPosition,
        Integer finalPosition,
        @Positive(message = "completionTimeSeconds must be greater than 0") Double completionTimeSeconds,
        @PositiveOrZero(message = "penaltyTimeSeconds cannot be negative") Double penaltyTimeSeconds,
        String notes
) {
    /**
     * Exactamente uno de los dos debe estar presente, nunca ambos ni ninguno — misma restricción
     * de exclusividad que {@code RegistrationRequest.isValid()}.
     */
    public boolean isValid() {
        return (competitorId != null) ^ (teamId != null);
    }
}
