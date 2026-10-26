package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

import java.time.LocalDate;
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
		TeamResponse team = TeamResponse.from(teamService.createTeam(request.name(), request.members()));
		return ResponseEntity.created(ServletUriComponentsBuilder.fromCurrentRequest()
			.path("/{id}").buildAndExpand(team.id()).toUri()).body(team);
	}

	@GetMapping("/teams/{id}")
	public TeamResponse getTeam(@PathVariable UUID id) {
		return TeamResponse.from(teamService.getTeam(id));
	}

	@PostMapping("/teams/{teamId}/retrospectives")
	public ResponseEntity<RetrospectiveResponse> createRetrospective(
			@PathVariable UUID teamId, @Valid @RequestBody CreateRetrospectiveRequest request) {
		Retrospective retrospective = retrospectiveService.createRetrospective(
			teamId, request.title(), request.memberId(), request.status());
		return ResponseEntity.status(HttpStatus.CREATED).body(RetrospectiveResponse.from(retrospective));
	}

	@GetMapping("/teams/{teamId}/retrospectives")
	public List<RetrospectiveResponse> listRetrospectives(@PathVariable UUID teamId) {
		return retrospectiveService.listRetrospectives(teamId).stream()
			.map(RetrospectiveResponse::from).toList();
	}

	@PutMapping("/retrospectives/{id}/close")
	public RetrospectiveResponse closeRetrospective(
			@PathVariable UUID id, @Valid @RequestBody CloseRetrospectiveRequest request) {
		return RetrospectiveResponse.from(retrospectiveService.closeRetrospective(id, request.memberId()));
	}

	public record CreateTeamRequest(
		@NotBlank(message = "Team name must not be blank")
		@Size(max = 255, message = "Team name must be at most 255 characters")
		@Pattern(regexp = "^[A-Za-z0-9 ]+$", message = "Team name must contain only letters, digits and spaces")
		String name,
		@NotEmpty(message = "A team must have at least one member")
		List<@NotBlank(message = "Member name must not be blank")
			@Size(max = 255, message = "Member name must be at most 255 characters")
			@Pattern(regexp = "^[A-Za-z0-9 ]+$", message = "Member name must contain only letters, digits and spaces")
			String> members
	) {
	}

	public record CreateRetrospectiveRequest(
		@NotBlank(message = "Retrospective title must not be blank")
		@Size(max = 255, message = "Retrospective title must be at most 255 characters")
		String title,
		@NotNull(message = "Member ID is required") UUID memberId,
		RetrospectiveStatus status
	) {
	}

	public record CloseRetrospectiveRequest(@NotNull(message = "Member ID is required") UUID memberId) {
	}

	public record MemberResponse(UUID id, String name, boolean active) {
	}

	public record TeamResponse(UUID id, String name, List<MemberResponse> members) {
		static TeamResponse from(Team team) {
			return new TeamResponse(team.getId(), team.getName(), team.getMembers().stream()
				.map(member -> new MemberResponse(member.getId(), member.getName(), member.isActive())).toList());
		}
	}

	public record RetrospectiveResponse(UUID id, String title, LocalDate date, RetrospectiveStatus status,
										UUID teamId) {
		static RetrospectiveResponse from(Retrospective retrospective) {
			return new RetrospectiveResponse(retrospective.getId(), retrospective.getTitle(),
				retrospective.getDate(), retrospective.getStatus(), retrospective.getTeam().getId());
		}
	}
}
