package com.example.implementacionparcial.competitors.dto;

import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.teams.dto.TeamMemberResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Schema(description = "Competitor's data")
public record CompetitorResponse(

        @Schema(description = "Competitor identifier")
        UUID id,

        @Schema(description = "Competitor name", example = "Javier")
        String name,

        @Schema(description = "Competitor nickname", example = "El jorobado")
        String nickname,

        @Schema(description = "Competitor may be a dwarf, camel, medium or other")
        CompetitorType competitorType,

        @Schema(description = "Competitor age", example = "19")
        int age,

        @Schema(description = "Competitor height", example = "180")
        float height,

        @Schema(description = "Competitor weight", example = "72")
        float weight,

        @Schema(description = "Competitor place of origin", example = "Manizales")
        String placeOfOrigin,

        @Schema(description = "If RETIRED, competitor was logically deleted")
        CompetitorStatus competitorStatus,

        @Schema(description = "Date competitor was registered")
        Date registrationDate,

        @Schema(description = "the registration of teams of this competitor")
        List<TeamMemberResponse> memberships
) {
}
