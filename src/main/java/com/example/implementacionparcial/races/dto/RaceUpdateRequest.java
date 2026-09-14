package com.example.implementacionparcial.races.dto;

import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

public record RaceUpdateRequest(
        String name,
        String description,
        LocalDateTime scheduledAt,
        String startLocation,
        String endLocation,
        @Positive(message = "distanceMeters must be greater than 0") Double distanceMeters,
        @Positive(message = "maxParticipants must be greater than 0") Integer maxParticipants,
        LocalDateTime registrationDeadline
) {
    public boolean isEmpty() {
        return name == null && description == null && scheduledAt == null && startLocation == null
                && endLocation == null && distanceMeters == null && maxParticipants == null
                && registrationDeadline == null;
    }
}
