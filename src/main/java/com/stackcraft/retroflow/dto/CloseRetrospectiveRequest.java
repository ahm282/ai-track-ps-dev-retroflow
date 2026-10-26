package com.stackcraft.retroflow.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CloseRetrospectiveRequest(@NotNull(message = "Member ID is required") UUID memberId) {
}
