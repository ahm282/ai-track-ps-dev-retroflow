package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CloseRetrospectiveRequest;
import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.CreateTeamRequest;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.dto.TeamResponse;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RetroflowController {

	private final TeamService teamService;
	private final RetrospectiveService retrospectiveService;

	public RetroflowController(TeamService teamService, RetrospectiveService retrospectiveService) {
		this.teamService = teamService;
		this.retrospectiveService = retrospectiveService;
	}

	@PostMapping("/teams")
	public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
		TeamResponse team = TeamMapper.toResponse(teamService.createTeam(request.name(), request.members()));
		return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}").buildAndExpand(team.id()).toUri()).body(team);
	}

	@GetMapping("/teams/{id}")
	public TeamResponse getTeam(@PathVariable UUID id) {
		return TeamMapper.toResponse(teamService.getTeam(id));
	}

	@PostMapping("/teams/{teamId}/retrospectives")
	public ResponseEntity<RetrospectiveResponse> createRetrospective(
			@PathVariable UUID teamId, @Valid @RequestBody CreateRetrospectiveRequest request) {
		Retrospective retrospective = retrospectiveService.createRetrospective(
			teamId, request.title(), request.memberId(), request.status());
		return ResponseEntity.status(HttpStatus.CREATED).body(RetrospectiveMapper.toResponse(retrospective));
	}

	@GetMapping("/teams/{teamId}/retrospectives")
	public List<RetrospectiveResponse> listRetrospectives(@PathVariable UUID teamId) {
		return retrospectiveService.listRetrospectives(teamId).stream()
			.map(RetrospectiveMapper::toResponse).toList();
	}

	@PutMapping("/retrospectives/{id}/close")
	public RetrospectiveResponse closeRetrospective(
			@PathVariable UUID id, @Valid @RequestBody CloseRetrospectiveRequest request) {
		return RetrospectiveMapper.toResponse(retrospectiveService.closeRetrospective(id, request.memberId()));
	}
}
