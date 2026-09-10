package com.example.implementacionparcial.teams.repository;

import com.example.implementacionparcial.teams.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Minimal placeholder: see {@link com.example.implementacionparcial.teams.entity.Team}.
 */
public interface ITeamRepository extends JpaRepository<Team, UUID> {
}
