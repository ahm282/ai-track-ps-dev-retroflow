package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RetrospectiveRepository extends JpaRepository<Retrospective, UUID> {

    boolean existsByTeamIdAndStatus(UUID teamId, RetrospectiveStatus status);

    @Override
    @EntityGraph(attributePaths = "team")
    Optional<Retrospective> findById(UUID id);

    @EntityGraph(attributePaths = "team")
    List<Retrospective> findByTeamIdOrderByCreatedAtAscIdAsc(UUID teamId);
}
