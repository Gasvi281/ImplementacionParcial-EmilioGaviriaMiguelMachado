package com.example.implementacionparcial.registrations.dto;

import java.util.UUID;

public record RegistrationRequest(
        UUID competitorId,
        UUID teamId,
        String notes
) {
    /**
     * Exactamente uno de los dos debe estar presente, nunca ambos ni ninguno.
     */
    public boolean isValid() {
        return (competitorId != null) ^ (teamId != null);
    }
}
