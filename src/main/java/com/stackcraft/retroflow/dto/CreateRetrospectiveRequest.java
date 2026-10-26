package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateRetrospectiveRequest(
    @NotBlank(message = "Retrospective title must not be blank")
    @Size(max = 255, message = "Retrospective title must be at most 255 characters")
    String title,
    @NotNull(message = "Member ID is required") UUID memberId,
    RetrospectiveStatus status
) {
}
