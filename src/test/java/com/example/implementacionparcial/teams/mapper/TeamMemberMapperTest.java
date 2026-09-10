package com.example.implementacionparcial.teams.mapper;

import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.teams.dto.TeamMemberResponse;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamMember;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TeamMemberMapperTest {

    @Test
    void toResponse_mapsTeamIdAndCompetitorIdInCorrectPositions() {
        UUID memberId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        Date joinedAt = new Date();

        Team team = Team.builder().id(teamId).status(TeamStatus.ACTIVE).build();
        Competitor competitor = Competitor.builder()
                .id(competitorId)
                .competitorType(CompetitorType.DWARF)
                .competitorStatus(CompetitorStatus.ACTIVE)
                .build();

        TeamMember teamMember = TeamMember.builder()
                .id(memberId)
                .team(team)
                .competitor(competitor)
                .joinedAt(joinedAt)
                .build();

        TeamMemberResponse response = TeamMemberMapper.toResponse(teamMember);

        assertThat(response.id()).isEqualTo(memberId);
        assertThat(response.team_id()).isEqualTo(teamId);
        assertThat(response.competitor_id()).isEqualTo(competitorId);
        assertThat(response.joinedAt()).isEqualTo(joinedAt);
    }
}