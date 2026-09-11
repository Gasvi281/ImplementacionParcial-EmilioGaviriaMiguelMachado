package com.example.implementacionparcial.results.service;

import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.results.dto.StandingResponse;
import com.example.implementacionparcial.results.entity.RaceResult;
import com.example.implementacionparcial.results.entity.ResultStatus;
import com.example.implementacionparcial.results.repository.IRaceResultRepository;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Sin Spring: StandingService solo lee de IRaceResultRepository (ver .claude/rules/results.md),
 * así que un mock alcanza para validar la agregación entera.
 */
@ExtendWith(MockitoExtension.class)
class StandingServiceTest {

    @Mock
    private IRaceResultRepository resultRepository;

    @InjectMocks
    private StandingService standingService;

    private Competitor competitor(String nickname) {
        return Competitor.builder()
                .id(UUID.randomUUID())
                .name(nickname)
                .nickname(nickname + "-" + UUID.randomUUID())
                .competitorType(CompetitorType.CAMEL)
                .competitorStatus(CompetitorStatus.ACTIVE)
                .build();
    }

    private Team team(String name) {
        return Team.builder().id(UUID.randomUUID()).name(name).status(TeamStatus.ACTIVE).build();
    }

    private Race race() {
        return Race.builder()
                .id(UUID.randomUUID())
                .name("Dune Dash")
                .type(RaceType.INDIVIDUAL)
                .status(RaceStatus.IN_PROGRESS)
                .build();
    }

    private RaceResult finished(Race race, Competitor competitor, Team team, int position, double time) {
        return RaceResult.builder()
                .id(UUID.randomUUID())
                .race(race)
                .competitor(competitor)
                .team(team)
                .status(ResultStatus.FINISHED)
                .finalPosition(position)
                .completionTimeSeconds(time)
                .penaltyTimeSeconds(0.0)
                .build();
    }

    private RaceResult didNotFinish(Race race, Competitor competitor) {
        return RaceResult.builder()
                .id(UUID.randomUUID())
                .race(race)
                .competitor(competitor)
                .status(ResultStatus.DID_NOT_FINISH)
                .penaltyTimeSeconds(0.0)
                .build();
    }

    @Test
    void getGlobal_accumulatesPointsWinsPodiumsAndTotalTimeAcrossRaces() {
        Competitor camel = competitor("speedy");
        when(resultRepository.findAllForStandings()).thenReturn(List.of(
                finished(race(), camel, null, 1, 100.0),
                finished(race(), camel, null, 3, 90.0)));

        Page<StandingResponse> page = standingService.getGlobal(PageRequest.of(0, 10));

        StandingResponse standing = page.getContent().get(0);
        assertThat(standing.rank()).isEqualTo(1);
        assertThat(standing.totalPoints()).isEqualTo(15); // 10 (1º) + 5 (3º)
        assertThat(standing.wins()).isEqualTo(1);
        assertThat(standing.podiums()).isEqualTo(2);
        assertThat(standing.racesParticipated()).isEqualTo(2);
        assertThat(standing.bestPosition()).isEqualTo(1);
        assertThat(standing.totalTimeSeconds()).isEqualTo(190.0);
    }

    @Test
    void getGlobal_countsDnfAsParticipationWithZeroPointsAndNoTime() {
        Competitor laggard = competitor("laggard");
        when(resultRepository.findAllForStandings()).thenReturn(List.of(didNotFinish(race(), laggard)));

        Page<StandingResponse> page = standingService.getGlobal(PageRequest.of(0, 10));

        StandingResponse standing = page.getContent().get(0);
        assertThat(standing.racesParticipated()).isEqualTo(1);
        assertThat(standing.totalPoints()).isEqualTo(0);
        assertThat(standing.wins()).isEqualTo(0);
        assertThat(standing.totalTimeSeconds()).isNull();
        assertThat(standing.bestPosition()).isNull();
    }

    @Test
    void getGlobal_ordersByPointsThenWinsThenTotalTimeAndAssignsRank() {
        Race r = race();
        Competitor first = competitor("first");   // 10 puntos
        Competitor second = competitor("second"); // 7 puntos
        Competitor third = competitor("third");   // 5 puntos
        when(resultRepository.findAllForStandings()).thenReturn(List.of(
                finished(r, second, null, 2, 100.0),
                finished(r, first, null, 1, 90.0),
                finished(r, third, null, 3, 80.0)));

        Page<StandingResponse> page = standingService.getGlobal(PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(s -> s.competitor().id())
                .containsExactly(first.getId(), second.getId(), third.getId());
        assertThat(page.getContent()).extracting(StandingResponse::rank).containsExactly(1, 2, 3);
    }

    @Test
    void getCompetitorStandings_excludesTeamResults() {
        Race r = race();
        Competitor camel = competitor("speedy");
        Team squad = team("Dune Runners");
        when(resultRepository.findAllForStandings()).thenReturn(List.of(
                finished(r, camel, null, 1, 100.0),
                finished(r, null, squad, 2, 100.0)));

        Page<StandingResponse> page = standingService.getCompetitorStandings(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).competitor().id()).isEqualTo(camel.getId());
    }

    @Test
    void getTeamStandings_excludesCompetitorResults() {
        Race r = race();
        Competitor camel = competitor("speedy");
        Team squad = team("Dune Runners");
        when(resultRepository.findAllForStandings()).thenReturn(List.of(
                finished(r, camel, null, 1, 100.0),
                finished(r, null, squad, 2, 100.0)));

        Page<StandingResponse> page = standingService.getTeamStandings(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).team().id()).isEqualTo(squad.getId());
    }

    @Test
    void getGlobal_paginatesPreservingRankAcrossPages() {
        Race r = race();
        Competitor a = competitor("a");
        Competitor b = competitor("b");
        Competitor c = competitor("c");
        when(resultRepository.findAllForStandings()).thenReturn(List.of(
                finished(r, a, null, 1, 90.0),
                finished(r, b, null, 2, 95.0),
                finished(r, c, null, 3, 100.0)));

        Page<StandingResponse> secondPage = standingService.getGlobal(PageRequest.of(1, 2));

        assertThat(secondPage.getContent()).hasSize(1);
        assertThat(secondPage.getContent().get(0).rank()).isEqualTo(3);
        assertThat(secondPage.getTotalElements()).isEqualTo(3);
    }
}
