package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.entity.Retrospective;

final class RetrospectiveMapper {

    private RetrospectiveMapper() {
    }

    static RetrospectiveResponse toResponse(Retrospective retrospective) {
        return new RetrospectiveResponse(retrospective.getId(), retrospective.getTitle(),
            retrospective.getDate(), retrospective.getStatus(), retrospective.getTeam().getId());
    }
}
