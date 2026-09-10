package com.example.implementacionparcial.competitors.service;

import com.example.implementacionparcial.auditlog.service.AuditLogService;
import com.example.implementacionparcial.common.exceptions.ConflictException;
import com.example.implementacionparcial.common.exceptions.ResourceNotFoundException;
import com.example.implementacionparcial.common.security.CurrentUser;
import com.example.implementacionparcial.competitors.dto.CompetitorRequest;
import com.example.implementacionparcial.competitors.dto.CompetitorResponse;
import com.example.implementacionparcial.competitors.entity.Competitor;
import com.example.implementacionparcial.competitors.entity.CompetitorStatus;
import com.example.implementacionparcial.competitors.entity.CompetitorType;
import com.example.implementacionparcial.competitors.repository.ICompetitorRepository;
import com.example.implementacionparcial.teams.repository.ITeamMemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetitorServiceTest {

    @Mock private ICompetitorRepository competitorRepository;
    @Mock private ITeamMemberRepository teamMemberRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private CurrentUser currentUser;

    @InjectMocks
    private CompetitorService competitorService;

    private final UUID userId = UUID.randomUUID();

    private Competitor sampleCompetitor(UUID id, CompetitorStatus status) {
        return Competitor.builder()
                .id(id)
                .name("Javier")
                .nickname("javi123")
                .competitorType(CompetitorType.DWARF)
                .age(30)
                .height(1.2f)
                .weight(60f)
                .placeOfOrigin("Medellín")
                .competitorStatus(status)
                .build();
    }

    private CompetitorRequest sampleRequest() {
        return new CompetitorRequest("Javier", "javi123", CompetitorType.DWARF, 30, 1.2f, 60f, "Medellín");
    }

    @Test
    void getById_throwsResourceNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(competitorRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> competitorService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createCompetitor_throwsConflict_whenNicknameDuplicated() {
        CompetitorRequest request = sampleRequest();
        when(competitorRepository.existsByNickname("javi123")).thenReturn(true);

        assertThatThrownBy(() -> competitorService.createCompetitor(request))
                .isInstanceOf(ConflictException.class);
        verify(competitorRepository, never()).save(any());
        verify(auditLogService, never()).record(any(), any(), any(), any(), any());
    }

    @Test
    void createCompetitor_savesAndRecordsAudit_whenValid() {
        CompetitorRequest request = sampleRequest();
        when(competitorRepository.existsByNickname("javi123")).thenReturn(false);
        when(competitorRepository.save(any(Competitor.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(currentUser.id()).thenReturn(userId);

        CompetitorResponse response = competitorService.createCompetitor(request);

        assertThat(response.nickname()).isEqualTo("javi123");
        verify(auditLogService).record(eq(userId), eq("CREATE"), eq("Competitor"), any(UUID.class), eq(null));
    }

    @Test
    void deleteCompetitor_throwsConflict_whenNotRetired() {
        UUID id = UUID.randomUUID();
        when(competitorRepository.findById(id))
                .thenReturn(Optional.of(sampleCompetitor(id, CompetitorStatus.ACTIVE)));

        assertThatThrownBy(() -> competitorService.deleteCompetitor(id))
                .isInstanceOf(ConflictException.class);
        verify(competitorRepository, never()).delete(any());
    }

    @Test
    void deleteCompetitor_deletesAndRecordsAudit_whenRetired() {
        UUID id = UUID.randomUUID();
        Competitor retired = sampleCompetitor(id, CompetitorStatus.RETIRED);
        when(competitorRepository.findById(id)).thenReturn(Optional.of(retired));
        when(currentUser.id()).thenReturn(userId);

        competitorService.deleteCompetitor(id);

        verify(competitorRepository).delete(retired);
        verify(auditLogService).record(eq(userId), eq("DELETE"), eq("Competitor"), eq(id), eq(null));
    }

    @Test
    void getEligibleOrThrow_throwsConflict_whenNotActive() {
        UUID id = UUID.randomUUID();
        when(competitorRepository.findById(id))
                .thenReturn(Optional.of(sampleCompetitor(id, CompetitorStatus.INJURED)));

        assertThatThrownBy(() -> competitorService.getEligibleOrThrow(id))
                .isInstanceOf(ConflictException.class);
    }
}
