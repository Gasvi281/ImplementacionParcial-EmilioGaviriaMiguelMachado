package com.example.implementacionparcial.competitors.controller;

import com.example.implementacionparcial.competitors.dto.CompetitorRequest;
import com.example.implementacionparcial.competitors.dto.CompetitorResponse;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.service.CompetitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/competitors")
@RequiredArgsConstructor
@Tag(name = "Competitors")
public class CompetitorController {

    private final CompetitorService competitorService;

    @GetMapping
    @Operation(summary = "List all competitors")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Competitors listed successfully")})
    public ResponseEntity<List<CompetitorResponse>> getAll() {
        return ResponseEntity.ok(competitorService.getCompetitors());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a competitor by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Competitor found"),
            @ApiResponse(responseCode = "404", description = "Competitor not found")
    })
    public ResponseEntity<CompetitorResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(competitorService.getById(id));
    }

    @PostMapping
    @Operation(summary = "Create a new competitor")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Competitor created"),
            @ApiResponse(responseCode = "400", description = "Invalid competitor data"),
            @ApiResponse(responseCode = "409", description = "Nickname already exists")
    })
    public ResponseEntity<CompetitorResponse> create(@Valid @RequestBody CompetitorRequest request) {
        CompetitorResponse created = competitorService.createCompetitor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a competitor completely")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Competitor updated"),
            @ApiResponse(responseCode = "400", description = "Invalid competitor data"),
            @ApiResponse(responseCode = "404", description = "Competitor not found"),
            @ApiResponse(responseCode = "409", description = "Nickname already exists")
    })
    public ResponseEntity<CompetitorResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CompetitorRequest request) {
        return ResponseEntity.ok(competitorService.updateCompetitor(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change a competitor's status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "404", description = "Competitor not found")
    })
    public ResponseEntity<CompetitorResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CompetitorStatus request) {
        return ResponseEntity.ok(competitorService.changeStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Permanently delete a competitor (only if RETIRED)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Competitor deleted"),
            @ApiResponse(responseCode = "404", description = "Competitor not found"),
            @ApiResponse(responseCode = "409", description = "Competitor is not RETIRED")
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        competitorService.deleteCompetitor(id);
        return ResponseEntity.noContent().build();
    }
}