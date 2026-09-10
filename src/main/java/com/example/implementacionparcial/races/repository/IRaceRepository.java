package com.example.implementacionparcial.races.repository;

import com.example.implementacionparcial.races.entity.Race;
import com.example.implementacionparcial.races.entity.RaceStatus;
import com.example.implementacionparcial.races.entity.RaceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface IRaceRepository extends JpaRepository<Race, UUID> {

    @Query("""
            SELECT r FROM Race r
            WHERE (:status IS NULL OR r.status = :status)
              AND (:type IS NULL OR r.type = :type)
              AND (:name IS NULL OR LOWER(r.name) LIKE LOWER(CONCAT('%', :name, '%')))
            """)
    Page<Race> search(@Param("status") RaceStatus status,
                       @Param("type") RaceType type,
                       @Param("name") String name,
                       Pageable pageable);
}
