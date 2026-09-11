package com.example.implementacionparcial.competitors.mapper;

import com.example.implementacionparcial.competitors.dto.CompetitorRequest;
import com.example.implementacionparcial.competitors.dto.CompetitorResponse;
import com.example.implementacionparcial.competitors.dto.CompetitorSummaryResponse;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.teams.mapper.TeamMemberMapper;

public final class CompetitorMapper {

    private CompetitorMapper(){

    }

    public static Competitor toEntity(CompetitorRequest request){
        if(request == null) return null;
        return Competitor.builder()
                .name(request.name())
                .nickname(request.nickname())
                .competitorType((request.competitorType()))
                .age(request.age())
                .height(request.height())
                .weight((request.weight()))
                .placeOfOrigin((request.placeOfOrigin()))
                .build();
    }

    public static CompetitorResponse toResponse(Competitor competitor){
        if(competitor == null) return null;
        return new CompetitorResponse(
                competitor.getId(),
                competitor.getName(),
                competitor.getNickname(),
                competitor.getCompetitorType(),
                competitor.getAge(),
                competitor.getHeight(),
                competitor.getWeight(),
                competitor.getPlaceOfOrigin(),
                competitor.getCompetitorStatus(),
                competitor.getRegistrationDate(),
                competitor.getMemberships().stream()
                        .map(TeamMemberMapper::toResponse)
                        .toList()
                );
    }

    public static CompetitorSummaryResponse toSummary(Competitor competitor) {
        return new CompetitorSummaryResponse(competitor.getId(), competitor.getNickname(), competitor.getCompetitorType());
    }
}
