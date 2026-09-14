package com.example.implementacionparcial.results.controller;

import com.example.implementacionparcial.common.config.SecurityConfig;
import com.example.implementacionparcial.competitors.dto.CompetitorSummaryResponse;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.results.dto.StandingResponse;
import com.example.implementacionparcial.results.service.StandingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StandingController.class)
@Import(SecurityConfig.class)
class StandingControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StandingService standingService;

    private StandingResponse sampleStanding() {
        CompetitorSummaryResponse competitor = new CompetitorSummaryResponse(UUID.randomUUID(), "speedy", CompetitorType.CAMEL);
        return new StandingResponse(1, competitor, null, 3, 2, 3, 25, 1, 280.0);
    }

    @Test
    void getGlobal_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/standings")).andExpect(status().isUnauthorized());
    }

    @Test
    void getGlobal_withAnyAuthenticatedRole_returns200() throws Exception {
        when(standingService.getGlobal(any())).thenReturn(new PageImpl<>(List.of(sampleStanding())));

        mockMvc.perform(get("/api/standings")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].rank").value(1));
    }

    @Test
    void getCompetitorStandings_returns200() throws Exception {
        when(standingService.getCompetitorStandings(any())).thenReturn(new PageImpl<>(List.of(sampleStanding())));

        mockMvc.perform(get("/api/standings/competitors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RACE_ORGANIZER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getTeamStandings_returns200() throws Exception {
        when(standingService.getTeamStandings(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/standings/teams")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                .andExpect(status().isOk());
    }
}
