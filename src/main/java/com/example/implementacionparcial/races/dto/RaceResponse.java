package com.example.implementacionparcial.races.dto;

import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;

import java.time.LocalDateTime;
import java.util.UUID;

public record RaceResponse(
        UUID id,
        String name,
        String description,
        LocalDateTime scheduledAt,
        String startLocation,
        String endLocation,
        Double distanceMeters,
        Integer maxParticipants,
        RaceType type,
        RaceStatus status,
        UUID organizerId,
        LocalDateTime registrationDeadline,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
