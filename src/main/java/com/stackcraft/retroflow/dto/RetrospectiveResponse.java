package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.RetrospectiveStatus;

import java.time.LocalDate;
import java.util.UUID;

public record RetrospectiveResponse(UUID id, String title, LocalDate date, RetrospectiveStatus status, UUID teamId) {
}
