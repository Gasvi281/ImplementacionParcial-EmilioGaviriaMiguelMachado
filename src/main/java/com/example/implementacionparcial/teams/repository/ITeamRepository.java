package com.example.implementacionparcial.teams.repository;

import com.example.implementacionparcial.teams.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ITeamRepository extends JpaRepository<Team, UUID> {
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, UUID id);
}
