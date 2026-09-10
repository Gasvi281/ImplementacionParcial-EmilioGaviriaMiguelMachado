package com.example.implementacionparcial.competitors.dto;

import com.example.implementacionparcial.competitors.entity.CompetitorType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Data needed to register a competitor")
public record CompetitorRequest(

    @NotBlank(message = "name is mandatory")
    @Size(min = 2, max = 100, message = "name must be between 2 and 100 characters")
    @Schema(description = "Competitor's name", example = "Javier")
    String name,

    @NotBlank(message = "nickname is mandatory")
    @Size(min = 2, max = 80, message = "nickname must be between 2 and 80 characters")
    @Schema(description = "Competitor's nickname (must be unique)", example = "La joroba")
    String nickname,

    @NotNull(message = "The competitor type is mandatory")
    @Schema(description = "Type of competitor", example = "DWARF",
            allowableValues = {"DWARF", "CAMEL", "MEDIUM", "OTHER"})
    CompetitorType competitorType,

    @NotNull(message = "The competitor's age is mandatory")
    @Min(value = 0, message = "Competitor's age cannot be negative")
    @Schema(description = "Competitor's age", example = "21")
    int age,

    @NotNull(message = "The competitor's height is mandatory")
    @Min(value = 0, message = "Competitor's height cannot be negative")
    @Schema(description = "Competitor's height", example = "167")
    float height,

    @NotNull(message = "The competitor's weight is mandatory")
    @Min(value = 0, message = "Competitor's weight cannot be negative")
    @Schema(description = "Competitor's weight", example = "69")
    float weight,

    @NotBlank(message = "The competitor's place of origin is mandatory")
    @Size(min = 2, max = 150, message = "place of origin must be between 2 and 150 characters")
    @Schema(description = "Competitor's place of origin", example = "Pereira")
    String placeOfOrigin
) {

}
