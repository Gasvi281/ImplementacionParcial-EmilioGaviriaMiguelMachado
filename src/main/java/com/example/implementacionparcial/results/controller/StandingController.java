package com.example.implementacionparcial.results.controller;

import com.example.implementacionparcial.results.dto.StandingResponse;
import com.example.implementacionparcial.results.service.StandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solo lectura: los standings se calculan on-the-fly en {@link StandingService}, no hay
 * create/update/delete acá.
 */
@RestController
@RequestMapping("/api/standings")
@RequiredArgsConstructor
@Tag(name = "Standings")
public class StandingController {

    private final StandingService standingService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get the global standings (competitors and teams combined)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Standings listed successfully")})
    public ResponseEntity<Page<StandingResponse>> getGlobal(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(standingService.getGlobal(pageable));
    }

    @GetMapping("/competitors")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get standings for individual competitors only")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Standings listed successfully")})
    public ResponseEntity<Page<StandingResponse>> getCompetitorStandings(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(standingService.getCompetitorStandings(pageable));
    }

    @GetMapping("/teams")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get standings for teams only")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Standings listed successfully")})
    public ResponseEntity<Page<StandingResponse>> getTeamStandings(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(standingService.getTeamStandings(pageable));
    }
}
