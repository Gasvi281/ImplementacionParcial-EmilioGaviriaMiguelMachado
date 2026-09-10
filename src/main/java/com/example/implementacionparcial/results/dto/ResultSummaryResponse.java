package com.example.implementacionparcial.results.dto;

import com.example.implementacionparcial.results.entity.ResultStatus;

import java.util.UUID;

public record ResultSummaryResponse(
        UUID id,
        Integer finalPosition,
        ResultStatus status,
        Double totalTimeSeconds
) {
}
