package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Member;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class FeedbackService {

    private static final int MAX_CONTENT_LENGTH = 4000;
    private static final String FEEDBACK_NOT_FOUND = "Feedback item not found";

    private static final Comparator<FeedbackItem> ACTION_ITEM_ORDER = Comparator
        .comparingInt((FeedbackItem item) -> item.getRetrospective().getStatus() == RetrospectiveStatus.OPEN ? 0 : 1)
        .thenComparingInt(item -> item.getPriority().ordinal())
        .thenComparing(FeedbackItem::getCreatedAt)
        .thenComparing(FeedbackItem::getId);

    private final FeedbackItemRepository feedbackRepository;
    private final RetrospectiveRepository retrospectiveRepository;
    private final TeamService teamService;

    public FeedbackService(FeedbackItemRepository feedbackRepository,
                           RetrospectiveRepository retrospectiveRepository, TeamService teamService) {
        this.feedbackRepository = feedbackRepository;
        this.retrospectiveRepository = retrospectiveRepository;
        this.teamService = teamService;
    }

    public FeedbackItem createFeedback(UUID retrospectiveId, String content, FeedbackType type,
                                       UUID submitterId, Priority priority, Boolean completed) {
        return create(retrospectiveId, content, type, priority, completed, submitterId, null, false);
    }

    public FeedbackItem createFeedbackBySubmitterName(UUID retrospectiveId, String content, String type,
                                                      String submitterName) {
        return create(retrospectiveId, content, parseType(type), null, null, null, submitterName, true);
    }

    public FeedbackItem createActionItemBySubmitterName(UUID retrospectiveId, String content, String priority,
                                                        String submitterName) {
        return create(retrospectiveId, content, FeedbackType.ACTION_ITEM, parsePriority(priority), null,
            null, submitterName, true);
    }

    @Transactional(readOnly = true)
    public List<FeedbackItem> listFeedback(UUID retrospectiveId) {
        findRetrospective(retrospectiveId);
        return feedbackRepository.findByRetrospectiveIdOrderByCreatedAtAscIdAsc(retrospectiveId);
    }

    public FeedbackItem updateFeedback(UUID id, String content, Priority priority, Boolean completed) {
        requireContent(content);
        FeedbackItem item = findItem(id);
        requireOpen(item.getRetrospective());
        if (item.getType() == FeedbackType.ACTION_ITEM) {
            requirePriority(priority);
            if (Boolean.FALSE.equals(completed) && Boolean.TRUE.equals(item.getCompleted())) {
                throw new RetroflowException("Action item cannot be uncompleted");
            }
            item.setPriority(priority);
            if (completed != null) {
                item.setCompleted(completed);
            }
        } else {
            requireNoActionFields(priority, completed);
        }
        item.setContent(content);
        return feedbackRepository.save(item);
    }

    public void deleteFeedback(UUID id) {
        FeedbackItem item = findItem(id);
        requireOpen(item.getRetrospective());
        feedbackRepository.delete(item);
    }

    public FeedbackItem completeActionItem(UUID id) {
        FeedbackItem item = findActionItem(id);
        requireOpen(item.getRetrospective());
        item.setCompleted(true);
        return feedbackRepository.save(item);
    }

    public FeedbackItem uncompleteActionItem(UUID id) {
        FeedbackItem item = findActionItem(id);
        requireOpen(item.getRetrospective());
        if (Boolean.TRUE.equals(item.getCompleted())) {
            throw new RetroflowException("Action item cannot be uncompleted");
        }
        return item;
    }

    @Transactional(readOnly = true)
    public List<FeedbackItem> listActionItems(String priority, String completed) {
        Priority wantedPriority = priority == null ? null : parsePriority(priority);
        Boolean wantedCompleted = completed == null ? null : parseCompleted(completed);
        return feedbackRepository.findByType(FeedbackType.ACTION_ITEM).stream()
            .filter(item -> wantedPriority == null || item.getPriority() == wantedPriority)
            .filter(item -> wantedCompleted == null || wantedCompleted.equals(item.getCompleted()))
            .sorted(ACTION_ITEM_ORDER)
            .toList();
    }

    private FeedbackItem create(UUID retrospectiveId, String content, FeedbackType type, Priority priority,
                                Boolean completed, UUID submitterId, String submitterName, boolean byName) {
        requireContent(content);
        if (type == null) {
            throw new RetroflowException("Type is required");
        }
        Retrospective retrospective = findRetrospective(retrospectiveId);
        Member submitter = byName ? null : teamService.findMember(submitterId);
        requireOpen(retrospective);
        if (type == FeedbackType.ACTION_ITEM) {
            requirePriority(priority);
            if (Boolean.TRUE.equals(completed)) {
                throw new RetroflowException("Action items must start incomplete");
            }
        } else {
            requireNoActionFields(priority, completed);
        }
        UUID teamId = retrospective.getTeam().getId();
        Member author = byName ? teamService.resolveMemberByName(teamId, submitterName)
            : teamService.requireMemberOf(teamId, submitter);
        FeedbackItem item = new FeedbackItem(null, content, type, priority, completed, null, author,
            author.getName(), retrospective);
        return feedbackRepository.save(item);
    }

    private Retrospective findRetrospective(UUID id) {
        return retrospectiveRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found"));
    }

    private FeedbackItem findItem(UUID id) {
        return feedbackRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(FEEDBACK_NOT_FOUND));
    }

    private FeedbackItem findActionItem(UUID id) {
        FeedbackItem item = findItem(id);
        if (item.getType() != FeedbackType.ACTION_ITEM) {
            throw new ResourceNotFoundException(FEEDBACK_NOT_FOUND);
        }
        return item;
    }

    private static void requireOpen(Retrospective retrospective) {
        if (retrospective.getStatus() == RetrospectiveStatus.CLOSED) {
            throw new RetroflowException("Retrospective is closed");
        }
    }

    private static void requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new RetroflowException("Content must not be blank");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new RetroflowException("Content must be at most " + MAX_CONTENT_LENGTH + " characters");
        }
    }

    private static void requirePriority(Priority priority) {
        if (priority == null) {
            throw new RetroflowException("Priority is required for action items");
        }
    }

    private static void requireNoActionFields(Priority priority, Boolean completed) {
        if (priority != null || completed != null) {
            throw new RetroflowException("Priority and completed are only allowed for action items");
        }
    }

    private static FeedbackType parseType(String type) {
        try {
            return FeedbackType.valueOf(type.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new RetroflowException("Type must be one of " + names(FeedbackType.values()));
        }
    }

    private static Priority parsePriority(String priority) {
        try {
            return Priority.valueOf(priority.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new RetroflowException("Priority must be one of " + names(Priority.values()));
        }
    }

    private static Boolean parseCompleted(String completed) {
        String value = completed.strip().toLowerCase(Locale.ROOT);
        if (value.equals("true") || value.equals("false")) {
            return Boolean.valueOf(value);
        }
        throw new RetroflowException("Completed must be true or false");
    }

    private static String names(Enum<?>[] values) {
        return Arrays.stream(values).map(Enum::name).collect(Collectors.joining(", "));
    }
}
