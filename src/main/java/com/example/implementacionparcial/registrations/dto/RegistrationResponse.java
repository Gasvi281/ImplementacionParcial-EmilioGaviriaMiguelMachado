package com.example.implementacionparcial.registrations.dto;

import com.example.implementacionparcial.competitors.dto.CompetitorSummaryResponse;
import com.example.implementacionparcial.races.dto.RaceSummaryResponse;
import com.example.implementacionparcial.registrations.entity.RegistrationStatus;
import com.example.implementacionparcial.teams.dto.TeamSummaryResponse;

import java.time.LocalDateTime;
import java.util.UUID;

public record RegistrationResponse(
        UUID id,
        RaceSummaryResponse race,
        CompetitorSummaryResponse competitor,
        TeamSummaryResponse team,
        RegistrationStatus status,
        Integer lane,
        Integer startPosition,
        String notes,
        UUID registeredByUserId,
        LocalDateTime registeredAt
) {
}
