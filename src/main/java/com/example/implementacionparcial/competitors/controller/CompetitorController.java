package com.example.implementacionparcial.competitors.controller;

import com.example.implementacionparcial.competitors.dto.CompetitorRequest;
import com.example.implementacionparcial.competitors.dto.CompetitorResponse;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.service.CompetitorService;
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
public class CompetitorController {

    private final CompetitorService competitorService;

    @GetMapping
    public ResponseEntity<List<CompetitorResponse>> getAll() {
        return ResponseEntity.ok(competitorService.getCompetitors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetitorResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(competitorService.getById(id));
    }

    @PostMapping
    public ResponseEntity<CompetitorResponse> create(@Valid @RequestBody CompetitorRequest request) {
        CompetitorResponse created = competitorService.createCompetitor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetitorResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CompetitorRequest request) {
        return ResponseEntity.ok(competitorService.updateCompetitor(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<CompetitorResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody CompetitorStatus request) {
        return ResponseEntity.ok(competitorService.changeStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        competitorService.deleteCompetitor(id);
        return ResponseEntity.noContent().build();
    }
}
