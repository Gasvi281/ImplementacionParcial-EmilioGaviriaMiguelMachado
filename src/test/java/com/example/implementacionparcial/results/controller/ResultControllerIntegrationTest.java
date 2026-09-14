package com.example.implementacionparcial.results.controller;

import com.example.implementacionparcial.common.config.SecurityConfig;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.competitors.dto.CompetitorSummaryResponse;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.races.dto.RaceSummaryResponse;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.results.dto.ResultRequest;
import com.example.implementacionparcial.results.dto.ResultResponse;
import com.example.implementacionparcial.results.dto.ResultUpdateRequest;
import com.example.implementacionparcial.results.entity.ResultStatus;
import com.example.implementacionparcial.results.service.ResultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ResultController.class)
@Import(SecurityConfig.class)
class ResultControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ResultService resultService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private ResultResponse sampleResponse(UUID id, ResultStatus status) {
        RaceSummaryResponse race = new RaceSummaryResponse(UUID.randomUUID(), "Dune Dash",
                LocalDateTime.now().plusDays(10), RaceType.INDIVIDUAL, RaceStatus.IN_PROGRESS);
        CompetitorSummaryResponse competitor = new CompetitorSummaryResponse(UUID.randomUUID(), "speedy", CompetitorType.CAMEL);
        return new ResultResponse(id, race, competitor, null, status, 1, 1, 100.0, 0.0, 100.0, 10,
                null, UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());
    }

    private ResultRequest sampleRequest() {
        return new ResultRequest(UUID.randomUUID(), null, ResultStatus.FINISHED, 1, 1, 100.0, 0.0, null);
    }

    @Test
    void getAllForRace_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/races/{raceId}/results", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllForRace_withAnyAuthenticatedRole_returns200() throws Exception {
        Page<ResultResponse> page = new PageImpl<>(List.of(sampleResponse(UUID.randomUUID(), ResultStatus.FINISHED)));
        when(resultService.getAllForRace(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/races/{raceId}/results", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getById_whenMissing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(resultService.getById(id)).thenThrow(ResourceNotFoundException.of("RaceResult", id));

        mockMvc.perform(get("/api/results/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_asOrganizer_returns201WithLocationHeader() throws Exception {
        UUID raceId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(resultService.create(eq(raceId), any(ResultRequest.class)))
                .thenReturn(sampleResponse(id, ResultStatus.FINISHED));

        mockMvc.perform(post("/api/races/{raceId}/results", raceId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RACE_ORGANIZER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/results/" + id));
    }

    @Test
    void create_asViewer_returns403() throws Exception {
        mockMvc.perform(post("/api/races/{raceId}/results", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_whenBusinessRuleViolated_returns409() throws Exception {
        UUID raceId = UUID.randomUUID();
        when(resultService.create(eq(raceId), any(ResultRequest.class)))
                .thenThrow(new ConflictException("This race already has an official winner"));

        mockMvc.perform(post("/api/races/{raceId}/results", raceId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isConflict());
    }

    @Test
    void update_asOrganizer_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(resultService.update(eq(id), any(ResultUpdateRequest.class)))
                .thenReturn(sampleResponse(id, ResultStatus.FINISHED));

        mockMvc.perform(patch("/api/results/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RACE_ORGANIZER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ResultUpdateRequest(ResultStatus.FINISHED, null, 1, 95.0, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));
    }

    @Test
    void update_asViewer_returns403() throws Exception {
        mockMvc.perform(patch("/api/results/{id}", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new ResultUpdateRequest(ResultStatus.FINISHED, null, 1, 95.0, null, null))))
                .andExpect(status().isForbidden());
    }
}
