package com.example.implementacionparcial.races.dto;

import com.example.implementacionparcial.races.entity.RaceType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record RaceRequest(
        @NotBlank(message = "name is mandatory") String name,
        String description,
        @NotNull(message = "scheduledAt is mandatory") @Future(message = "scheduledAt must be in the future") LocalDateTime scheduledAt,
        @NotBlank(message = "startLocation is mandatory") String startLocation,
        @NotBlank(message = "endLocation is mandatory") String endLocation,
        @NotNull(message = "distanceMeters is mandatory") @Positive(message = "distanceMeters must be greater than 0") Double distanceMeters,
        @NotNull(message = "maxParticipants is mandatory") @Positive(message = "maxParticipants must be greater than 0") Integer maxParticipants,
        @NotNull(message = "type is mandatory") RaceType type,
        @NotNull(message = "registrationDeadline is mandatory") LocalDateTime registrationDeadline
) {
}
