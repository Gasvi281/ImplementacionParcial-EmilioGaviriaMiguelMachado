package com.example.implementacionparcial.competitors.dto;

import com.example.implementacionparcial.competitors.entity.CompetitorType;

import java.util.UUID;

public record CompetitorSummaryResponse(
        UUID id,
        String nickname,
        CompetitorType type
) {
}
