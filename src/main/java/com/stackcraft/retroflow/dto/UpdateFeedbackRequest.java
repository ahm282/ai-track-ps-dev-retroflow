package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFeedbackRequest(
    @NotBlank(message = "Content must not be blank")
    @Size(max = 4000, message = "Content must be at most 4000 characters")
    String content,
    Priority priority,
    Boolean completed
) {
}
