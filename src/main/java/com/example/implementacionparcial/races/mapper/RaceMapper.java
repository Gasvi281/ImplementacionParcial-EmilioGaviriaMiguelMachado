package com.example.implementacionparcial.races.mapper;

import com.example.implementacionparcial.races.dto.RaceRequest;
import com.example.implementacionparcial.races.dto.RaceResponse;
import com.example.implementacionparcial.races.dto.RaceSummaryResponse;
import com.example.implementacionparcial.races.dto.RaceUpdateRequest;
import com.example.implementacionparcial.races.entity.Race;

public final class RaceMapper {

    private RaceMapper() {
    }

    public static Race toEntity(RaceRequest request) {
        return Race.builder()
                .name(request.name())
                .description(request.description())
                .scheduledAt(request.scheduledAt())
                .startLocation(request.startLocation())
                .endLocation(request.endLocation())
                .distanceMeters(request.distanceMeters())
                .maxParticipants(request.maxParticipants())
                .type(request.type())
                .registrationDeadline(request.registrationDeadline())
                .build();
    }

    public static void applyUpdate(Race race, RaceUpdateRequest request) {
        if (request.name() != null) {
            race.setName(request.name());
        }
        if (request.description() != null) {
            race.setDescription(request.description());
        }
        if (request.scheduledAt() != null) {
            race.setScheduledAt(request.scheduledAt());
        }
        if (request.startLocation() != null) {
            race.setStartLocation(request.startLocation());
        }
        if (request.endLocation() != null) {
            race.setEndLocation(request.endLocation());
        }
        if (request.distanceMeters() != null) {
            race.setDistanceMeters(request.distanceMeters());
        }
        if (request.maxParticipants() != null) {
            race.setMaxParticipants(request.maxParticipants());
        }
        if (request.registrationDeadline() != null) {
            race.setRegistrationDeadline(request.registrationDeadline());
        }
    }

    public static RaceResponse toResponse(Race race) {
        return new RaceResponse(
                race.getId(), race.getName(), race.getDescription(), race.getScheduledAt(),
                race.getStartLocation(), race.getEndLocation(), race.getDistanceMeters(),
                race.getMaxParticipants(), race.getType(), race.getStatus(), race.getOrganizerId(),
                race.getRegistrationDeadline(), race.getCreatedAt(), race.getUpdatedAt());
    }

    public static RaceSummaryResponse toSummary(Race race) {
        return new RaceSummaryResponse(race.getId(), race.getName(), race.getScheduledAt(),
                race.getType(), race.getStatus());
    }
}
