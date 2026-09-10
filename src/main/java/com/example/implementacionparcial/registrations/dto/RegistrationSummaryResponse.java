package com.example.implementacionparcial.registrations.dto;

import com.example.implementacionparcial.registrations.entity.RegistrationStatus;

import java.util.UUID;

public record RegistrationSummaryResponse(
        UUID id,
        RegistrationStatus status,
        Integer lane,
        Integer startPosition
) {
}
