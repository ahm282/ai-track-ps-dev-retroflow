package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateFeedbackRequest(
    @NotBlank(message = "Content must not be blank")
    @Size(max = 4000, message = "Content must be at most 4000 characters")
    String content,
    @NotNull(message = "Type is required") FeedbackType type,
    @NotNull(message = "Submitter ID is required") UUID submitterId,
    Priority priority,
    Boolean completed
) {
}
