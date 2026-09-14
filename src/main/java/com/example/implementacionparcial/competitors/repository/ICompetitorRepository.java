package com.example.implementacionparcial.competitors.repository;

import com.example.implementacionparcial.competitors.entity.Competitor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ICompetitorRepository extends JpaRepository<Competitor, UUID> {
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndIdNot(String nickname, UUID id);
}
