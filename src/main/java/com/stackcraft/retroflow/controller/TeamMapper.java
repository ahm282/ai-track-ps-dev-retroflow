package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.MemberResponse;
import com.stackcraft.retroflow.dto.TeamResponse;
import com.stackcraft.retroflow.entity.Team;

final class TeamMapper {

    private TeamMapper() {
    }

    static TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getMembers().stream()
            .map(member -> new MemberResponse(member.getId(), member.getName())).toList());
    }
}
