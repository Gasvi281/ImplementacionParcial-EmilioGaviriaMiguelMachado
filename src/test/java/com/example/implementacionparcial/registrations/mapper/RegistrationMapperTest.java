package com.example.implementacionparcial.registrations.mapper;

import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.registrations.dto.RegistrationRequest;
import com.example.implementacionparcial.registrations.dto.RegistrationResponse;
import com.example.implementacionparcial.registrations.dto.RegistrationSummaryResponse;
import com.example.implementacionparcial.registrations.dto.RegistrationUpdateRequest;
import com.example.implementacionparcial.registrations.entity.RaceRegistration;
import com.example.implementacionparcial.registrations.entity.RegistrationStatus;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RegistrationMapperTest {

    private Race sampleRace() {
        return Race.builder()
                .id(UUID.randomUUID())
                .name("Dune Dash")
                .scheduledAt(LocalDateTime.now().plusDays(10))
                .type(RaceType.MIXED)
                .status(RaceStatus.OPEN_FOR_REGISTRATION)
                .build();
    }

    private Competitor sampleCompetitor() {
        return Competitor.builder()
                .id(UUID.randomUUID())
                .name("Speedy")
                .nickname("speedy")
                .type(CompetitorType.CAMEL)
                .status(CompetitorStatus.ACTIVE)
                .build();
    }

    private Team sampleTeam() {
        return Team.builder()
                .id(UUID.randomUUID())
                .name("Dune Runners")
                .status(TeamStatus.ACTIVE)
                .build();
    }

    @Test
    void toEntity_mapsNotesAndDefaultsToPendingStatus() {
        RegistrationRequest request = new RegistrationRequest(UUID.randomUUID(), null, "first attempt");

        RaceRegistration registration = RegistrationMapper.toEntity(request);

        assertThat(registration.getNotes()).isEqualTo("first attempt");
        assertThat(registration.getStatus()).isEqualTo(RegistrationStatus.PENDING);
        assertThat(registration.getRace()).isNull();
        assertThat(registration.getCompetitor()).isNull();
        assertThat(registration.getTeam()).isNull();
    }

    @Test
    void applyApproval_onlyOverwritesNonNullFields() {
        RaceRegistration registration = RaceRegistration.builder()
                .id(UUID.randomUUID())
                .notes("original notes")
                .build();

        RegistrationMapper.applyApproval(registration, new RegistrationUpdateRequest(3, null, null));

        assertThat(registration.getLane()).isEqualTo(3);
        assertThat(registration.getStartPosition()).isNull();
        assertThat(registration.getNotes()).isEqualTo("original notes");
    }

    @Test
    void toResponse_mapsCompetitorRegistrationWithNullTeam() {
        Race race = sampleRace();
        Competitor competitor = sampleCompetitor();
        UUID userId = UUID.randomUUID();
        RaceRegistration registration = RaceRegistration.builder()
                .id(UUID.randomUUID())
                .race(race)
                .competitor(competitor)
                .status(RegistrationStatus.APPROVED)
                .lane(2)
                .startPosition(1)
                .notes("ok")
                .registeredByUserId(userId)
                .registeredAt(LocalDateTime.now())
                .build();

        RegistrationResponse response = RegistrationMapper.toResponse(registration);

        assertThat(response.id()).isEqualTo(registration.getId());
        assertThat(response.race().id()).isEqualTo(race.getId());
        assertThat(response.competitor().id()).isEqualTo(competitor.getId());
        assertThat(response.team()).isNull();
        assertThat(response.status()).isEqualTo(RegistrationStatus.APPROVED);
        assertThat(response.lane()).isEqualTo(2);
        assertThat(response.startPosition()).isEqualTo(1);
        assertThat(response.registeredByUserId()).isEqualTo(userId);
    }

    @Test
    void toResponse_mapsTeamRegistrationWithNullCompetitor() {
        Race race = sampleRace();
        Team team = sampleTeam();
        RaceRegistration registration = RaceRegistration.builder()
                .id(UUID.randomUUID())
                .race(race)
                .team(team)
                .status(RegistrationStatus.PENDING)
                .registeredByUserId(UUID.randomUUID())
                .registeredAt(LocalDateTime.now())
                .build();

        RegistrationResponse response = RegistrationMapper.toResponse(registration);

        assertThat(response.competitor()).isNull();
        assertThat(response.team().id()).isEqualTo(team.getId());
    }

    @Test
    void toSummary_mapsMinimalFields() {
        RaceRegistration registration = RaceRegistration.builder()
                .id(UUID.randomUUID())
                .status(RegistrationStatus.APPROVED)
                .lane(4)
                .startPosition(2)
                .build();

        RegistrationSummaryResponse summary = RegistrationMapper.toSummary(registration);

        assertThat(summary.id()).isEqualTo(registration.getId());
        assertThat(summary.status()).isEqualTo(RegistrationStatus.APPROVED);
        assertThat(summary.lane()).isEqualTo(4);
        assertThat(summary.startPosition()).isEqualTo(2);
    }
}
