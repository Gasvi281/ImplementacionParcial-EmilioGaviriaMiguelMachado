package com.example.implementacionparcial.registrations.controller;

import com.example.implementacionparcial.common.config.SecurityConfig;
import com.example.implementacionparcial.common.exceptions.BadRequestException;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.competitors.dto.CompetitorSummaryResponse;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.races.dto.RaceSummaryResponse;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.registrations.dto.RegistrationRejectionRequest;
import com.example.implementacionparcial.registrations.dto.RegistrationRequest;
import com.example.implementacionparcial.registrations.dto.RegistrationResponse;
import com.example.implementacionparcial.registrations.dto.RegistrationUpdateRequest;
import com.example.implementacionparcial.registrations.entity.RegistrationStatus;
import com.example.implementacionparcial.registrations.service.RegistrationService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RegistrationController.class)
@Import(SecurityConfig.class)
class RegistrationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegistrationService registrationService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private RegistrationResponse sampleResponse(UUID id, RegistrationStatus status) {
        RaceSummaryResponse race = new RaceSummaryResponse(UUID.randomUUID(), "Dune Dash",
                LocalDateTime.now().plusDays(10), RaceType.INDIVIDUAL, RaceStatus.OPEN_FOR_REGISTRATION);
        CompetitorSummaryResponse competitor = new CompetitorSummaryResponse(UUID.randomUUID(), "speedy", CompetitorType.CAMEL);
        return new RegistrationResponse(id, race, competitor, null, status, null, null, null,
                UUID.randomUUID(), LocalDateTime.now());
    }

    @Test
    void getAllForRace_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/races/{raceId}/registrations", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllForRace_withAnyAuthenticatedRole_returns200() throws Exception {
        Page<RegistrationResponse> page = new PageImpl<>(List.of(sampleResponse(UUID.randomUUID(), RegistrationStatus.PENDING)));
        when(registrationService.getAllForRace(any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/races/{raceId}/registrations", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getById_whenMissing_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(registrationService.getById(id)).thenThrow(ResourceNotFoundException.of("RaceRegistration", id));

        mockMvc.perform(get("/api/registrations/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void create_asOrganizer_returns201WithLocationHeader() throws Exception {
        UUID raceId = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        when(registrationService.create(eq(raceId), any(RegistrationRequest.class)))
                .thenReturn(sampleResponse(id, RegistrationStatus.PENDING));

        mockMvc.perform(post("/api/races/{raceId}/registrations", raceId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RACE_ORGANIZER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegistrationRequest(UUID.randomUUID(), null, null))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/registrations/" + id));
    }

    @Test
    void create_asViewer_returns403() throws Exception {
        mockMvc.perform(post("/api/races/{raceId}/registrations", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegistrationRequest(UUID.randomUUID(), null, null))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_whenBusinessRuleViolated_returns400() throws Exception {
        UUID raceId = UUID.randomUUID();
        when(registrationService.create(eq(raceId), any(RegistrationRequest.class)))
                .thenThrow(new BadRequestException("Exactly one of competitorId or teamId must be provided"));

        mockMvc.perform(post("/api/races/{raceId}/registrations", raceId)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegistrationRequest(null, null, null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void approve_asOrganizer_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(registrationService.approve(eq(id), any(RegistrationUpdateRequest.class)))
                .thenReturn(sampleResponse(id, RegistrationStatus.APPROVED));

        mockMvc.perform(patch("/api/registrations/{id}/approve", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RACE_ORGANIZER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegistrationUpdateRequest(1, 1, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void approve_whenAlreadyApproved_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(registrationService.approve(eq(id), any(RegistrationUpdateRequest.class)))
                .thenThrow(new ConflictException("Only PENDING registrations can be approved"));

        mockMvc.perform(patch("/api/registrations/{id}/approve", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegistrationUpdateRequest(null, null, null))))
                .andExpect(status().isConflict());
    }

    @Test
    void reject_withBlankNotes_returns400WithValidationErrors() throws Exception {
        mockMvc.perform(patch("/api/registrations/{id}/reject", UUID.randomUUID())
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegistrationRejectionRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void reject_asOrganizer_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(registrationService.reject(eq(id), any(RegistrationRejectionRequest.class)))
                .thenReturn(sampleResponse(id, RegistrationStatus.REJECTED));

        mockMvc.perform(patch("/api/registrations/{id}/reject", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RACE_ORGANIZER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RegistrationRejectionRequest("no space"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void delete_asAdministrator_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/registrations/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asOrganizer_returns403() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/registrations/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RACE_ORGANIZER"))))
                .andExpect(status().isForbidden());
    }
}
