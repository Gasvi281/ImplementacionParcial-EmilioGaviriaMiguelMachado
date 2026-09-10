package com.example.implementacionparcial.teams.mapper;

import com.example.implementacionparcial.teams.dto.TeamSummaryResponse;
import com.example.implementacionparcial.teams.entity.Team;

/**
 * Minimal placeholder: only {@code toSummary} is provided, since it is the only mapping other
 * domains (like {@code registrations}) need. The full mapper is owned by whoever implements
 * {@code feature/teams}.
 */
public final class TeamMapper {

    private TeamMapper() {
    }

    public static TeamSummaryResponse toSummary(Team team) {
        return new TeamSummaryResponse(team.getId(), team.getName(), team.getStatus());
    }
}
