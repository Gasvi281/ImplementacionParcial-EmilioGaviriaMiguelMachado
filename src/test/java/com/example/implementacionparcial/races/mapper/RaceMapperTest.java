package com.example.implementacionparcial.races.mapper;

import com.example.implementacionparcial.races.dto.RaceRequest;
import com.example.implementacionparcial.races.dto.RaceResponse;
import com.example.implementacionparcial.races.dto.RaceSummaryResponse;
import com.example.implementacionparcial.races.dto.RaceUpdateRequest;
import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RaceMapperTest {

    private RaceRequest sampleRequest() {
        return new RaceRequest(
                "Dune Dash", "A camel race across the dunes",
                LocalDateTime.now().plusDays(10),
                "Oasis Camp", "Sunset Ridge",
                5000.0, 20, RaceType.INDIVIDUAL,
                LocalDateTime.now().plusDays(5));
    }

    @Test
    void toEntity_mapsAllRequestFields() {
        RaceRequest request = sampleRequest();

        Race race = RaceMapper.toEntity(request);

        assertThat(race.getName()).isEqualTo(request.name());
        assertThat(race.getDescription()).isEqualTo(request.description());
        assertThat(race.getScheduledAt()).isEqualTo(request.scheduledAt());
        assertThat(race.getStartLocation()).isEqualTo(request.startLocation());
        assertThat(race.getEndLocation()).isEqualTo(request.endLocation());
        assertThat(race.getDistanceMeters()).isEqualTo(request.distanceMeters());
        assertThat(race.getMaxParticipants()).isEqualTo(request.maxParticipants());
        assertThat(race.getType()).isEqualTo(request.type());
        assertThat(race.getRegistrationDeadline()).isEqualTo(request.registrationDeadline());
        assertThat(race.getStatus()).isEqualTo(RaceStatus.DRAFT);
    }

    @Test
    void applyUpdate_onlyOverwritesNonNullFields() {
        Race race = Race.builder()
                .id(UUID.randomUUID())
                .name("Original Name")
                .description("Original description")
                .scheduledAt(LocalDateTime.now().plusDays(10))
                .startLocation("A")
                .endLocation("B")
                .distanceMeters(1000.0)
                .maxParticipants(10)
                .type(RaceType.TEAM)
                .status(RaceStatus.DRAFT)
                .registrationDeadline(LocalDateTime.now().plusDays(5))
                .build();

        RaceUpdateRequest update = new RaceUpdateRequest(
                "Updated Name", null, null, null, null, null, null, null);

        RaceMapper.applyUpdate(race, update);

        assertThat(race.getName()).isEqualTo("Updated Name");
        assertThat(race.getDescription()).isEqualTo("Original description");
        assertThat(race.getDistanceMeters()).isEqualTo(1000.0);
        assertThat(race.getMaxParticipants()).isEqualTo(10);
    }

    @Test
    void toResponse_mapsAllEntityFields() {
        UUID organizerId = UUID.randomUUID();
        Race race = Race.builder()
                .id(UUID.randomUUID())
                .name("Dune Dash")
                .description("desc")
                .scheduledAt(LocalDateTime.now().plusDays(10))
                .startLocation("A")
                .endLocation("B")
                .distanceMeters(5000.0)
                .maxParticipants(20)
                .type(RaceType.MIXED)
                .status(RaceStatus.OPEN_FOR_REGISTRATION)
                .organizerId(organizerId)
                .registrationDeadline(LocalDateTime.now().plusDays(5))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        RaceResponse response = RaceMapper.toResponse(race);

        assertThat(response.id()).isEqualTo(race.getId());
        assertThat(response.name()).isEqualTo(race.getName());
        assertThat(response.status()).isEqualTo(RaceStatus.OPEN_FOR_REGISTRATION);
        assertThat(response.organizerId()).isEqualTo(organizerId);
    }

    @Test
    void toSummary_mapsOnlyMinimalFields() {
        Race race = Race.builder()
                .id(UUID.randomUUID())
                .name("Dune Dash")
                .scheduledAt(LocalDateTime.now().plusDays(10))
                .type(RaceType.INDIVIDUAL)
                .status(RaceStatus.DRAFT)
                .build();

        RaceSummaryResponse summary = RaceMapper.toSummary(race);

        assertThat(summary.id()).isEqualTo(race.getId());
        assertThat(summary.name()).isEqualTo(race.getName());
        assertThat(summary.type()).isEqualTo(RaceType.INDIVIDUAL);
        assertThat(summary.status()).isEqualTo(RaceStatus.DRAFT);
    }
}
