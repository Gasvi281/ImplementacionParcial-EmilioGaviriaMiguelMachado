package com.example.implementacionparcial.teams.controller;

import com.example.implementacionparcial.common.config.SecurityConfig;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.teams.dto.TeamMemberResponse;
import com.example.implementacionparcial.teams.dto.TeamRequest;
import com.example.implementacionparcial.teams.dto.TeamResponse;
import com.example.implementacionparcial.teams.entity.TeamStatus;
import com.example.implementacionparcial.teams.service.TeamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TeamController.class)
@Import(SecurityConfig.class)
class TeamControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private TeamService teamService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TeamRequest sampleRequest() {
        return new TeamRequest("Enanos del valle", "Un equipo conformado por jugadores retirados de baseball", "Chiqui Tapia", 8);
    }

    private TeamResponse sampleResponse(UUID id) {
        return new TeamResponse(id, "Enanos del valle", "desc", "Chiqui Tapia", 8, new Date(), TeamStatus.ACTIVE, List.of());
    }

    @Test
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/teams"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_asAdministrator_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(teamService.createTeam(any(TeamRequest.class))).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/teams")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void create_asNonAdministrator_returns403() throws Exception {
        mockMvc.perform(post("/api/teams")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void addMember_whenTeamFull_returns409() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        when(teamService.addTeamMember(teamId, competitorId))
                .thenThrow(new ConflictException("Team with id " + teamId + " is already full"));

        mockMvc.perform(post("/api/teams/{teamId}/members/{competitorId}", teamId, competitorId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                .andExpect(status().isConflict());
    }

    @Test
    void addMember_asAdministrator_returns201() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        when(teamService.addTeamMember(teamId, competitorId))
                .thenReturn(new TeamMemberResponse(UUID.randomUUID(), teamId, competitorId, new Date()));

        mockMvc.perform(post("/api/teams/{teamId}/members/{competitorId}", teamId, competitorId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                .andExpect(status().isCreated());
    }

    @Test
    void removeMember_whenNotFound_returns404() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Competitor " + competitorId + " is not a member of team " + teamId))
                .when(teamService).removeTeamMember(teamId, competitorId);

        mockMvc.perform(delete("/api/teams/{teamId}/members/{competitorId}", teamId, competitorId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeMember_asAdministrator_returns204() throws Exception {
        UUID teamId = UUID.randomUUID();
        UUID competitorId = UUID.randomUUID();

        mockMvc.perform(delete("/api/teams/{teamId}/members/{competitorId}", teamId, competitorId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                .andExpect(status().isNoContent());
    }
}