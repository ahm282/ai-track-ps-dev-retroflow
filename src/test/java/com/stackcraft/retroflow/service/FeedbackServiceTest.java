package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
class FeedbackServiceTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private RetrospectiveService retrospectiveService;

    @Autowired
    private FeedbackService feedbackService;

    private Team team;
    private Team otherTeam;
    private Retrospective retro;

    @BeforeEach
    void setUp() {
        team = newTeam("Alice", "Bob");
        otherTeam = newTeam("Carol");
        retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1", alice(), null);
    }

    @ParameterizedTest
    @EnumSource(value = FeedbackType.class, names = {"WENT_WELL", "NEEDS_IMPROVEMENT"})
    void creatingFeedback_storesItWithoutActionFields(FeedbackType type) {
        // Act
        FeedbackItem item = feedbackService.createFeedback(retro.getId(), "Good pairing", type, alice(), null, null);

        // Assert
        FeedbackItem stored = feedbackService.listFeedback(retro.getId()).get(0);
        assertThat(stored.getId()).isEqualTo(item.getId());
        assertThat(stored.getContent()).isEqualTo("Good pairing");
        assertThat(stored.getType()).isEqualTo(type);
        assertThat(stored.getSubmitter().getId()).isEqualTo(alice());
        assertThat(stored.getSubmitterName()).isEqualTo("Alice");
        assertThat(stored.getPriority()).isNull();
        assertThat(stored.getCompleted()).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {"null", "false"}, nullValues = "null")
    void creatingAnActionItem_startsIncompleteWithItsPriority(Boolean completed) {
        // Act
        FeedbackItem item = feedbackService.createFeedback(
            retro.getId(), "Fix the build", FeedbackType.ACTION_ITEM, alice(), Priority.HIGH, completed);

        // Assert
        assertThat(item.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(item.getCompleted()).isFalse();
    }

    @Test
    void creatingAnActionItem_asCompleted_isRejected() {
        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), "Fix the build", FeedbackType.ACTION_ITEM, alice(), Priority.HIGH, true));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Action items must start incomplete");
    }

    @Test
    void creatingAnActionItem_withoutPriority_isRejected() {
        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), "Fix the build", FeedbackType.ACTION_ITEM, alice(), null, null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Priority is required for action items");
    }

    @ParameterizedTest
    @CsvSource(value = {"HIGH,null", "null,false", "null,true"}, nullValues = "null")
    void creatingNonActionFeedback_withActionFields_isRejected(Priority priority, Boolean completed) {
        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), "Good pairing", FeedbackType.WENT_WELL, alice(), priority, completed));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class)
            .hasMessage("Priority and completed are only allowed for action items");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void creatingFeedback_withBlankContent_isRejected(String content) {
        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), content, FeedbackType.WENT_WELL, alice(), null, null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Content must not be blank");
    }

    @Test
    void creatingFeedback_withContentOver4000Characters_isRejectedAndExactly4000Accepted() {
        // Act
        Throwable tooLong = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), "a".repeat(4001), FeedbackType.WENT_WELL, alice(), null, null));
        FeedbackItem accepted = feedbackService.createFeedback(
            retro.getId(), "a".repeat(4000), FeedbackType.WENT_WELL, alice(), null, null);

        // Assert
        assertThat(tooLong).isInstanceOf(RetroflowException.class).hasMessage("Content must be at most 4000 characters");
        assertThat(accepted.getContent()).hasSize(4000);
    }

    @Test
    void creatingFeedback_withoutAType_isRejected() {
        // Act
        Throwable thrown = catchThrowable(() ->
            feedbackService.createFeedback(retro.getId(), "Good pairing", null, alice(), null, null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Type is required");
    }

    @Test
    void creatingFeedback_withMissingResources_isNotFound() {
        // Act
        Throwable unknownRetro = catchThrowable(() -> feedbackService.createFeedback(
            UUID.randomUUID(), "Good pairing", FeedbackType.WENT_WELL, alice(), null, null));
        Throwable unknownSubmitter = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), "Good pairing", FeedbackType.WENT_WELL, UUID.randomUUID(), null, null));

        // Assert
        assertThat(unknownRetro).isInstanceOf(ResourceNotFoundException.class).hasMessage("Retrospective not found");
        assertThat(unknownSubmitter).isInstanceOf(ResourceNotFoundException.class).hasMessage("Member not found");
    }

    @Test
    void creatingFeedback_withoutASubmitterId_isRejected() {
        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), "Good pairing", FeedbackType.WENT_WELL, null, null, null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Member ID is required");
    }

    @Test
    void creatingFeedback_asAMemberOfAnotherTeamOrARemovedMember_isRejected() {
        // Arrange
        teamService.removeMember(team.getId(), bob());

        // Act
        Throwable otherTeamMember = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), "Good pairing", FeedbackType.WENT_WELL, carol(), null, null));
        Throwable removedMember = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), "Good pairing", FeedbackType.WENT_WELL, bob(), null, null));

        // Assert
        assertThat(otherTeamMember).isInstanceOf(RetroflowException.class).hasMessage("Member is not part of this team");
        assertThat(removedMember).isInstanceOf(RetroflowException.class).hasMessage("Member is not part of this team");
    }

    @Test
    void creatingFeedback_checksContentBeforeLookingUpTheRetrospective() {
        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.createFeedback(
            UUID.randomUUID(), " ", FeedbackType.WENT_WELL, UUID.randomUUID(), null, null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Content must not be blank");
    }

    @Test
    void creatingFeedback_looksUpTheRetrospectiveBeforeTheSubmitter() {
        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.createFeedback(
            UUID.randomUUID(), "Good pairing", FeedbackType.WENT_WELL, UUID.randomUUID(), null, null));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Retrospective not found");
    }

    @Test
    void creatingFeedback_inAClosedRetrospective_isRejectedBeforeFieldRulesAndMembership() {
        // Arrange
        close();

        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), "Fix the build", FeedbackType.ACTION_ITEM, carol(), Priority.HIGH, true));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Retrospective is closed");
    }

    @Test
    void creatingFeedback_withAnUnknownSubmitterInAClosedRetrospective_isNotFound() {
        // Arrange
        close();

        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), "Good pairing", FeedbackType.WENT_WELL, UUID.randomUUID(), null, null));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Member not found");
    }

    @Test
    void creatingFeedback_checksActionFieldsBeforeMembership() {
        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.createFeedback(
            retro.getId(), "Good pairing", FeedbackType.WENT_WELL, carol(), Priority.HIGH, null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class)
            .hasMessage("Priority and completed are only allowed for action items");
    }

    @ParameterizedTest
    @ValueSource(strings = {"create", "update", "delete", "complete"})
    void inAClosedRetrospective_everyWriteIsRejected(String operation) {
        // Arrange
        UUID noteId = note().getId();
        UUID actionId = action(Priority.HIGH).getId();
        close();
        Runnable write = switch (operation) {
            case "create" -> () -> feedbackService.createFeedback(
                retro.getId(), "More", FeedbackType.WENT_WELL, alice(), null, null);
            case "update" -> () -> feedbackService.updateFeedback(noteId, "Changed", null, null);
            case "delete" -> () -> feedbackService.deleteFeedback(noteId);
            default -> () -> feedbackService.completeActionItem(actionId);
        };

        // Act
        Throwable thrown = catchThrowable(write::run);

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Retrospective is closed");
    }

    @Test
    void inAClosedRetrospective_itemsStayReadable() {
        // Arrange
        FeedbackItem note = note();
        close();

        // Act
        List<FeedbackItem> items = feedbackService.listFeedback(retro.getId());

        // Assert
        assertThat(items).extracting(FeedbackItem::getId).containsExactly(note.getId());
    }

    @Test
    void updatingClosedFeedback_isRejectedBeforeTheActionFieldRules() {
        // Arrange
        UUID noteId = note().getId();
        close();

        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.updateFeedback(noteId, "Changed", Priority.HIGH, true));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Retrospective is closed");
    }

    @Test
    void listingFeedback_returnsTheRetrospectivesItemsOldestFirst() {
        // Arrange
        FeedbackItem first = note();
        FeedbackItem second = action(Priority.LOW);
        FeedbackItem third = note();
        Retrospective otherRetro = retrospectiveService.createRetrospective(
            otherTeam.getId(), "Elsewhere", carol(), null);
        feedbackService.createFeedback(otherRetro.getId(), "Elsewhere", FeedbackType.WENT_WELL, carol(), null, null);

        // Act
        List<FeedbackItem> items = feedbackService.listFeedback(retro.getId());

        // Assert
        assertThat(items).extracting(FeedbackItem::getId).containsExactly(first.getId(), second.getId(), third.getId());
    }

    @Test
    void listingFeedback_forAMissingRetrospective_isNotFound() {
        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.listFeedback(UUID.randomUUID()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Retrospective not found");
    }

    @Test
    void updatingAnActionItem_replacesContentAndPriorityAndKeepsItsCompletion() {
        // Arrange
        FeedbackItem item = action(Priority.HIGH);
        feedbackService.completeActionItem(item.getId());

        // Act
        FeedbackItem updated = feedbackService.updateFeedback(item.getId(), "Fix the pipeline", Priority.LOW, null);

        // Assert
        FeedbackItem stored = feedbackService.listFeedback(retro.getId()).get(0);
        assertThat(updated.getId()).isEqualTo(item.getId());
        assertThat(stored.getContent()).isEqualTo("Fix the pipeline");
        assertThat(stored.getPriority()).isEqualTo(Priority.LOW);
        assertThat(stored.getCompleted()).isTrue();
        assertThat(stored.getType()).isEqualTo(FeedbackType.ACTION_ITEM);
        assertThat(stored.getSubmitter().getId()).isEqualTo(alice());
    }

    @Test
    void updatingAnActionItem_withCompletedTrue_completesIt() {
        // Arrange
        FeedbackItem item = action(Priority.HIGH);

        // Act
        FeedbackItem updated = feedbackService.updateFeedback(item.getId(), "Fix the build", Priority.HIGH, true);

        // Assert
        assertThat(updated.getCompleted()).isTrue();
    }

    @Test
    void updatingAnIncompleteActionItem_withCompletedFalse_keepsItIncomplete() {
        // Arrange
        FeedbackItem item = action(Priority.HIGH);

        // Act
        FeedbackItem updated = feedbackService.updateFeedback(item.getId(), "Fix the build", Priority.HIGH, false);

        // Assert
        assertThat(updated.getCompleted()).isFalse();
    }

    @Test
    void updatingACompletedActionItem_withCompletedFalse_isRejected() {
        // Arrange
        FeedbackItem item = action(Priority.HIGH);
        feedbackService.completeActionItem(item.getId());

        // Act
        Throwable thrown = catchThrowable(() ->
            feedbackService.updateFeedback(item.getId(), "Fix the build", Priority.HIGH, false));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Action item cannot be uncompleted");
        assertThat(feedbackService.listFeedback(retro.getId()).get(0).getCompleted()).isTrue();
    }

    @Test
    void updatingAnActionItem_withoutPriority_isRejected() {
        // Arrange
        FeedbackItem item = action(Priority.HIGH);

        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.updateFeedback(item.getId(), "Fix the build", null, null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Priority is required for action items");
    }

    @Test
    void updatingNonActionFeedback_changesOnlyItsContent() {
        // Arrange
        FeedbackItem item = note();

        // Act
        FeedbackItem updated = feedbackService.updateFeedback(item.getId(), "Great pairing", null, null);

        // Assert
        assertThat(updated.getContent()).isEqualTo("Great pairing");
        assertThat(updated.getType()).isEqualTo(FeedbackType.WENT_WELL);
        assertThat(updated.getPriority()).isNull();
        assertThat(updated.getCompleted()).isNull();
    }

    @ParameterizedTest
    @CsvSource(value = {"HIGH,null", "null,false"}, nullValues = "null")
    void updatingNonActionFeedback_withActionFields_isRejected(Priority priority, Boolean completed) {
        // Arrange
        FeedbackItem item = note();

        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.updateFeedback(item.getId(), "Changed", priority, completed));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class)
            .hasMessage("Priority and completed are only allowed for action items");
    }

    @Test
    void updatingFeedback_withBlankContentOrForAMissingItem_isRejected() {
        // Arrange
        FeedbackItem item = note();

        // Act
        Throwable blank = catchThrowable(() -> feedbackService.updateFeedback(item.getId(), "  ", null, null));
        Throwable tooLong = catchThrowable(() -> feedbackService.updateFeedback(item.getId(), "a".repeat(4001), null, null));
        Throwable missing = catchThrowable(() -> feedbackService.updateFeedback(UUID.randomUUID(), "Changed", null, null));

        // Assert
        assertThat(blank).isInstanceOf(RetroflowException.class).hasMessage("Content must not be blank");
        assertThat(tooLong).isInstanceOf(RetroflowException.class).hasMessage("Content must be at most 4000 characters");
        assertThat(missing).isInstanceOf(ResourceNotFoundException.class).hasMessage("Feedback item not found");
    }

    @Test
    void theSubmitterName_isRecordedAtSubmissionAndSurvivesARename() {
        // Arrange
        FeedbackItem item = note();

        // Act
        teamService.renameMember(team.getId(), alice(), "Alicia");

        // Assert
        FeedbackItem stored = feedbackService.listFeedback(retro.getId()).get(0);
        assertThat(stored.getId()).isEqualTo(item.getId());
        assertThat(stored.getSubmitterName()).isEqualTo("Alice");
        assertThat(stored.getSubmitter().getName()).isEqualTo("Alicia");
    }

    @Test
    void feedbackKeepsItsSubmitter_whenTheMemberIsRemoved() {
        // Arrange
        FeedbackItem item = feedbackService.createFeedback(
            retro.getId(), "Good pairing", FeedbackType.WENT_WELL, bob(), null, null);

        // Act
        teamService.removeMember(team.getId(), bob());

        // Assert
        FeedbackItem stored = feedbackService.listFeedback(retro.getId()).get(0);
        assertThat(stored.getId()).isEqualTo(item.getId());
        assertThat(stored.getSubmitter().getId()).isEqualTo(bob());
        assertThat(stored.getSubmitterName()).isEqualTo("Bob");
    }

    @Test
    void deletingFeedback_removesItWhileTheRetrospectiveIsOpen() {
        // Arrange
        FeedbackItem item = note();

        // Act
        feedbackService.deleteFeedback(item.getId());

        // Assert
        assertThat(feedbackService.listFeedback(retro.getId())).isEmpty();
    }

    @Test
    void deletingAMissingFeedbackItem_isNotFound() {
        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.deleteFeedback(UUID.randomUUID()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Feedback item not found");
    }

    @Test
    void completingAnActionItem_isIdempotent() {
        // Arrange
        FeedbackItem item = action(Priority.HIGH);

        // Act
        FeedbackItem first = feedbackService.completeActionItem(item.getId());
        FeedbackItem second = feedbackService.completeActionItem(item.getId());

        // Assert
        assertThat(first.getCompleted()).isTrue();
        assertThat(second.getCompleted()).isTrue();
        assertThat(feedbackService.listFeedback(retro.getId()).get(0).getCompleted()).isTrue();
    }

    @Test
    void completingNonActionFeedbackOrAMissingItem_isNotFound() {
        // Arrange
        FeedbackItem item = note();

        // Act
        Throwable nonAction = catchThrowable(() -> feedbackService.completeActionItem(item.getId()));
        Throwable missing = catchThrowable(() -> feedbackService.completeActionItem(UUID.randomUUID()));

        // Assert
        assertThat(nonAction).isInstanceOf(ResourceNotFoundException.class).hasMessage("Feedback item not found");
        assertThat(missing).isInstanceOf(ResourceNotFoundException.class).hasMessage("Feedback item not found");
    }

    @Test
    void uncompletingAnIncompleteActionItem_changesNothingAndACompletedOneIsRejected() {
        // Arrange
        FeedbackItem incomplete = action(Priority.HIGH);
        FeedbackItem completed = action(Priority.LOW);
        feedbackService.completeActionItem(completed.getId());

        // Act
        FeedbackItem unchanged = feedbackService.uncompleteActionItem(incomplete.getId());
        Throwable thrown = catchThrowable(() -> feedbackService.uncompleteActionItem(completed.getId()));

        // Assert
        assertThat(unchanged.getCompleted()).isFalse();
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Action item cannot be uncompleted");
    }

    @Test
    void creatingFeedbackBySubmitterName_resolvesTheMemberTrimmedAndCaseInsensitively() {
        // Act
        FeedbackItem note = feedbackService.createFeedbackBySubmitterName(
            retro.getId(), "Good pairing", "WENT_WELL", "  aLiCe ");
        FeedbackItem action = feedbackService.createActionItemBySubmitterName(
            retro.getId(), "Fix the build", "HIGH", "bob");

        // Assert
        assertThat(note.getSubmitter().getId()).isEqualTo(alice());
        assertThat(note.getSubmitterName()).isEqualTo("Alice");
        assertThat(action.getSubmitter().getId()).isEqualTo(bob());
        assertThat(action.getType()).isEqualTo(FeedbackType.ACTION_ITEM);
        assertThat(action.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(action.getCompleted()).isFalse();
    }

    @Test
    void creatingFeedbackBySubmitterName_withNoMatchOrTwoMatches_isRejected() {
        // Arrange
        Team sams = newTeam("Sam", "sam");
        Retrospective samsRetro = retrospectiveService.createRetrospective(
            sams.getId(), "Sprint 1", sams.getMembers().get(0).getId(), null);

        // Act
        Throwable noMatch = catchThrowable(() -> feedbackService.createFeedbackBySubmitterName(
            retro.getId(), "Good pairing", "WENT_WELL", "Zed"));
        Throwable ambiguous = catchThrowable(() -> feedbackService.createFeedbackBySubmitterName(
            samsRetro.getId(), "Good pairing", "WENT_WELL", "SAM"));

        // Assert
        assertThat(noMatch).isInstanceOf(RetroflowException.class).hasMessage("Member is not part of this team");
        assertThat(ambiguous).isInstanceOf(RetroflowException.class).hasMessage("Member name is ambiguous");
    }

    @Test
    void creatingFeedbackBySubmitterName_withAnUnknownTypeOrPriority_isRejected() {
        // Act
        Throwable badType = catchThrowable(() -> feedbackService.createFeedbackBySubmitterName(
            retro.getId(), "Good pairing", "bogus", "Alice"));
        Throwable badPriority = catchThrowable(() -> feedbackService.createActionItemBySubmitterName(
            retro.getId(), "Fix the build", "bogus", "Alice"));

        // Assert
        assertThat(badType).isInstanceOf(RetroflowException.class)
            .hasMessage("Type must be one of WENT_WELL, NEEDS_IMPROVEMENT, ACTION_ITEM");
        assertThat(badPriority).isInstanceOf(RetroflowException.class)
            .hasMessage("Priority must be one of HIGH, MEDIUM, LOW");
    }

    @ParameterizedTest
    @CsvSource(value = {"high,null,ab", "null,TRUE,b", "HIGH,false,a", "null,null,abc"}, nullValues = "null")
    void listingActionItems_appliesTheFilters(String priority, String completed, String expected) {
        // Arrange
        Map<Character, FeedbackItem> items = Map.of(
            'a', action(Priority.HIGH),
            'b', action(Priority.HIGH),
            'c', action(Priority.LOW));
        feedbackService.completeActionItem(items.get('b').getId());
        note();

        // Act
        List<FeedbackItem> result = feedbackService.listActionItems(priority, completed);

        // Assert
        List<UUID> expectedIds = expected.chars().mapToObj(letter -> items.get((char) letter).getId()).toList();
        assertThat(ofRetrospectives(result, retro.getId())).extracting(FeedbackItem::getId)
            .containsExactlyInAnyOrderElementsOf(expectedIds);
    }

    @ParameterizedTest
    @CsvSource(value = {
        "bogus|null|Priority must be one of HIGH, MEDIUM, LOW",
        "null|maybe|Completed must be true or false",
        "bogus|maybe|Priority must be one of HIGH, MEDIUM, LOW"
    }, delimiter = '|', nullValues = "null")
    void listingActionItems_withAnInvalidFilter_isRejectedWithThePriorityErrorFirst(
            String priority, String completed, String message) {
        // Act
        Throwable thrown = catchThrowable(() -> feedbackService.listActionItems(priority, completed));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage(message);
    }

    @Test
    void listingActionItems_ordersOpenRetrospectivesFirstThenPriorityThenCreation() {
        // Arrange
        FeedbackItem low = action(Priority.LOW);
        FeedbackItem highFirst = action(Priority.HIGH);
        FeedbackItem medium = action(Priority.MEDIUM);
        FeedbackItem highSecond = action(Priority.HIGH);
        Retrospective otherRetro = retrospectiveService.createRetrospective(
            otherTeam.getId(), "Elsewhere", carol(), null);
        FeedbackItem closedHigh = feedbackService.createFeedback(
            otherRetro.getId(), "Old", FeedbackType.ACTION_ITEM, carol(), Priority.HIGH, null);
        retrospectiveService.closeRetrospective(otherRetro.getId(), carol());

        // Act
        List<FeedbackItem> result = feedbackService.listActionItems(null, null);

        // Assert
        assertThat(ofRetrospectives(result, retro.getId(), otherRetro.getId())).extracting(FeedbackItem::getId)
            .containsExactly(highFirst.getId(), highSecond.getId(), medium.getId(), low.getId(), closedHigh.getId());
    }

    private FeedbackItem note() {
        return feedbackService.createFeedback(retro.getId(), "Good pairing", FeedbackType.WENT_WELL, alice(), null, null);
    }

    private FeedbackItem action(Priority priority) {
        return feedbackService.createFeedback(
            retro.getId(), "Fix the build", FeedbackType.ACTION_ITEM, alice(), priority, null);
    }

    private void close() {
        retrospectiveService.closeRetrospective(retro.getId(), alice());
    }

    private static List<FeedbackItem> ofRetrospectives(List<FeedbackItem> items, UUID... retrospectiveIds) {
        Set<UUID> wanted = Set.of(retrospectiveIds);
        return items.stream().filter(item -> wanted.contains(item.getRetrospective().getId())).toList();
    }

    private Team newTeam(String... members) {
        return teamService.createTeam("T" + UUID.randomUUID().toString().replace("-", ""), List.of(members));
    }

    private UUID alice() {
        return team.getMembers().get(0).getId();
    }

    private UUID bob() {
        return team.getMembers().get(1).getId();
    }

    private UUID carol() {
        return otherTeam.getMembers().get(0).getId();
    }
}
