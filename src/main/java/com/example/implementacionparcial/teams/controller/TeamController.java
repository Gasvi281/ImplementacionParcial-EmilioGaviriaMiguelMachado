package com.example.implementacionparcial.teams.controller;

import com.example.implementacionparcial.teams.dto.TeamMemberResponse;
import com.example.implementacionparcial.teams.dto.TeamRequest;
import com.example.implementacionparcial.teams.dto.TeamResponse;
import com.example.implementacionparcial.teams.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;

    @GetMapping
    public ResponseEntity<List<TeamResponse>> getTeams(){
        return ResponseEntity.ok(teamService.getTeams());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TeamResponse> getTeamById(@PathVariable UUID id){
        return ResponseEntity.ok(teamService.getTeamById(id));
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody TeamRequest request){
        TeamResponse createdTeam = teamService.createTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTeam);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeamResponse> updateTeam(
            @Valid @RequestBody TeamRequest request,
            @PathVariable UUID id){
        return ResponseEntity.ok(teamService.update(request, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<TeamResponse> deactivateTeam(@PathVariable UUID id){
        return ResponseEntity.ok(teamService.deactivate(id));
    }

    @PostMapping("/{teamId}/members/{competitorId}")
    public ResponseEntity<TeamMemberResponse> addMember(@PathVariable UUID teamId, @PathVariable UUID competitorId){
        TeamMemberResponse addedMember = teamService.addTeamMember(teamId, competitorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(addedMember);
    }

    @DeleteMapping("/{teamId}/members/{competitorId}")
    public ResponseEntity<Void> deleteMember(@PathVariable UUID teamId, @PathVariable UUID competitorId){
        teamService.removeMember(teamId, competitorId);
        return ResponseEntity.noContent().build();
    }
}
