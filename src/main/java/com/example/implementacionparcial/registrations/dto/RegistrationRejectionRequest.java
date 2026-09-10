package com.example.implementacionparcial.registrations.dto;

import jakarta.validation.constraints.NotBlank;

public record RegistrationRejectionRequest(
        @NotBlank(message = "notes is mandatory to reject a registration") String notes
) {
}
