package com.example.implementacionparcial.teams.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeamRequest(

        @NotBlank(message = "Team name is mandatory")
        @Size(min = 10, max = 120, message = "name must be between 10 and 120 characters")
        @Schema(description = "Team's name", example = "Enanos del valle")
        String name,

        @NotBlank(message = "Description is mandatory")
        @Size(min = 30, max = 1000, message = "Team description must be between 30 and 1000 characters")
        @Schema(description = "Team's description", example = "Un equipo conformado unicamente por jugadores de baseball jubilados")
        String description,

        @NotBlank(message = "Coach name is mandatory")
        @Size(min = 10, max = 100, message = "Coach name must be between 10 and 100 characters")
        @Schema(description = "Coach's name", example = "Chiqui Tapia")
        String coach,

        @NotNull(message = "The team's maximum member count is mandatory")
        @Min(value = 0, message = "Team's max member count cannot be negative")
        @Schema(description = "Team's maximum member count", example = "8")
        int maxMembers
) {
}
