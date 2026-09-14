package com.example.implementacionparcial.registrations.repository;

import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.competitors.repository.ICompetitorRepository;
import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.races.repository.IRaceRepository;
import com.example.implementacionparcial.registrations.entity.RaceRegistration;
import com.example.implementacionparcial.registrations.entity.RegistrationStatus;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import com.example.implementacionparcial.teams.repository.ITeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RaceRegistrationRepositoryIntegrationTest {

    @Autowired
    private IRaceRegistrationRepository registrationRepository;
    @Autowired
    private IRaceRepository raceRepository;
    @Autowired
    private ICompetitorRepository competitorRepository;
    @Autowired
    private ITeamRepository teamRepository;

    private Race persistedRace(RaceType type) {
        return raceRepository.save(Race.builder()
                .name("Dune Dash")
                .scheduledAt(LocalDateTime.now().plusDays(10))
                .startLocation("A")
                .endLocation("B")
                .distanceMeters(1000.0)
                .maxParticipants(10)
                .type(type)
                .status(RaceStatus.OPEN_FOR_REGISTRATION)
                .organizerId(UUID.randomUUID())
                .registrationDeadline(LocalDateTime.now().plusDays(5))
                .build());
    }

    private Competitor persistedCompetitor(Team team) {
        return competitorRepository.save(Competitor.builder()
                .name("Speedy")
                .nickname("speedy-" + UUID.randomUUID())
                .competitorType(CompetitorType.CAMEL)
                .competitorStatus(CompetitorStatus.ACTIVE)
                .age(25)
                .height(1.5f)
                .weight(70f)
                .placeOfOrigin("Medellin")
                .build());
    }

    private Team persistedTeam() {
        return teamRepository.save(Team.builder()
                .name("Dune Runners")
                .description("Equipo de prueba para los tests de integración")
                .coach("Test coach")
                .maxMembers(10)
                .status(TeamStatus.ACTIVE)
                .build());
    }

    private RaceRegistration newRegistration(Race race, Competitor competitor, Team team, RegistrationStatus status) {
        return RaceRegistration.builder()
                .race(race)
                .competitor(competitor)
                .team(team)
                .status(status)
                .registeredByUserId(UUID.randomUUID())
                .build();
    }

    @Test
    void save_persistsRegistrationWithGeneratedIdAndRegisteredAt() {
        Race race = persistedRace(RaceType.INDIVIDUAL);
        Competitor competitor = persistedCompetitor(null);

        RaceRegistration saved = registrationRepository.save(
                newRegistration(race, competitor, null, RegistrationStatus.PENDING));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRegisteredAt()).isNotNull();
    }

    @Test
    void countByRace_IdAndStatus_countsOnlyMatchingStatus() {
        Race race = persistedRace(RaceType.INDIVIDUAL);
        registrationRepository.save(newRegistration(race, persistedCompetitor(null), null, RegistrationStatus.APPROVED));
        registrationRepository.save(newRegistration(race, persistedCompetitor(null), null, RegistrationStatus.APPROVED));
        registrationRepository.save(newRegistration(race, persistedCompetitor(null), null, RegistrationStatus.PENDING));

        long approved = registrationRepository.countByRace_IdAndStatus(race.getId(), RegistrationStatus.APPROVED);

        assertThat(approved).isEqualTo(2);
    }

    @Test
    void search_filtersByStatusWithinRace() {
        Race race = persistedRace(RaceType.INDIVIDUAL);
        registrationRepository.save(newRegistration(race, persistedCompetitor(null), null, RegistrationStatus.PENDING));
        registrationRepository.save(newRegistration(race, persistedCompetitor(null), null, RegistrationStatus.APPROVED));

        Page<RaceRegistration> pending = registrationRepository.search(race.getId(), RegistrationStatus.PENDING, PageRequest.of(0, 10));
        Page<RaceRegistration> all = registrationRepository.search(race.getId(), null, PageRequest.of(0, 10));

        assertThat(pending.getContent()).hasSize(1);
        assertThat(all.getContent()).hasSize(2);
    }

    @Test
    void existsByRace_IdAndCompetitor_IdAndStatusIn_detectsExistingRegistration() {
        Race race = persistedRace(RaceType.INDIVIDUAL);
        Competitor competitor = persistedCompetitor(null);
        registrationRepository.save(newRegistration(race, competitor, null, RegistrationStatus.PENDING));

        boolean exists = registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(
                race.getId(), competitor.getId(), Set.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED));
        boolean existsForOtherStatus = registrationRepository.existsByRace_IdAndCompetitor_IdAndStatusIn(
                race.getId(), competitor.getId(), Set.of(RegistrationStatus.REJECTED));

        assertThat(exists).isTrue();
        assertThat(existsForOtherStatus).isFalse();
    }

    @Test
    void existsByRace_IdAndTeam_IdAndStatusIn_detectsExistingRegistration() {
        Race race = persistedRace(RaceType.TEAM);
        Team team = persistedTeam();
        registrationRepository.save(newRegistration(race, null, team, RegistrationStatus.APPROVED));

        boolean exists = registrationRepository.existsByRace_IdAndTeam_IdAndStatusIn(
                race.getId(), team.getId(), Set.of(RegistrationStatus.PENDING, RegistrationStatus.APPROVED));

        assertThat(exists).isTrue();
    }

    @Test
    void existsByRace_IdAndLaneAndIdNot_excludesSameRegistrationButDetectsOthers() {
        Race race = persistedRace(RaceType.INDIVIDUAL);
        RaceRegistration first = registrationRepository.save(
                newRegistration(race, persistedCompetitor(null), null, RegistrationStatus.APPROVED));
        first.setLane(3);
        registrationRepository.save(first);
        RaceRegistration second = registrationRepository.save(
                newRegistration(race, persistedCompetitor(null), null, RegistrationStatus.PENDING));

        boolean occupiedBySomeoneElse = registrationRepository.existsByRace_IdAndLaneAndIdNot(race.getId(), 3, second.getId());
        boolean occupiedBySelf = registrationRepository.existsByRace_IdAndLaneAndIdNot(race.getId(), 3, first.getId());

        assertThat(occupiedBySomeoneElse).isTrue();
        assertThat(occupiedBySelf).isFalse();
    }
}
