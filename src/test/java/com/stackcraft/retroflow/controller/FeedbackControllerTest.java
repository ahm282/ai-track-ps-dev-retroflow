package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Member;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.service.FeedbackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer contract tests for the feedback and action-item routes: raw JSON requests and responses, mocked
 * service. Entities appear only in the stub helpers. Cases that cannot occur for a route are listed in
 * test-matrix.md under "Contract Test Coverage".
 */
@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    private static final UUID TEAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ALICE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID RETRO_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ITEM_ID = UUID.fromString("55555555-5555-5555-5555-555555555555");

    private static final String NOTE_JSON = """
        {
          "id": "55555555-5555-5555-5555-555555555555",
          "retrospectiveId": "44444444-4444-4444-4444-444444444444",
          "content": "Good pairing",
          "type": "WENT_WELL",
          "submitterId": "22222222-2222-2222-2222-222222222222",
          "submitterName": "Alice"
        }
        """;

    private static final String ACTION_JSON = """
        {
          "id": "55555555-5555-5555-5555-555555555555",
          "retrospectiveId": "44444444-4444-4444-4444-444444444444",
          "content": "Fix the build",
          "type": "ACTION_ITEM",
          "submitterId": "22222222-2222-2222-2222-222222222222",
          "submitterName": "Alice",
          "priority": "HIGH",
          "completed": %s
        }
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FeedbackService feedbackService;

    @Test
    void creatingFeedback_returnsTheItemWithoutActionFields() throws Exception {
        // Arrange
        when(feedbackService.createFeedback(RETRO_ID, "Good pairing", FeedbackType.WENT_WELL, ALICE_ID, null, null))
            .thenReturn(note("Good pairing"));

        // Act
        ResultActions result = mockMvc.perform(post("/api/retrospectives/{id}/feedback", RETRO_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content": "Good pairing", "type": "WENT_WELL", "submitterId": "22222222-2222-2222-2222-222222222222"}
                """));

        // Assert
        result.andExpect(status().isCreated());
        result.andExpect(content().json(NOTE_JSON, true));
    }

    @Test
    void creatingAnActionItem_returnsItWithPriorityAndCompleted() throws Exception {
        // Arrange
        when(feedbackService.createFeedback(
            RETRO_ID, "Fix the build", FeedbackType.ACTION_ITEM, ALICE_ID, Priority.HIGH, false))
            .thenReturn(action(false));

        // Act
        ResultActions result = mockMvc.perform(post("/api/retrospectives/{id}/feedback", RETRO_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content": "Fix the build", "type": "ACTION_ITEM", "priority": "HIGH", "completed": false,
                 "submitterId": "22222222-2222-2222-2222-222222222222"}
                """));

        // Assert
        result.andExpect(status().isCreated());
        result.andExpect(content().json(ACTION_JSON.formatted("false"), true));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidCreateRequests")
    void creatingFeedback_withInvalidInput_isRejected(String body, String message) throws Exception {
        // Act
        ResultActions result = mockMvc.perform(post("/api/retrospectives/{id}/feedback", RETRO_ID)
            .contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("{\"message\": \"%s\"}".formatted(message), true));
    }

    static Stream<Arguments> invalidCreateRequests() {
        String alice = "\"submitterId\": \"22222222-2222-2222-2222-222222222222\"";
        return Stream.of(
            Arguments.of("{\"type\": \"WENT_WELL\", " + alice + "}", "content: Content must not be blank"),
            Arguments.of("{\"content\": \"   \", \"type\": \"WENT_WELL\", " + alice + "}",
                "content: Content must not be blank"),
            Arguments.of("{\"content\": \"" + "a".repeat(4001) + "\", \"type\": \"WENT_WELL\", " + alice + "}",
                "content: Content must be at most 4000 characters"),
            Arguments.of("{\"content\": \"x\", " + alice + "}", "type: Type is required"),
            Arguments.of("{\"content\": \"x\", \"type\": \"WENT_WELL\"}", "submitterId: Submitter ID is required"),
            Arguments.of("{}", "content: Content must not be blank; submitterId: Submitter ID is required; "
                + "type: Type is required"),
            Arguments.of("{\"content\": \"x\", \"type\": \"POSITIVE\", " + alice + "}",
                "Invalid value for type; use one of WENT_WELL, NEEDS_IMPROVEMENT, ACTION_ITEM"),
            Arguments.of("{\"content\": \"x\", \"type\": \"ACTION_ITEM\", \"priority\": \"URGENT\", " + alice + "}",
                "Invalid value for priority; use one of HIGH, MEDIUM, LOW"),
            Arguments.of("{\"content\": \"x\", \"type\": \"ACTION_ITEM\", \"completed\": \"yes\", " + alice + "}",
                "Invalid value for completed; use true or false"),
            Arguments.of("{\"content\": \"x\", \"type\": \"WENT_WELL\", \"submitterId\": \"not-a-uuid\"}",
                "Invalid value for submitterId; use a valid UUID"),
            Arguments.of("", "Request body is required and must contain valid JSON"),
            Arguments.of("{\"content\": ", "Request body is required and must contain valid JSON"));
    }

    @Test
    void creatingFeedback_withMalformedRetrospectiveId_isRejected() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(post("/api/retrospectives/{id}/feedback", "not-a-uuid")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content": "Good pairing", "type": "WENT_WELL", "submitterId": "22222222-2222-2222-2222-222222222222"}
                """));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "retrospectiveId must be a valid UUID"}
            """, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Retrospective not found", "Member not found"})
    void creatingFeedback_forAMissingResource_returnsNotFound(String message) throws Exception {
        // Arrange
        when(feedbackService.createFeedback(RETRO_ID, "Good pairing", FeedbackType.WENT_WELL, ALICE_ID, null, null))
            .thenThrow(new ResourceNotFoundException(message));

        // Act
        ResultActions result = mockMvc.perform(post("/api/retrospectives/{id}/feedback", RETRO_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content": "Good pairing", "type": "WENT_WELL", "submitterId": "22222222-2222-2222-2222-222222222222"}
                """));

        // Assert
        result.andExpect(status().isNotFound());
        result.andExpect(content().json("{\"message\": \"%s\"}".formatted(message), true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Retrospective is closed",
        "Member is not part of this team",
        "Priority is required for action items",
        "Priority and completed are only allowed for action items",
        "Action items must start incomplete"})
    void creatingFeedback_violatingARule_isRejected(String message) throws Exception {
        // Arrange
        when(feedbackService.createFeedback(RETRO_ID, "Good pairing", FeedbackType.WENT_WELL, ALICE_ID, null, null))
            .thenThrow(new RetroflowException(message));

        // Act
        ResultActions result = mockMvc.perform(post("/api/retrospectives/{id}/feedback", RETRO_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content": "Good pairing", "type": "WENT_WELL", "submitterId": "22222222-2222-2222-2222-222222222222"}
                """));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("{\"message\": \"%s\"}".formatted(message), true));
    }

    @Test
    void listingFeedback_returnsEveryItemWithOnlyTheFieldsItsTypeHas() throws Exception {
        // Arrange
        when(feedbackService.listFeedback(RETRO_ID)).thenReturn(List.of(note("Good pairing"), action(true)));

        // Act
        ResultActions result = mockMvc.perform(get("/api/retrospectives/{id}/feedback", RETRO_ID));

        // Assert
        result.andExpect(status().isOk());
        result.andExpect(content().json("[" + NOTE_JSON + "," + ACTION_JSON.formatted("true") + "]", true));
    }

    @Test
    void listingFeedback_withMalformedRetrospectiveId_isRejected() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get("/api/retrospectives/{id}/feedback", "not-a-uuid"));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "retrospectiveId must be a valid UUID"}
            """, true));
    }

    @Test
    void listingFeedback_forAMissingRetrospective_returnsNotFound() throws Exception {
        // Arrange
        when(feedbackService.listFeedback(RETRO_ID)).thenThrow(new ResourceNotFoundException("Retrospective not found"));

        // Act
        ResultActions result = mockMvc.perform(get("/api/retrospectives/{id}/feedback", RETRO_ID));

        // Assert
        result.andExpect(status().isNotFound());
        result.andExpect(content().json("""
            {"message": "Retrospective not found"}
            """, true));
    }

    @Test
    void updatingFeedback_returnsTheUpdatedItem() throws Exception {
        // Arrange
        when(feedbackService.updateFeedback(ITEM_ID, "Great pairing", null, null)).thenReturn(note("Great pairing"));

        // Act
        ResultActions result = mockMvc.perform(put("/api/feedback/{id}", ITEM_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content": "Great pairing"}
                """));

        // Assert
        result.andExpect(status().isOk());
        result.andExpect(content().json(NOTE_JSON.replace("Good pairing", "Great pairing"), true));
    }

    @Test
    void updatingAnActionItem_passesPriorityAndCompletedToTheService() throws Exception {
        // Arrange
        when(feedbackService.updateFeedback(ITEM_ID, "Fix the build", Priority.HIGH, true)).thenReturn(action(true));

        // Act
        ResultActions result = mockMvc.perform(put("/api/feedback/{id}", ITEM_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"content": "Fix the build", "priority": "HIGH", "completed": true}
                """));

        // Assert
        result.andExpect(status().isOk());
        result.andExpect(content().json(ACTION_JSON.formatted("true"), true));
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("invalidUpdateRequests")
    void updatingFeedback_withInvalidInput_isRejected(String body, String message) throws Exception {
        // Act
        ResultActions result = mockMvc.perform(put("/api/feedback/{id}", ITEM_ID)
            .contentType(MediaType.APPLICATION_JSON).content(body));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("{\"message\": \"%s\"}".formatted(message), true));
    }

    static Stream<Arguments> invalidUpdateRequests() {
        return Stream.of(
            Arguments.of("{}", "content: Content must not be blank"),
            Arguments.of("{\"content\": \"  \"}", "content: Content must not be blank"),
            Arguments.of("{\"content\": \"" + "a".repeat(4001) + "\"}",
                "content: Content must be at most 4000 characters"),
            Arguments.of("{\"content\": \"x\", \"priority\": \"URGENT\"}",
                "Invalid value for priority; use one of HIGH, MEDIUM, LOW"),
            Arguments.of("{\"content\": \"x\", \"completed\": \"yes\"}",
                "Invalid value for completed; use true or false"),
            Arguments.of("", "Request body is required and must contain valid JSON"));
    }

    @Test
    void updatingFeedback_withMalformedId_isRejected() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(put("/api/feedback/{id}", "not-a-uuid")
            .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"x\"}"));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "id must be a valid UUID"}
            """, true));
    }

    @Test
    void updatingAMissingFeedbackItem_returnsNotFound() throws Exception {
        // Arrange
        when(feedbackService.updateFeedback(ITEM_ID, "x", null, null))
            .thenThrow(new ResourceNotFoundException("Feedback item not found"));

        // Act
        ResultActions result = mockMvc.perform(put("/api/feedback/{id}", ITEM_ID)
            .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"x\"}"));

        // Assert
        result.andExpect(status().isNotFound());
        result.andExpect(content().json("""
            {"message": "Feedback item not found"}
            """, true));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Retrospective is closed",
        "Action item cannot be uncompleted",
        "Priority is required for action items",
        "Priority and completed are only allowed for action items"})
    void updatingFeedback_violatingARule_isRejected(String message) throws Exception {
        // Arrange
        when(feedbackService.updateFeedback(ITEM_ID, "x", null, null)).thenThrow(new RetroflowException(message));

        // Act
        ResultActions result = mockMvc.perform(put("/api/feedback/{id}", ITEM_ID)
            .contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"x\"}"));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("{\"message\": \"%s\"}".formatted(message), true));
    }

    @Test
    void deletingFeedback_returnsNoContent() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(delete("/api/feedback/{id}", ITEM_ID));

        // Assert
        result.andExpect(status().isNoContent());
        result.andExpect(content().string(""));
        verify(feedbackService).deleteFeedback(ITEM_ID);
    }

    @Test
    void deletingFeedback_withMalformedId_isRejected() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(delete("/api/feedback/{id}", "not-a-uuid"));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "id must be a valid UUID"}
            """, true));
    }

    @Test
    void deletingAMissingFeedbackItem_returnsNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Feedback item not found")).when(feedbackService).deleteFeedback(ITEM_ID);

        // Act
        ResultActions result = mockMvc.perform(delete("/api/feedback/{id}", ITEM_ID));

        // Assert
        result.andExpect(status().isNotFound());
        result.andExpect(content().json("""
            {"message": "Feedback item not found"}
            """, true));
    }

    @Test
    void deletingFeedbackInAClosedRetrospective_isRejected() throws Exception {
        // Arrange
        doThrow(new RetroflowException("Retrospective is closed")).when(feedbackService).deleteFeedback(ITEM_ID);

        // Act
        ResultActions result = mockMvc.perform(delete("/api/feedback/{id}", ITEM_ID));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "Retrospective is closed"}
            """, true));
    }

    @Test
    void listingActionItems_withoutFilters_returnsThem() throws Exception {
        // Arrange
        when(feedbackService.listActionItems(null, null)).thenReturn(List.of(action(false)));

        // Act
        ResultActions result = mockMvc.perform(get("/api/action-items"));

        // Assert
        result.andExpect(status().isOk());
        result.andExpect(content().json("[" + ACTION_JSON.formatted("false") + "]", true));
    }

    @Test
    void listingActionItems_passesTheFiltersToTheService() throws Exception {
        // Arrange
        when(feedbackService.listActionItems("HIGH", "false")).thenReturn(List.of(action(false)));

        // Act
        ResultActions result = mockMvc.perform(get("/api/action-items")
            .param("priority", "HIGH").param("completed", "false"));

        // Assert
        result.andExpect(status().isOk());
        result.andExpect(content().json("[" + ACTION_JSON.formatted("false") + "]", true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Priority must be one of HIGH, MEDIUM, LOW", "Completed must be true or false"})
    void listingActionItems_withAnInvalidFilter_isRejected(String message) throws Exception {
        // Arrange
        when(feedbackService.listActionItems("bogus", null)).thenThrow(new RetroflowException(message));

        // Act
        ResultActions result = mockMvc.perform(get("/api/action-items").param("priority", "bogus"));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("{\"message\": \"%s\"}".formatted(message), true));
    }

    @Test
    void completingAnActionItem_returnsItCompleted() throws Exception {
        // Arrange
        when(feedbackService.completeActionItem(ITEM_ID)).thenReturn(action(true));

        // Act
        ResultActions result = mockMvc.perform(put("/api/action-items/{id}/complete", ITEM_ID));

        // Assert
        result.andExpect(status().isOk());
        result.andExpect(content().json(ACTION_JSON.formatted("true"), true));
    }

    @Test
    void completingAnActionItem_withMalformedId_isRejected() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(put("/api/action-items/{id}/complete", "not-a-uuid"));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "id must be a valid UUID"}
            """, true));
    }

    @Test
    void completingAMissingOrNonActionItem_returnsNotFound() throws Exception {
        // Arrange
        when(feedbackService.completeActionItem(ITEM_ID)).thenThrow(new ResourceNotFoundException("Feedback item not found"));

        // Act
        ResultActions result = mockMvc.perform(put("/api/action-items/{id}/complete", ITEM_ID));

        // Assert
        result.andExpect(status().isNotFound());
        result.andExpect(content().json("""
            {"message": "Feedback item not found"}
            """, true));
    }

    @Test
    void completingAnActionItemInAClosedRetrospective_isRejected() throws Exception {
        // Arrange
        when(feedbackService.completeActionItem(ITEM_ID)).thenThrow(new RetroflowException("Retrospective is closed"));

        // Act
        ResultActions result = mockMvc.perform(put("/api/action-items/{id}/complete", ITEM_ID));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "Retrospective is closed"}
            """, true));
    }

    private static FeedbackItem note(String content) {
        return new FeedbackItem(ITEM_ID, content, FeedbackType.WENT_WELL, null, null, null, alice(), "Alice", retro());
    }

    private static FeedbackItem action(boolean completed) {
        return new FeedbackItem(ITEM_ID, "Fix the build", FeedbackType.ACTION_ITEM, Priority.HIGH, completed, null,
            alice(), "Alice", retro());
    }

    private static Member alice() {
        return new Member(ALICE_ID, "Alicia", true, null);
    }

    private static Retrospective retro() {
        Team team = new Team(TEAM_ID, "Team Alpha", "team alpha", List.of(alice()), null);
        return new Retrospective(RETRO_ID, "Sprint 1 Retro", LocalDate.of(2026, 10, 26), RetrospectiveStatus.OPEN,
            team, null);
    }
}
