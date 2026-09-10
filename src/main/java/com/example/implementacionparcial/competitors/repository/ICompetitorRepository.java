package com.example.implementacionparcial.competitors.repository;

import com.example.implementacionparcial.competitors.entity.Competitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Minimal placeholder: see {@link com.example.implementacionparcial.competitors.entity.Competitor}.
 */
public interface ICompetitorRepository extends JpaRepository<Competitor, UUID> {

    List<Competitor> findAllByTeam_Id(UUID teamId);
}
