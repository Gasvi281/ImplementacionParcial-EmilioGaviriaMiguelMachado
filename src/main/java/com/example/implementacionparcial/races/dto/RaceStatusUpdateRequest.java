package com.example.implementacionparcial.races.dto;

import com.example.implementacionparcial.races.entity.RaceStatus;
import jakarta.validation.constraints.NotNull;

public record RaceStatusUpdateRequest(
        @NotNull(message = "status is mandatory") RaceStatus status
) {
}
