package com.example.implementacionparcial.teams.mapper;

import com.example.implementacionparcial.teams.dto.TeamMemberResponse;
import com.example.implementacionparcial.teams.entity.TeamMember;

public final class TeamMemberMapper {

    private TeamMemberMapper(){

    }

    public static TeamMemberResponse toResponse(TeamMember teamMember){
        if(teamMember == null) return null;
        return new TeamMemberResponse(
                teamMember.getId(),
                teamMember.getTeam().getId(),
                teamMember.getCompetitor().getId(),
                teamMember.getJoinedAt()
        );
    }
}
