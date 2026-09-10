package com.example.implementacionparcial.teams.mapper;

import com.example.implementacionparcial.teams.dto.TeamRequest;
import com.example.implementacionparcial.teams.dto.TeamResponse;
import com.example.implementacionparcial.teams.entity.Team;

public final class TeamMapper {

    private TeamMapper(){

    }

    public static Team toEntity(TeamRequest request){
        if(request == null) return null;
        return Team.builder()
                .name(request.name())
                .description((request.description()))
                .coach(request.coach())
                .build();
    }

    public static TeamResponse toResponse(Team team){
        if(team == null) return null;
        return new TeamResponse(
                team.getId(),
                team.getName(),
                team.getDescription(),
                team.getCoach(),
                team.getMaxMembers(),
                team.getCreationDate(),
                team.getStatus(),
                team.getMembers().stream()
                        .map(TeamMemberMapper::toResponse)
                        .toList()
        );
    }
}
