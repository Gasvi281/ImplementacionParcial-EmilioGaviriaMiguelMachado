package com.example.implementacionparcial.teams.dto;

import com.example.implementacionparcial.teams.entity.TeamStatus;

import java.util.UUID;

public record TeamSummaryResponse(
        UUID id,
        String name,
        TeamStatus status
) {
}
