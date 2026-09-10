package com.example.implementacionparcial.registrations.dto;

import jakarta.validation.constraints.Positive;

/**
 * Body de {@code PATCH /api/registrations/{id}/approve}: permite asignar {@code lane}/
 * {@code startPosition} (y ajustar {@code notes}) en el mismo paso en que se aprueba la
 * inscripción. No existe un {@code PATCH} genérico sobre una inscripción ya creada, así que
 * este es el único DTO de actualización parcial del dominio.
 */
public record RegistrationUpdateRequest(
        @Positive(message = "lane must be greater than 0") Integer lane,
        @Positive(message = "startPosition must be greater than 0") Integer startPosition,
        String notes
) {
    public boolean isEmpty() {
        return lane == null && startPosition == null && notes == null;
    }
}
