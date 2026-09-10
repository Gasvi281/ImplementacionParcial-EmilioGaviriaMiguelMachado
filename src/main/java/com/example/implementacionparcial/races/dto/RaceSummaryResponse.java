package com.example.implementacionparcial.races.dto;

import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;

import java.time.LocalDateTime;
import java.util.UUID;

public record RaceSummaryResponse(
        UUID id,
        String name,
        LocalDateTime scheduledAt,
        RaceType type,
        RaceStatus status
) {
}
