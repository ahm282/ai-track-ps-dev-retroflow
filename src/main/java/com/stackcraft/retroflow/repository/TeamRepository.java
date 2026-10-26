package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Team;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    boolean existsByNameKey(String nameKey);

    @Override
    @EntityGraph(attributePaths = "members")
    Optional<Team> findById(UUID id);

    @EntityGraph(attributePaths = "members")
    List<Team> findAllByOrderByCreatedAtAscIdAsc();
}
