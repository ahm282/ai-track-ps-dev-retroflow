package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Member;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RetrospectiveService {

    private final RetrospectiveRepository retrospectiveRepository;
    private final TeamService teamService;
    private final FeedbackService feedbackService;
    private final Validator validator;

    public RetrospectiveService(RetrospectiveRepository retrospectiveRepository, TeamService teamService,
                                FeedbackService feedbackService, Validator validator) {
        this.retrospectiveRepository = retrospectiveRepository;
        this.teamService = teamService;
        this.feedbackService = feedbackService;
        this.validator = validator;
    }

    public Retrospective createRetrospective(UUID teamId, String title, UUID memberId,
                                              RetrospectiveStatus status) {
        return create(teamId, title, status, true, memberId);
    }

    public Retrospective createRetrospective(UUID teamId, String title) {
        return create(teamId, title, null, false, null);
    }

    @Transactional(readOnly = true)
    public Retrospective getRetrospective(UUID id) {
        return retrospectiveRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found"));
    }

    @Transactional(readOnly = true)
    public List<Retrospective> listRetrospectives(UUID teamId) {
        teamService.getTeam(teamId);
        return retrospectiveRepository.findByTeamIdOrderByCreatedAtAscIdAsc(teamId);
    }

    public Retrospective closeRetrospective(UUID id, UUID memberId) {
        return close(id, true, memberId);
    }

    public Retrospective closeRetrospective(UUID id) {
        return close(id, false, null);
    }

    public FeedbackItem addFeedbackItem(UUID retrospectiveId, String content, String type, String submitterName) {
        return feedbackService.createFeedbackBySubmitterName(retrospectiveId, content, type, submitterName);
    }

    public ActionItem addActionItem(UUID retrospectiveId, String content, String priority, String submitterName) {
        return ActionItem.from(feedbackService.createActionItemBySubmitterName(
            retrospectiveId, content, priority, submitterName));
    }

    public ActionItem completeActionItem(UUID id) {
        return ActionItem.from(feedbackService.completeActionItem(id));
    }

    public ActionItem uncompleteActionItem(UUID id) {
        return ActionItem.from(feedbackService.uncompleteActionItem(id));
    }

    private Retrospective create(UUID teamId, String title, RetrospectiveStatus status,
                                 boolean checkMember, UUID memberId) {
        RetrospectiveStatus effectiveStatus = status == null ? RetrospectiveStatus.OPEN : status;
        Team team = teamService.getTeam(teamId);
        Retrospective retrospective = new Retrospective(null, title, LocalDate.now(ZoneOffset.UTC),
            effectiveStatus, team, null);
        EntityValidation.require(validator, retrospective);
        Member requester = checkMember ? teamService.findMember(memberId) : null;
        if (checkMember) {
            teamService.requireMemberOf(teamId, requester);
        }
        if (effectiveStatus == RetrospectiveStatus.OPEN
                && retrospectiveRepository.existsByTeamIdAndStatus(teamId, RetrospectiveStatus.OPEN)) {
            throw new RetroflowException("Only 1 retro can be open");
        }
        return retrospectiveRepository.save(retrospective);
    }

    private Retrospective close(UUID id, boolean checkMember, UUID memberId) {
        Retrospective retrospective = getRetrospective(id);
        Member requester = checkMember ? teamService.findMember(memberId) : null;
        if (retrospective.getStatus() == RetrospectiveStatus.CLOSED) {
            throw new RetroflowException("Retrospective already closed");
        }
        if (checkMember) {
            teamService.requireMemberOf(retrospective.getTeam().getId(), requester);
        }
        retrospective.setStatus(RetrospectiveStatus.CLOSED);
        return retrospectiveRepository.save(retrospective);
    }
}