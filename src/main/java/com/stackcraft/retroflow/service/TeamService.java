package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class TeamService {

    public Team createTeam(String name, List<String> memberNames) {
        throw new UnsupportedOperationException("Team creation is not implemented yet");
    }

    public Team getTeam(UUID id) {
        throw new UnsupportedOperationException("Team lookup is not implemented yet");
    }
}