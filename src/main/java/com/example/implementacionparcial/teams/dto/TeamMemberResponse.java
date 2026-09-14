package com.example.implementacionparcial.teams.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;
import java.util.UUID;

@Schema(description = "Data of a single competitor's information in a certain team")
public record TeamMemberResponse(

        @Schema(description = "Team member identifier")
        UUID id,

        @Schema(description = "Team identifier")
        UUID team_id,

        @Schema(description = "Competitor identifier")
        UUID competitor_id,

        @Schema(description = "Date competitor joined team")
        Date joinedAt
) {
}
