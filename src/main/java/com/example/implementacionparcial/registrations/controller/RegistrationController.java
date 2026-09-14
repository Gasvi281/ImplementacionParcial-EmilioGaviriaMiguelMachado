package com.example.implementacionparcial.registrations.controller;

import com.example.implementacionparcial.registrations.dto.RegistrationRejectionRequest;
import com.example.implementacionparcial.registrations.dto.RegistrationRequest;
import com.example.implementacionparcial.registrations.dto.RegistrationResponse;
import com.example.implementacionparcial.registrations.dto.RegistrationUpdateRequest;
import com.example.implementacionparcial.registrations.entity.RegistrationStatus;
import com.example.implementacionparcial.registrations.service.RegistrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Sin {@code @RequestMapping} a nivel de clase: las rutas de este dominio viven bajo dos bases
 * distintas ({@code /api/races/{raceId}/registrations} y {@code /api/registrations/{id}}), como
 * indica {@code .claude/rules/registrations.md}.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Registrations")
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping("/api/races/{raceId}/registrations")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'RACE_ORGANIZER')")
    @Operation(summary = "Register a competitor or a team for a race")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Registration created"),
            @ApiResponse(responseCode = "400", description = "Invalid registration data"),
            @ApiResponse(responseCode = "404", description = "Race, competitor or team not found"),
            @ApiResponse(responseCode = "409", description = "Registration violates a business rule")
    })
    public ResponseEntity<RegistrationResponse> create(@PathVariable UUID raceId,
                                                         @Valid @RequestBody RegistrationRequest request) {
        RegistrationResponse created = registrationService.create(raceId, request);
        return ResponseEntity.created(URI.create("/api/registrations/" + created.id())).body(created);
    }

    @GetMapping("/api/races/{raceId}/registrations")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List registrations for a race (filter by status, paginated)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Registrations listed successfully")})
    public ResponseEntity<Page<RegistrationResponse>> getAllForRace(
            @PathVariable UUID raceId,
            @RequestParam(required = false) RegistrationStatus status,
            @PageableDefault(size = 20, sort = "registeredAt") Pageable pageable) {
        return ResponseEntity.ok(registrationService.getAllForRace(raceId, status, pageable));
    }

    @GetMapping("/api/registrations/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a registration by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration found"),
            @ApiResponse(responseCode = "404", description = "Registration not found")
    })
    public ResponseEntity<RegistrationResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(registrationService.getById(id));
    }

    @PatchMapping("/api/registrations/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'RACE_ORGANIZER')")
    @Operation(summary = "Approve a pending registration, optionally assigning lane/startPosition")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration approved"),
            @ApiResponse(responseCode = "404", description = "Registration not found"),
            @ApiResponse(responseCode = "409", description = "Registration cannot be approved")
    })
    public ResponseEntity<RegistrationResponse> approve(@PathVariable UUID id,
                                                          @Valid @RequestBody RegistrationUpdateRequest request) {
        return ResponseEntity.ok(registrationService.approve(id, request));
    }

    @PatchMapping("/api/registrations/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'RACE_ORGANIZER')")
    @Operation(summary = "Reject a pending registration")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration rejected"),
            @ApiResponse(responseCode = "400", description = "Missing rejection reason"),
            @ApiResponse(responseCode = "404", description = "Registration not found"),
            @ApiResponse(responseCode = "409", description = "Registration cannot be rejected")
    })
    public ResponseEntity<RegistrationResponse> reject(@PathVariable UUID id,
                                                         @Valid @RequestBody RegistrationRejectionRequest request) {
        return ResponseEntity.ok(registrationService.reject(id, request));
    }

    @DeleteMapping("/api/registrations/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Cancel a registration (logical delete: transitions status to CANCELLED)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Registration cancelled"),
            @ApiResponse(responseCode = "404", description = "Registration not found"),
            @ApiResponse(responseCode = "409", description = "Registration already cancelled")
    })
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        registrationService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
