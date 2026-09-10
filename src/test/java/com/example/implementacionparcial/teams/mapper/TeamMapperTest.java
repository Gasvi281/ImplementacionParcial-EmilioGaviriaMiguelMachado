package com.example.implementacionparcial.teams.mapper;

import com.example.implementacionparcial.teams.dto.TeamRequest;
import com.example.implementacionparcial.teams.dto.TeamResponse;
import com.example.implementacionparcial.teams.entity.Team;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TeamMapperTest {

    @Test
    void toEntity_mapsAllRequestFields_includingMaxMembers() {
        TeamRequest request = new TeamRequest(
                "Enanos del valle", "Un equipo conformado por jugadores retirados de baseball", "Chiqui Tapia", 8);

        Team team = TeamMapper.toEntity(request);

        assertThat(team.getName()).isEqualTo("Enanos del valle");
        assertThat(team.getDescription()).isEqualTo(request.description());
        assertThat(team.getCoach()).isEqualTo("Chiqui Tapia");
        assertThat(team.getMaxMembers()).isEqualTo(8);
        assertThat(team.getStatus()).isEqualTo(TeamStatus.ACTIVE); // default del builder
    }

    @Test
    void toResponse_mapsAllEntityFields_withEmptyMembers() {
        UUID id = UUID.randomUUID();
        Team team = Team.builder()
                .id(id)
                .name("Enanos del valle")
                .description("desc")
                .coach("Chiqui Tapia")
                .maxMembers(8)
                .status(TeamStatus.SUSPENDED)
                .members(Collections.emptyList())
                .build();

        TeamResponse response = TeamMapper.toResponse(team);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.maxMembers()).isEqualTo(8);
        assertThat(response.status()).isEqualTo(TeamStatus.SUSPENDED);
        assertThat(response.teamMembers()).isEmpty();
    }
}