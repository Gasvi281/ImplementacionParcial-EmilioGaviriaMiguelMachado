package com.example.implementacionparcial.competitors.controller;

import com.example.implementacionparcial.common.config.SecurityConfig;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.competitors.dto.CompetitorRequest;
import com.example.implementacionparcial.competitors.dto.CompetitorResponse;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.competitors.service.CompetitorService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CompetitorController.class)
@Import(SecurityConfig.class)
class CompetitorControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CompetitorService competitorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private CompetitorResponse sampleResponse(UUID id) {
        return new CompetitorResponse(id, "Javier", "javi123", CompetitorType.DWARF,
                30, 1.2f, 60f, "Medellín", CompetitorStatus.ACTIVE, new Date(), List.of());
    }

    private CompetitorRequest sampleRequest() {
        return new CompetitorRequest("Javier", "javi123", CompetitorType.DWARF, 30, 1.2f, 60f, "Medellín");
    }

    @Test
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/competitors"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_withAnyAuthenticatedRole_returns200() throws Exception {
        when(competitorService.getCompetitors()).thenReturn(List.of(sampleResponse(UUID.randomUUID())));

        mockMvc.perform(get("/api/competitors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getById_whenMissing_returns404WithErrorBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(competitorService.getById(id)).thenThrow(ResourceNotFoundException.of("Competitor", id));

        mockMvc.perform(get("/api/competitors/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/competitors/" + id));
    }

    @Test
    void create_asAdministrator_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        when(competitorService.createCompetitor(any(CompetitorRequest.class))).thenReturn(sampleResponse(id));

        mockMvc.perform(post("/api/competitors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated());
    }

    @Test
    void create_asNonAdministrator_returns403() throws Exception {
        mockMvc.perform(post("/api/competitors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withInvalidBody_returns400WithValidationErrors() throws Exception {
        CompetitorRequest invalid = new CompetitorRequest("", "", null, -1, -1f, -1f, "");

        mockMvc.perform(post("/api/competitors")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void delete_asAdministrator_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/competitors/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_whenNotRetired_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(competitorService.getById(any())).thenReturn(sampleResponse(id));
        org.mockito.Mockito.doThrow(new ConflictException("Only RETIRED competitors can be permanently deleted"))
                .when(competitorService).deleteCompetitor(id);

        mockMvc.perform(delete("/api/competitors/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                .andExpect(status().isConflict());
    }
}