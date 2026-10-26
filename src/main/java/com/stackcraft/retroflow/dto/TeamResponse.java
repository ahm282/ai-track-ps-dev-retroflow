package com.stackcraft.retroflow.dto;

import java.util.List;
import java.util.UUID;

public record TeamResponse(UUID id, String name, List<MemberResponse> members) {
}
