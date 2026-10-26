package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CloseRetrospectiveRequest;
import com.stackcraft.retroflow.dto.CreateMemberRequest;
import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.CreateTeamRequest;
import com.stackcraft.retroflow.dto.MemberResponse;
import com.stackcraft.retroflow.dto.RenameMemberRequest;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.dto.TeamResponse;
import com.stackcraft.retroflow.entity.Member;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

	@GetMapping("/teams")
	public List<TeamResponse> listTeams() {
		return teamService.listTeams().stream().map(TeamMapper::toResponse).toList();
	}

	@GetMapping("/teams/{id}")
	public TeamResponse getTeam(@PathVariable UUID id) {
		return TeamMapper.toResponse(teamService.getTeam(id));
	}

	@PostMapping("/teams/{teamId}/members")
	public ResponseEntity<MemberResponse> addMember(
			@PathVariable UUID teamId, @Valid @RequestBody CreateMemberRequest request) {
		Member member = teamService.addMember(teamId, request.name());
		return ResponseEntity.status(HttpStatus.CREATED).body(TeamMapper.toResponse(member));
	}

	@PutMapping("/teams/{teamId}/members/{memberId}")
	public MemberResponse renameMember(@PathVariable UUID teamId, @PathVariable UUID memberId,
			@Valid @RequestBody RenameMemberRequest request) {
		return TeamMapper.toResponse(teamService.renameMember(teamId, memberId, request.name()));
	}

	@DeleteMapping("/teams/{teamId}/members/{memberId}")
	public ResponseEntity<Void> removeMember(@PathVariable UUID teamId, @PathVariable UUID memberId) {
		teamService.removeMember(teamId, memberId);
		return ResponseEntity.noContent().build();
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

	@GetMapping("/retrospectives/{id}")
	public RetrospectiveResponse getRetrospective(@PathVariable UUID id) {
		return RetrospectiveMapper.toResponse(retrospectiveService.getRetrospective(id));
	}

	@PutMapping("/retrospectives/{id}/close")
	public RetrospectiveResponse closeRetrospective(
			@PathVariable UUID id, @Valid @RequestBody CloseRetrospectiveRequest request) {
		return RetrospectiveMapper.toResponse(retrospectiveService.closeRetrospective(id, request.memberId()));
	}
}
