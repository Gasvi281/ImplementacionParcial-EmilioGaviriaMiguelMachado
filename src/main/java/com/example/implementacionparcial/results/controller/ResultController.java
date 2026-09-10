package com.example.implementacionparcial.results.controller;

import com.example.implementacionparcial.results.dto.ResultRequest;
import com.example.implementacionparcial.results.dto.ResultResponse;
import com.example.implementacionparcial.results.dto.ResultUpdateRequest;
import com.example.implementacionparcial.results.entity.ResultStatus;
import com.example.implementacionparcial.results.service.ResultService;
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
 * distintas ({@code /api/races/{raceId}/results} y {@code /api/results/{id}}), como indica
 * {@code .claude/rules/results.md}.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Results")
public class ResultController {

    private final ResultService resultService;

    @PostMapping("/api/races/{raceId}/results")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'RACE_ORGANIZER')")
    @Operation(summary = "Record a competitor or team result for a race in progress")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Result created"),
            @ApiResponse(responseCode = "400", description = "Invalid result data"),
            @ApiResponse(responseCode = "404", description = "Race, competitor or team not found"),
            @ApiResponse(responseCode = "409", description = "Result violates a business rule")
    })
    public ResponseEntity<ResultResponse> create(@PathVariable UUID raceId,
                                                  @Valid @RequestBody ResultRequest request) {
        ResultResponse created = resultService.create(raceId, request);
        return ResponseEntity.created(URI.create("/api/results/" + created.id())).body(created);
    }

    @GetMapping("/api/races/{raceId}/results")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List results for a race (filter by status, paginated)")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "Results listed successfully")})
    public ResponseEntity<Page<ResultResponse>> getAllForRace(
            @PathVariable UUID raceId,
            @RequestParam(required = false) ResultStatus status,
            @PageableDefault(size = 20, sort = "recordedAt") Pageable pageable) {
        return ResponseEntity.ok(resultService.getAllForRace(raceId, status, pageable));
    }

    @GetMapping("/api/results/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get a result by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Result found"),
            @ApiResponse(responseCode = "404", description = "Result not found")
    })
    public ResponseEntity<ResultResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(resultService.getById(id));
    }

    @PatchMapping("/api/results/{id}")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'RACE_ORGANIZER')")
    @Operation(summary = "Partially update a result (standings are recalculated on the fly)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Result updated"),
            @ApiResponse(responseCode = "400", description = "Invalid result data"),
            @ApiResponse(responseCode = "404", description = "Result not found"),
            @ApiResponse(responseCode = "409", description = "Update violates a business rule")
    })
    public ResponseEntity<ResultResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody ResultUpdateRequest request) {
        return ResponseEntity.ok(resultService.update(id, request));
    }
}
