package com.example.implementacionparcial.races.controller;

import com.example.implementacionparcial.common.config.SecurityConfig;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.races.dto.RaceRequest;
import com.example.implementacionparcial.races.dto.RaceResponse;
import com.example.implementacionparcial.races.dto.RaceStatusUpdateRequest;
import com.example.implementacionparcial.races.dto.RaceUpdateRequest;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.races.service.RaceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

@WebMvcTest(RaceController.class)
@Import(SecurityConfig.class)
class RaceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RaceService raceService;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private RaceResponse sampleResponse(UUID id, RaceStatus status) {
        return new RaceResponse(id, "Dune Dash", "desc", LocalDateTime.now().plusDays(10),
                "A", "B", 5000.0, 20, RaceType.INDIVIDUAL, status, UUID.randomUUID(),
                LocalDateTime.now().plusDays(5), LocalDateTime.now(), LocalDateTime.now());
    }

    private RaceRequest sampleRequest() {
        return new RaceRequest("Dune Dash", "desc", LocalDateTime.now().plusDays(10),
                "A", "B", 5000.0, 20, RaceType.INDIVIDUAL, LocalDateTime.now().plusDays(5));
    }

    @Test
    void getAll_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/races"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAll_withAnyAuthenticatedRole_returns200() throws Exception {
        Page<RaceResponse> page = new PageImpl<>(List.of(sampleResponse(UUID.randomUUID(), RaceStatus.DRAFT)));
        when(raceService.getAll(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/races")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void getById_whenMissing_returns404WithErrorBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(raceService.getById(id)).thenThrow(ResourceNotFoundException.of("Race", id));

        mockMvc.perform(get("/api/races/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.path").value("/api/races/" + id));
    }

    @Test
    void create_asOrganizer_returns201WithLocationHeader() throws Exception {
        UUID id = UUID.randomUUID();
        when(raceService.create(any(RaceRequest.class))).thenReturn(sampleResponse(id, RaceStatus.DRAFT));

        mockMvc.perform(post("/api/races")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RACE_ORGANIZER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/races/" + id));
    }

    @Test
    void create_asViewer_returns403() throws Exception {
        mockMvc.perform(post("/api/races")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VIEWER")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_withInvalidBody_returns400WithValidationErrors() throws Exception {
        RaceRequest invalid = new RaceRequest("", null, null, "", "", -1.0, -1, null, null);

        mockMvc.perform(post("/api/races")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void updateStatus_whenTransitionInvalid_returns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(raceService.updateStatus(eq(id), eq(RaceStatus.DRAFT)))
                .thenThrow(new ConflictException("Race status cannot move backwards"));

        mockMvc.perform(patch("/api/races/{id}/status", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")))
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new RaceStatusUpdateRequest(RaceStatus.DRAFT))))
                .andExpect(status().isConflict());
    }

    @Test
    void update_asOrganizer_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        when(raceService.update(eq(id), any(RaceUpdateRequest.class))).thenReturn(sampleResponse(id, RaceStatus.DRAFT));

        mockMvc.perform(patch("/api/races/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RACE_ORGANIZER")))
                        .contentType("application/json")
                        .content("{\"name\":\"New name\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_asAdministrator_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/races/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_asOrganizer_returns403() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/api/races/{id}", id)
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_RACE_ORGANIZER"))))
                .andExpect(status().isForbidden());
    }
}
