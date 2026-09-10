package com.example.implementacionparcial.results.mapper;

import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.results.dto.ResultRequest;
import com.example.implementacionparcial.results.dto.ResultResponse;
import com.example.implementacionparcial.results.dto.ResultSummaryResponse;
import com.example.implementacionparcial.results.dto.ResultUpdateRequest;
import com.example.implementacionparcial.results.entity.RaceResult;
import com.example.implementacionparcial.results.entity.ResultStatus;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResultMapperTest {

    private Race sampleRace() {
        return Race.builder()
                .id(UUID.randomUUID())
                .name("Dune Dash")
                .type(RaceType.MIXED)
                .status(RaceStatus.IN_PROGRESS)
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
        return Team.builder().id(UUID.randomUUID()).name("Dune Runners").status(TeamStatus.ACTIVE).build();
    }

    @Test
    void toEntity_mapsFieldsAndDefaultsPenaltyToZeroWhenNotProvided() {
        ResultRequest request = new ResultRequest(
                UUID.randomUUID(), null, ResultStatus.FINISHED, 1, 1, 100.0, null, "great run");

        RaceResult result = ResultMapper.toEntity(request);

        assertThat(result.getStatus()).isEqualTo(ResultStatus.FINISHED);
        assertThat(result.getFinalPosition()).isEqualTo(1);
        assertThat(result.getCompletionTimeSeconds()).isEqualTo(100.0);
        assertThat(result.getPenaltyTimeSeconds()).isEqualTo(0.0);
        assertThat(result.getNotes()).isEqualTo("great run");
        assertThat(result.getRace()).isNull();
        assertThat(result.getCompetitor()).isNull();
    }

    @Test
    void applyUpdate_onlyOverwritesNonNullFields() {
        RaceResult result = RaceResult.builder()
                .id(UUID.randomUUID())
                .status(ResultStatus.DID_NOT_FINISH)
                .notes("original")
                .penaltyTimeSeconds(0.0)
                .build();

        ResultMapper.applyUpdate(result, new ResultUpdateRequest(ResultStatus.FINISHED, null, 2, 110.0, null, null));

        assertThat(result.getStatus()).isEqualTo(ResultStatus.FINISHED);
        assertThat(result.getFinalPosition()).isEqualTo(2);
        assertThat(result.getCompletionTimeSeconds()).isEqualTo(110.0);
        assertThat(result.getPenaltyTimeSeconds()).isEqualTo(0.0); // no vino en el patch, no cambia
        assertThat(result.getNotes()).isEqualTo("original"); // no vino en el patch, no cambia
    }

    @Test
    void toResponse_mapsCompetitorResultWithComputedTotalTimeAndPoints() {
        Race race = sampleRace();
        Competitor competitor = sampleCompetitor();
        UUID userId = UUID.randomUUID();
        RaceResult result = RaceResult.builder()
                .id(UUID.randomUUID())
                .race(race)
                .competitor(competitor)
                .status(ResultStatus.FINISHED)
                .finalPosition(2)
                .completionTimeSeconds(100.0)
                .penaltyTimeSeconds(5.0)
                .recordedByUserId(userId)
                .build();

        ResultResponse response = ResultMapper.toResponse(result);

        assertThat(response.id()).isEqualTo(result.getId());
        assertThat(response.race().id()).isEqualTo(race.getId());
        assertThat(response.competitor().id()).isEqualTo(competitor.getId());
        assertThat(response.team()).isNull();
        assertThat(response.totalTimeSeconds()).isEqualTo(105.0);
        assertThat(response.points()).isEqualTo(7);
        assertThat(response.recordedByUserId()).isEqualTo(userId);
    }

    @Test
    void toResponse_mapsTeamResultWithNullCompetitorAndNullTotalTimeWhenNoCompletionTime() {
        Race race = sampleRace();
        Team team = sampleTeam();
        RaceResult result = RaceResult.builder()
                .id(UUID.randomUUID())
                .race(race)
                .team(team)
                .status(ResultStatus.DID_NOT_START)
                .penaltyTimeSeconds(0.0)
                .recordedByUserId(UUID.randomUUID())
                .build();

        ResultResponse response = ResultMapper.toResponse(result);

        assertThat(response.competitor()).isNull();
        assertThat(response.team().id()).isEqualTo(team.getId());
        assertThat(response.totalTimeSeconds()).isNull();
        assertThat(response.points()).isEqualTo(0);
    }

    @Test
    void toSummary_mapsMinimalFields() {
        RaceResult result = RaceResult.builder()
                .id(UUID.randomUUID())
                .status(ResultStatus.FINISHED)
                .finalPosition(3)
                .completionTimeSeconds(90.0)
                .penaltyTimeSeconds(0.0)
                .build();

        ResultSummaryResponse summary = ResultMapper.toSummary(result);

        assertThat(summary.id()).isEqualTo(result.getId());
        assertThat(summary.finalPosition()).isEqualTo(3);
        assertThat(summary.status()).isEqualTo(ResultStatus.FINISHED);
        assertThat(summary.totalTimeSeconds()).isEqualTo(90.0);
    }

    @Test
    void pointsFor_matchesTheLiteralPointsTable() {
        assertThat(ResultMapper.pointsFor(1, ResultStatus.FINISHED)).isEqualTo(10);
        assertThat(ResultMapper.pointsFor(2, ResultStatus.FINISHED)).isEqualTo(7);
        assertThat(ResultMapper.pointsFor(3, ResultStatus.FINISHED)).isEqualTo(5);
        assertThat(ResultMapper.pointsFor(4, ResultStatus.FINISHED)).isEqualTo(3);
        assertThat(ResultMapper.pointsFor(5, ResultStatus.FINISHED)).isEqualTo(1);
        assertThat(ResultMapper.pointsFor(6, ResultStatus.FINISHED)).isEqualTo(0);
        assertThat(ResultMapper.pointsFor(1, ResultStatus.DISQUALIFIED)).isEqualTo(0);
        assertThat(ResultMapper.pointsFor(null, ResultStatus.FINISHED)).isEqualTo(0);
    }
}
