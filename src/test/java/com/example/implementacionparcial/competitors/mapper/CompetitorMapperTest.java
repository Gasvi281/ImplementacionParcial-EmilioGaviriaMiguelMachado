package com.example.implementacionparcial.competitors.mapper;

import com.example.implementacionparcial.competitors.dto.CompetitorRequest;
import com.example.implementacionparcial.competitors.dto.CompetitorResponse;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompetitorMapperTest {

    @Test
    void toEntity_mapsAllRequestFields() {
        CompetitorRequest request = new CompetitorRequest(
                "Javier", "javi123", CompetitorType.DWARF, 30, 1.2f, 60f, "Medellín");

        Competitor competitor = CompetitorMapper.toEntity(request);

        assertThat(competitor.getName()).isEqualTo("Javier");
        assertThat(competitor.getNickname()).isEqualTo("javi123");
        assertThat(competitor.getCompetitorType()).isEqualTo(CompetitorType.DWARF);
        assertThat(competitor.getAge()).isEqualTo(30);
        assertThat(competitor.getHeight()).isEqualTo(1.2f);
        assertThat(competitor.getWeight()).isEqualTo(60f);
        assertThat(competitor.getPlaceOfOrigin()).isEqualTo("Medellín");
        assertThat(competitor.getCompetitorStatus()).isEqualTo(CompetitorStatus.ACTIVE); // default del builder
    }

    @Test
    void toResponse_mapsAllEntityFields_withEmptyMemberships() {
        UUID id = UUID.randomUUID();
        Competitor competitor = Competitor.builder()
                .id(id)
                .name("Javier")
                .nickname("javi123")
                .competitorType(CompetitorType.DWARF)
                .age(30)
                .height(1.2f)
                .weight(60f)
                .placeOfOrigin("Medellín")
                .competitorStatus(CompetitorStatus.RETIRED)
                .memberships(Collections.emptyList())
                .build();

        CompetitorResponse response = CompetitorMapper.toResponse(competitor);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.competitorStatus()).isEqualTo(CompetitorStatus.RETIRED);
        assertThat(response.memberships()).isEmpty();
    }
}