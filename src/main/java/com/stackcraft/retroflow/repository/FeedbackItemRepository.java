package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FeedbackItemRepository extends JpaRepository<FeedbackItem, UUID> {

    @Override
    @EntityGraph(attributePaths = {"retrospective", "submitter"})
    Optional<FeedbackItem> findById(UUID id);

    @EntityGraph(attributePaths = {"retrospective", "submitter"})
    List<FeedbackItem> findByRetrospectiveIdOrderByCreatedAtAscIdAsc(UUID retrospectiveId);

    @EntityGraph(attributePaths = {"retrospective", "submitter"})
    List<FeedbackItem> findByType(FeedbackType type);
}
