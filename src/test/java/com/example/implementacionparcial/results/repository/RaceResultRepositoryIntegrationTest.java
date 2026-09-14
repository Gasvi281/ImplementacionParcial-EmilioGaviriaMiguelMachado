package com.example.implementacionparcial.results.repository;

import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.competitors.repository.ICompetitorRepository;
import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.races.repository.IRaceRepository;
import com.example.implementacionparcial.results.entity.RaceResult;
import com.example.implementacionparcial.results.entity.ResultStatus;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import com.example.implementacionparcial.teams.repository.ITeamRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RaceResultRepositoryIntegrationTest {

    @Autowired
    private IRaceResultRepository resultRepository;
    @Autowired
    private IRaceRepository raceRepository;
    @Autowired
    private ICompetitorRepository competitorRepository;
    @Autowired
    private ITeamRepository teamRepository;

    private Race persistedRace() {
        return raceRepository.save(Race.builder()
                .name("Dune Dash")
                .scheduledAt(LocalDateTime.now().plusDays(10))
                .startLocation("A")
                .endLocation("B")
                .distanceMeters(1000.0)
                .maxParticipants(10)
                .type(RaceType.INDIVIDUAL)
                .status(RaceStatus.IN_PROGRESS)
                .organizerId(UUID.randomUUID())
                .registrationDeadline(LocalDateTime.now().plusDays(5))
                .build());
    }

    private Competitor persistedCompetitor() {
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

    private RaceResult newResult(Race race, Competitor competitor, Team team, ResultStatus status, Integer finalPosition) {
        return RaceResult.builder()
                .race(race)
                .competitor(competitor)
                .team(team)
                .status(status)
                .finalPosition(finalPosition)
                .completionTimeSeconds(finalPosition == null ? null : 100.0)
                .penaltyTimeSeconds(0.0)
                .recordedByUserId(UUID.randomUUID())
                .build();
    }

    @Test
    void save_persistsResultWithGeneratedIdAndTimestamps() {
        Race race = persistedRace();

        RaceResult saved = resultRepository.save(newResult(race, persistedCompetitor(), null, ResultStatus.FINISHED, 1));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRecordedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void search_filtersByStatusWithinRace() {
        Race race = persistedRace();
        resultRepository.save(newResult(race, persistedCompetitor(), null, ResultStatus.FINISHED, 1));
        resultRepository.save(newResult(race, persistedCompetitor(), null, ResultStatus.DID_NOT_FINISH, null));

        Page<RaceResult> finished = resultRepository.search(race.getId(), ResultStatus.FINISHED, PageRequest.of(0, 10));
        Page<RaceResult> all = resultRepository.search(race.getId(), null, PageRequest.of(0, 10));

        assertThat(finished.getContent()).hasSize(1);
        assertThat(all.getContent()).hasSize(2);
    }

    @Test
    void existsByRace_IdAndFinalPosition_detectsWinner() {
        Race race = persistedRace();
        resultRepository.save(newResult(race, persistedCompetitor(), null, ResultStatus.FINISHED, 1));

        assertThat(resultRepository.existsByRace_IdAndFinalPosition(race.getId(), 1)).isTrue();
        assertThat(resultRepository.existsByRace_IdAndFinalPosition(race.getId(), 2)).isFalse();
    }

    @Test
    void existsByRace_IdAndFinalPositionAndIdNot_excludesSelfButDetectsOthers() {
        Race race = persistedRace();
        RaceResult winner = resultRepository.save(newResult(race, persistedCompetitor(), null, ResultStatus.FINISHED, 1));
        RaceResult other = resultRepository.save(newResult(race, persistedCompetitor(), null, ResultStatus.FINISHED, 2));

        boolean occupiedBySomeoneElse = resultRepository.existsByRace_IdAndFinalPositionAndIdNot(race.getId(), 1, other.getId());
        boolean occupiedBySelf = resultRepository.existsByRace_IdAndFinalPositionAndIdNot(race.getId(), 1, winner.getId());

        assertThat(occupiedBySomeoneElse).isTrue();
        assertThat(occupiedBySelf).isFalse();
    }

    @Test
    void existsByRace_IdAndFinalPositionAndStatus_detectsDuplicateAmongFinishers() {
        Race race = persistedRace();
        resultRepository.save(newResult(race, persistedCompetitor(), null, ResultStatus.FINISHED, 2));

        boolean duplicated = resultRepository.existsByRace_IdAndFinalPositionAndStatus(race.getId(), 2, ResultStatus.FINISHED);
        boolean freePosition = resultRepository.existsByRace_IdAndFinalPositionAndStatus(race.getId(), 3, ResultStatus.FINISHED);

        assertThat(duplicated).isTrue();
        assertThat(freePosition).isFalse();
    }

    @Test
    void existsByRace_IdAndCompetitor_Id_detectsExistingResult() {
        Race race = persistedRace();
        Competitor competitor = persistedCompetitor();
        resultRepository.save(newResult(race, competitor, null, ResultStatus.FINISHED, 1));

        assertThat(resultRepository.existsByRace_IdAndCompetitor_Id(race.getId(), competitor.getId())).isTrue();
        assertThat(resultRepository.existsByRace_IdAndCompetitor_Id(race.getId(), UUID.randomUUID())).isFalse();
    }

    @Test
    void existsByRace_IdAndTeam_Id_detectsExistingResult() {
        Race race = persistedRace();
        Team team = persistedTeam();
        resultRepository.save(newResult(race, null, team, ResultStatus.FINISHED, 1));

        assertThat(resultRepository.existsByRace_IdAndTeam_Id(race.getId(), team.getId())).isTrue();
    }

    @Test
    void findAllForStandings_returnsAllResultsWithRelationsLoaded() {
        Race race = persistedRace();
        resultRepository.save(newResult(race, persistedCompetitor(), null, ResultStatus.FINISHED, 1));
        resultRepository.save(newResult(race, null, persistedTeam(), ResultStatus.FINISHED, 2));

        List<RaceResult> all = resultRepository.findAllForStandings();

        assertThat(all).hasSize(2);
        assertThat(all).allSatisfy(r -> assertThat(r.getRace()).isNotNull());
    }
}
