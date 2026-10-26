package com.stackcraft.retroflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FeedbackResponse(UUID id, UUID retrospectiveId, String content, FeedbackType type,
                               UUID submitterId, String submitterName, Priority priority, Boolean completed) {
}
