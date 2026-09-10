package com.example.implementacionparcial.teams.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Schema(description = "Team with its team members")
public record TeamResponse(

        @Schema(description = "Team identifier")
        UUID id,

        @Schema(description = "Team name", example = "Enanos del valle")
        String name,

        @Schema(description = "Team description", example = "Un equipo conformado unicamente por jugadores de baseball jubilados")
        String description,

        @Schema(description = "Coach name", example = "Chiqui mafia")
        String coach,

        @Schema(description = "Team maximum member count", example = "8")
        int maxMembers,

        @Schema(description = "Team formation date")
        Date creationDate,

        @Schema(description = "If inactive, the team was deactivated")
        String status,

        @Schema(description = "The registrations of all team members")
        List<TeamMemberResponse> teamMembers
) {
}
