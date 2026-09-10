package com.example.implementacionparcial.races.repository;

import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RaceRepositoryIntegrationTest {

    @Autowired
    private IRaceRepository raceRepository;

    private Race newRace(String name, RaceStatus status, RaceType type) {
        return Race.builder()
                .name(name)
                .description("desc")
                .scheduledAt(LocalDateTime.now().plusDays(10))
                .startLocation("A")
                .endLocation("B")
                .distanceMeters(1000.0)
                .maxParticipants(10)
                .type(type)
                .status(status)
                .organizerId(UUID.randomUUID())
                .registrationDeadline(LocalDateTime.now().plusDays(5))
                .build();
    }

    @Test
    void save_persistsRaceWithGeneratedIdAndTimestamps() {
        Race saved = raceRepository.save(newRace("Dune Dash", RaceStatus.DRAFT, RaceType.INDIVIDUAL));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void search_filtersByStatusTypeAndName() {
        raceRepository.save(newRace("Dune Dash", RaceStatus.DRAFT, RaceType.INDIVIDUAL));
        raceRepository.save(newRace("Team Trek", RaceStatus.OPEN_FOR_REGISTRATION, RaceType.TEAM));
        raceRepository.save(newRace("Desert Duo", RaceStatus.OPEN_FOR_REGISTRATION, RaceType.MIXED));

        Page<Race> byStatus = raceRepository.search(RaceStatus.OPEN_FOR_REGISTRATION, null, null, PageRequest.of(0, 10));
        assertThat(byStatus.getContent()).hasSize(2);

        Page<Race> byType = raceRepository.search(null, RaceType.TEAM, null, PageRequest.of(0, 10));
        assertThat(byType.getContent()).extracting(Race::getName).containsExactly("Team Trek");

        Page<Race> byNamePartial = raceRepository.search(null, null, "dune", PageRequest.of(0, 10));
        assertThat(byNamePartial.getContent()).extracting(Race::getName).containsExactly("Dune Dash");

        Page<Race> noFilters = raceRepository.search(null, null, null, PageRequest.of(0, 10));
        assertThat(noFilters.getContent()).hasSize(3);
    }

    @Test
    void search_paginatesResults() {
        for (int i = 0; i < 5; i++) {
            raceRepository.save(newRace("Race " + i, RaceStatus.DRAFT, RaceType.INDIVIDUAL));
        }

        Page<Race> firstPage = raceRepository.search(null, null, null, PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(5);
        assertThat(firstPage.getTotalPages()).isEqualTo(3);
    }
}
