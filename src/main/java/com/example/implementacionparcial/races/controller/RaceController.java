package com.example.implementacionparcial.races.controller;

import com.example.implementacionparcial.races.dto.RaceRequest;
import com.example.implementacionparcial.races.dto.RaceResponse;
import com.example.implementacionparcial.races.dto.RaceStatusUpdateRequest;
import com.example.implementacionparcial.races.dto.RaceUpdateRequest;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import com.example.implementacionparcial.races.service.RaceService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/races")
@RequiredArgsConstructor
@Tag(name = "Races")
public class RaceController {

    private final RaceService raceService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List races (filters, pagination and sorting)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Races listed successfully")})
    public ResponseEntity<Page<RaceResponse>> getAll(
            @RequestParam(required = false) RaceStatus status,
            @RequestParam(required = false) RaceType type,
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "scheduledAt") Pageable pageable) {
        return ResponseEntity.ok(raceService.getAll(status, type, name, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a race by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Race found"),
            @ApiResponse(responseCode = "404", description = "Race not found")
    })
    public ResponseEntity<RaceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(raceService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'RACE_ORGANIZER')")
    @Operation(summary = "Create a new race")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Race created"),
            @ApiResponse(responseCode = "400", description = "Invalid race data")
    })
    public ResponseEntity<RaceResponse> create(@Valid @RequestBody RaceRequest request) {
        RaceResponse created = raceService.create(request);
        return ResponseEntity.created(URI.create("/api/races/" + created.id())).body(created);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'RACE_ORGANIZER')")
    @Operation(summary = "Partially update a race")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Race updated"),
            @ApiResponse(responseCode = "404", description = "Race not found"),
            @ApiResponse(responseCode = "409", description = "Race cannot be edited in its current status")
    })
    public ResponseEntity<RaceResponse> update(@PathVariable UUID id, @Valid @RequestBody RaceUpdateRequest request) {
        return ResponseEntity.ok(raceService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'RACE_ORGANIZER')")
    @Operation(summary = "Transition a race to a new status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "404", description = "Race not found"),
            @ApiResponse(responseCode = "409", description = "Invalid status transition")
    })
    public ResponseEntity<RaceResponse> updateStatus(@PathVariable UUID id,
                                                       @Valid @RequestBody RaceStatusUpdateRequest request) {
        return ResponseEntity.ok(raceService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    @Operation(summary = "Cancel a race (logical delete: transitions status to CANCELLED)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Race cancelled"),
            @ApiResponse(responseCode = "404", description = "Race not found"),
            @ApiResponse(responseCode = "409", description = "Race cannot be cancelled in its current status")
    })
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        raceService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
