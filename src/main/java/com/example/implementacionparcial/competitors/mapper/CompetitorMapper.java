package com.example.implementacionparcial.competitors.mapper;

import com.example.implementacionparcial.competitors.dto.CompetitorSummaryResponse;
import com.example.implementacionparcial.competitors.entity.Competitor;

/**
 * Minimal placeholder: only {@code toSummary} is provided, since it is the only mapping other
 * domains (like {@code registrations}) need. The full mapper (toEntity/toResponse/applyUpdate)
 * is owned by whoever implements {@code feature/competitors}.
 */
public final class CompetitorMapper {

    private CompetitorMapper() {
    }

    public static CompetitorSummaryResponse toSummary(Competitor competitor) {
        return new CompetitorSummaryResponse(competitor.getId(), competitor.getNickname(), competitor.getType());
    }
}
