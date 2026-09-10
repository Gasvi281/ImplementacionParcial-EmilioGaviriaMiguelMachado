package com.example.implementacionparcial.registrations.mapper;

import com.example.implementacionparcial.competitors.mapper.CompetitorMapper;
import com.example.implementacionparcial.races.mapper.RaceMapper;
import com.example.implementacionparcial.registrations.dto.RegistrationRequest;
import com.example.implementacionparcial.registrations.dto.RegistrationResponse;
import com.example.implementacionparcial.registrations.dto.RegistrationSummaryResponse;
import com.example.implementacionparcial.registrations.dto.RegistrationUpdateRequest;
import com.example.implementacionparcial.registrations.entity.RaceRegistration;
import com.example.implementacionparcial.teams.mapper.TeamMapper;

public final class RegistrationMapper {

    private RegistrationMapper() {
    }

    public static RaceRegistration toEntity(RegistrationRequest request) {
        return RaceRegistration.builder()
                .notes(request.notes())
                .build();
        // race/competitor/team se resuelven en el service (necesitan repositorios/services de otros dominios)
    }

    public static void applyApproval(RaceRegistration registration, RegistrationUpdateRequest request) {
        if (request.lane() != null) {
            registration.setLane(request.lane());
        }
        if (request.startPosition() != null) {
            registration.setStartPosition(request.startPosition());
        }
        if (request.notes() != null) {
            registration.setNotes(request.notes());
        }
    }

    public static RegistrationResponse toResponse(RaceRegistration registration) {
        return new RegistrationResponse(
                registration.getId(),
                RaceMapper.toSummary(registration.getRace()),
                registration.getCompetitor() == null ? null : CompetitorMapper.toSummary(registration.getCompetitor()),
                registration.getTeam() == null ? null : TeamMapper.toSummary(registration.getTeam()),
                registration.getStatus(),
                registration.getLane(),
                registration.getStartPosition(),
                registration.getNotes(),
                registration.getRegisteredByUserId(),
                registration.getRegisteredAt());
    }

    public static RegistrationSummaryResponse toSummary(RaceRegistration registration) {
        return new RegistrationSummaryResponse(
                registration.getId(), registration.getStatus(), registration.getLane(), registration.getStartPosition());
    }
}
