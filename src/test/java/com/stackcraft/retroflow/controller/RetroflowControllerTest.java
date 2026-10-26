package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.entity.Member;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer contract tests: requests and expected responses are raw JSON taken from decisions.md,
 * so they hold when the planned refactor moves or renames the DTOs. Entities appear only in the
 * stub helpers, because the mocked services return them.
 */
@WebMvcTest(RetroflowController.class)
class RetroflowControllerTest {

    private static final UUID TEAM_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ALICE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID BOB_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID RETRO_ID = UUID.fromString("44444444-4444-4444-4444-444444444444");

    private static final String TEAM_JSON = """
        {
          "id": "11111111-1111-1111-1111-111111111111",
          "name": "Team Alpha",
          "members": [
            {"id": "22222222-2222-2222-2222-222222222222", "name": "Alice"},
            {"id": "33333333-3333-3333-3333-333333333333", "name": "Bob"}
          ]
        }
        """;

    private static final String CLOSE_REQUEST_JSON = """
        {"memberId": "22222222-2222-2222-2222-222222222222"}
        """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TeamService teamService;

    @MockBean
    private RetrospectiveService retrospectiveService;

    @Test
    void creatingATeam_returnsTheTeamWithItsMembers() throws Exception {
        // Arrange
        when(teamService.createTeam("Team Alpha", List.of("Alice", "Bob"))).thenReturn(team());

        // Act
        ResultActions result = mockMvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Team Alpha", "members": ["Alice", "Bob"]}
                """));

        // Assert
        result.andExpect(status().isCreated());
        result.andExpect(content().json(TEAM_JSON, true));
    }

    @Test
    void creatingATeam_withBlankName_isRejected() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "   ", "members": ["Alice"]}
                """));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "name: Team name must not be blank"}
            """, true));
    }

    @Test
    void creatingATeam_withDuplicateName_isRejected() throws Exception {
        // Arrange
        when(teamService.createTeam("Team Alpha", List.of("Alice")))
            .thenThrow(new RetroflowException("Team name already exists"));

        // Act
        ResultActions result = mockMvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Team Alpha", "members": ["Alice"]}
                """));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "Team name already exists"}
            """, true));
    }

    @Test
    void creatingATeam_withNonJsonContentType_isRejected() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(post("/api/teams").contentType(MediaType.TEXT_PLAIN)
            .content("Team Alpha"));

        // Assert
        result.andExpect(status().isUnsupportedMediaType());
        result.andExpect(content().json("""
            {"message": "Content type must be application/json"}
            """, true));
    }

    @Test
    void anUnexpectedServerError_returnsAGenericMessage() throws Exception {
        // Arrange
        when(teamService.createTeam("Team Alpha", List.of("Alice")))
            .thenThrow(new IllegalStateException("connection pool exhausted"));

        // Act
        ResultActions result = mockMvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"name": "Team Alpha", "members": ["Alice"]}
                """));

        // Assert
        result.andExpect(status().isInternalServerError());
        result.andExpect(content().json("""
            {"message": "Unexpected error"}
            """, true));
    }

    @Test
    void gettingATeam_returnsTheTeamWithItsMembers() throws Exception {
        // Arrange
        when(teamService.getTeam(TEAM_ID)).thenReturn(team());

        // Act
        ResultActions result = mockMvc.perform(get("/api/teams/{id}", TEAM_ID));

        // Assert
        result.andExpect(status().isOk());
        result.andExpect(content().json(TEAM_JSON, true));
    }

    @Test
    void gettingATeam_withMalformedId_isRejected() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get("/api/teams/{id}", "not-a-uuid"));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "id must be a valid UUID"}
            """, true));
    }

    @Test
    void gettingAMissingTeam_returnsNotFound() throws Exception {
        // Arrange
        when(teamService.getTeam(TEAM_ID)).thenThrow(new ResourceNotFoundException("Team not found"));

        // Act
        ResultActions result = mockMvc.perform(get("/api/teams/{id}", TEAM_ID));

        // Assert
        result.andExpect(status().isNotFound());
        result.andExpect(content().json("""
            {"message": "Team not found"}
            """, true));
    }

    @Test
    void creatingARetrospective_returnsTheRetrospective() throws Exception {
        // Arrange
        when(retrospectiveService.createRetrospective(TEAM_ID, "Sprint 1 Retro", ALICE_ID, null))
            .thenReturn(retrospective(RetrospectiveStatus.OPEN));

        // Act
        ResultActions result = mockMvc.perform(post("/api/teams/{teamId}/retrospectives", TEAM_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title": "Sprint 1 Retro", "memberId": "22222222-2222-2222-2222-222222222222"}
                """));

        // Assert
        result.andExpect(status().isCreated());
        result.andExpect(content().json(retrospectiveJson("OPEN"), true));
    }

    @Test
    void creatingARetrospective_withoutMemberId_isRejected() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(post("/api/teams/{teamId}/retrospectives", TEAM_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title": "Sprint 1 Retro"}
                """));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "memberId: Member ID is required"}
            """, true));
    }

    @Test
    void creatingARetrospective_forAMissingTeam_returnsNotFound() throws Exception {
        // Arrange
        when(retrospectiveService.createRetrospective(TEAM_ID, "Sprint 1 Retro", ALICE_ID, null))
            .thenThrow(new ResourceNotFoundException("Team not found"));

        // Act
        ResultActions result = mockMvc.perform(post("/api/teams/{teamId}/retrospectives", TEAM_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title": "Sprint 1 Retro", "memberId": "22222222-2222-2222-2222-222222222222"}
                """));

        // Assert
        result.andExpect(status().isNotFound());
        result.andExpect(content().json("""
            {"message": "Team not found"}
            """, true));
    }

    @Test
    void aTeamWithAnOpenRetrospective_cannotOpenASecondOne() throws Exception {
        // Arrange
        when(retrospectiveService.createRetrospective(TEAM_ID, "Sprint 2 Retro", ALICE_ID, null))
            .thenThrow(new RetroflowException("Only 1 retro can be open"));

        // Act
        ResultActions result = mockMvc.perform(post("/api/teams/{teamId}/retrospectives", TEAM_ID)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title": "Sprint 2 Retro", "memberId": "22222222-2222-2222-2222-222222222222"}
                """));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "Only 1 retro can be open"}
            """, true));
    }

    @Test
    void listingRetrospectives_returnsTheTeamsRetrospectives() throws Exception {
        // Arrange
        when(retrospectiveService.listRetrospectives(TEAM_ID))
            .thenReturn(List.of(retrospective(RetrospectiveStatus.OPEN)));

        // Act
        ResultActions result = mockMvc.perform(get("/api/teams/{teamId}/retrospectives", TEAM_ID));

        // Assert
        result.andExpect(status().isOk());
        result.andExpect(content().json("[" + retrospectiveJson("OPEN") + "]", true));
    }

    @Test
    void listingRetrospectives_withMalformedTeamId_isRejected() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get("/api/teams/{teamId}/retrospectives", "not-a-uuid"));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "teamId must be a valid UUID"}
            """, true));
    }

    @Test
    void listingRetrospectives_forAMissingTeam_returnsNotFound() throws Exception {
        // Arrange
        when(retrospectiveService.listRetrospectives(TEAM_ID))
            .thenThrow(new ResourceNotFoundException("Team not found"));

        // Act
        ResultActions result = mockMvc.perform(get("/api/teams/{teamId}/retrospectives", TEAM_ID));

        // Assert
        result.andExpect(status().isNotFound());
        result.andExpect(content().json("""
            {"message": "Team not found"}
            """, true));
    }

    @Test
    void closingARetrospective_returnsItAsClosed() throws Exception {
        // Arrange
        when(retrospectiveService.closeRetrospective(RETRO_ID, ALICE_ID))
            .thenReturn(retrospective(RetrospectiveStatus.CLOSED));

        // Act
        ResultActions result = mockMvc.perform(put("/api/retrospectives/{id}/close", RETRO_ID)
            .contentType(MediaType.APPLICATION_JSON).content(CLOSE_REQUEST_JSON));

        // Assert
        result.andExpect(status().isOk());
        result.andExpect(content().json(retrospectiveJson("CLOSED"), true));
    }

    @Test
    void closingARetrospective_withoutMemberId_isRejected() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(put("/api/retrospectives/{id}/close", RETRO_ID)
            .contentType(MediaType.APPLICATION_JSON).content("{}"));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "memberId: Member ID is required"}
            """, true));
    }

    @Test
    void closingAMissingRetrospective_returnsNotFound() throws Exception {
        // Arrange
        when(retrospectiveService.closeRetrospective(RETRO_ID, ALICE_ID))
            .thenThrow(new ResourceNotFoundException("Retrospective not found"));

        // Act
        ResultActions result = mockMvc.perform(put("/api/retrospectives/{id}/close", RETRO_ID)
            .contentType(MediaType.APPLICATION_JSON).content(CLOSE_REQUEST_JSON));

        // Assert
        result.andExpect(status().isNotFound());
        result.andExpect(content().json("""
            {"message": "Retrospective not found"}
            """, true));
    }

    @Test
    void closingAClosedRetrospective_isRejected() throws Exception {
        // Arrange
        when(retrospectiveService.closeRetrospective(RETRO_ID, ALICE_ID))
            .thenThrow(new RetroflowException("Retrospective already closed"));

        // Act
        ResultActions result = mockMvc.perform(put("/api/retrospectives/{id}/close", RETRO_ID)
            .contentType(MediaType.APPLICATION_JSON).content(CLOSE_REQUEST_JSON));

        // Assert
        result.andExpect(status().isBadRequest());
        result.andExpect(content().json("""
            {"message": "Retrospective already closed"}
            """, true));
    }

    @Test
    void closingARetrospective_withPost_isMethodNotAllowed() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(post("/api/retrospectives/{id}/close", RETRO_ID)
            .contentType(MediaType.APPLICATION_JSON).content(CLOSE_REQUEST_JSON));

        // Assert
        result.andExpect(status().isMethodNotAllowed());
        result.andExpect(content().json("""
            {"message": "Method POST is not supported for this endpoint"}
            """, true));
    }

    @Test
    void anUnknownRoute_returnsNotFound() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(get("/api/unknown"));

        // Assert
        result.andExpect(status().isNotFound());
        result.andExpect(content().json("""
            {"message": "No endpoint GET /api/unknown"}
            """, true));
    }

    private static Team team() {
        return new Team(TEAM_ID, "Team Alpha", "team alpha", List.of(
            new Member(ALICE_ID, "Alice", true, null),
            new Member(BOB_ID, "Bob", true, null)), null);
    }

    private static Retrospective retrospective(RetrospectiveStatus status) {
        return new Retrospective(RETRO_ID, "Sprint 1 Retro", LocalDate.of(2026, 10, 26), status, team(), null);
    }

    private static String retrospectiveJson(String status) {
        return """
            {
              "id": "44444444-4444-4444-4444-444444444444",
              "title": "Sprint 1 Retro",
              "date": "2026-10-26",
              "status": "%s",
              "teamId": "11111111-1111-1111-1111-111111111111"
            }
            """.formatted(status);
    }
}
