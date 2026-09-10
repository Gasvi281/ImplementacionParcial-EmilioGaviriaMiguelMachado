package com.example.implementacionparcial.teams.controller;

import com.example.implementacionparcial.teams.dto.TeamMemberResponse;
import com.example.implementacionparcial.teams.dto.TeamRequest;
import com.example.implementacionparcial.teams.dto.TeamResponse;
import com.example.implementacionparcial.teams.service.TeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
@Tag(name = "Teams")
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List all teams")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Teams listed successfully")})
    public ResponseEntity<List<TeamResponse>> getAll() {
        return ResponseEntity.ok(teamService.getTeams());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a team by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team found"),
            @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<TeamResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "Create a new team")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Team created"),
            @ApiResponse(responseCode = "400", description = "Invalid team data"),
            @ApiResponse(responseCode = "409", description = "Team name already exists")
    })
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody TeamRequest request) {
        TeamResponse created = teamService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "Replace a team completely")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Team updated"),
            @ApiResponse(responseCode = "400", description = "Invalid team data"),
            @ApiResponse(responseCode = "404", description = "Team not found"),
            @ApiResponse(responseCode = "409", description = "Team name already exists")
    })
    public ResponseEntity<TeamResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody TeamRequest request) {
        return ResponseEntity.ok(teamService.update(request, id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "Deactivate a team (logical delete: status set to Inactive)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Team deactivated"),
            @ApiResponse(responseCode = "404", description = "Team not found")
    })
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        teamService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{teamId}/members/{competitorId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "Add a competitor to a team")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Competitor added to team"),
            @ApiResponse(responseCode = "404", description = "Team or competitor not found"),
            @ApiResponse(responseCode = "409", description = "Team full, duplicate membership, or competitor already in an active team")
    })
    public ResponseEntity<TeamMemberResponse> addMember(
            @PathVariable UUID teamId,
            @PathVariable UUID competitorId) {
        TeamMemberResponse response = teamService.addTeamMember(teamId, competitorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{teamId}/members/{competitorId}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR')")
    @Operation(summary = "Remove a competitor from a team")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Competitor removed from team"),
            @ApiResponse(responseCode = "404", description = "Membership not found")
    })
    public ResponseEntity<Void> removeMember(
            @PathVariable UUID teamId,
            @PathVariable UUID competitorId) {
        teamService.removeTeamMember(teamId, competitorId);
        return ResponseEntity.noContent().build();
    }
}