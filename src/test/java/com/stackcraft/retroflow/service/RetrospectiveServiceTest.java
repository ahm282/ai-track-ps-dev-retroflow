package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@SpringBootTest
class RetrospectiveServiceTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private RetrospectiveService retrospectiveService;

    @Test
    void creatingARetrospective_defaultsToOpenAndTodayInUtc() {
        // Arrange
        Team team = team("Alice");
        LocalDate before = LocalDate.now(ZoneOffset.UTC);

        // Act
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);

        // Assert
        LocalDate after = LocalDate.now(ZoneOffset.UTC);
        Retrospective stored = retrospectiveService.getRetrospective(retro.getId());
        assertThat(stored.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
        assertThat(stored.getDate()).isBetween(before, after);
        assertThat(stored.getTitle()).isEqualTo("Sprint 1");
        assertThat(stored.getTeam().getId()).isEqualTo(team.getId());
    }

    @Test
    void creatingARetrospective_withAnExplicitClosedStatus_isStoredClosedEvenWhileAnotherIsOpen() {
        // Arrange
        Team team = team("Alice");
        retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);

        // Act
        Retrospective closed = retrospectiveService.createRetrospective(
            team.getId(), "Old Sprint", memberId(team, 0), RetrospectiveStatus.CLOSED);

        // Assert
        assertThat(retrospectiveService.getRetrospective(closed.getId()).getStatus())
            .isEqualTo(RetrospectiveStatus.CLOSED);
    }

    @Test
    void aTeamWithAnOpenRetrospective_cannotOpenASecondOne() {
        // Arrange
        Team team = team("Alice");
        retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);

        // Act
        Throwable thrown = catchThrowable(() ->
            retrospectiveService.createRetrospective(team.getId(), "Sprint 2", memberId(team, 0), null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Only 1 retro can be open");
    }

    @Test
    void anotherTeam_canOpenARetrospectiveWhileThisOneHasAnOpenRetrospective() {
        // Arrange
        Team team = team("Alice");
        Team other = team("Carol");
        retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);

        // Act
        Retrospective retro = retrospectiveService.createRetrospective(other.getId(), "Sprint 1", memberId(other, 0), null);

        // Assert
        assertThat(retro.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
    }

    @Test
    void afterClosingTheOpenRetrospective_aNewOneCanBeOpened() {
        // Arrange
        Team team = team("Alice");
        Retrospective first = retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);
        retrospectiveService.closeRetrospective(first.getId(), memberId(team, 0));

        // Act
        Retrospective second = retrospectiveService.createRetrospective(team.getId(), "Sprint 2", memberId(team, 0), null);

        // Assert
        assertThat(second.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
    }

    @Test
    void creatingARetrospective_forAMissingTeam_isNotFound() {
        // Arrange
        Team team = team("Alice");

        // Act
        Throwable thrown = catchThrowable(() ->
            retrospectiveService.createRetrospective(UUID.randomUUID(), "Sprint 1", memberId(team, 0), null));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Team not found");
    }

    @Test
    void creatingARetrospective_asAMemberOfAnotherTeam_isRejected() {
        // Arrange
        Team team = team("Alice");
        Team other = team("Carol");

        // Act
        Throwable thrown = catchThrowable(() ->
            retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(other, 0), null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Member is not part of this team");
    }

    @Test
    void creatingARetrospective_asARemovedMember_isRejected() {
        // Arrange
        Team team = team("Alice", "Bob");
        teamService.removeMember(team.getId(), memberId(team, 1));

        // Act
        Throwable thrown = catchThrowable(() ->
            retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 1), null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Member is not part of this team");
    }

    @Test
    void creatingARetrospective_asAnUnknownMember_isNotFound() {
        // Arrange
        Team team = team("Alice");

        // Act
        Throwable thrown = catchThrowable(() ->
            retrospectiveService.createRetrospective(team.getId(), "Sprint 1", UUID.randomUUID(), null));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Member not found");
    }

    @Test
    void creatingARetrospective_withoutAMemberId_isRejected() {
        // Arrange
        Team team = team("Alice");

        // Act
        Throwable thrown = catchThrowable(() ->
            retrospectiveService.createRetrospective(team.getId(), "Sprint 1", null, null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Member ID is required");
    }

    @Test
    void creatingARetrospective_checksTheMemberBeforeTheOpenRetrospectiveRule() {
        // Arrange
        Team team = team("Alice");
        Team other = team("Carol");
        retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);

        // Act
        Throwable unknownMember = catchThrowable(() ->
            retrospectiveService.createRetrospective(team.getId(), "Sprint 2", UUID.randomUUID(), null));
        Throwable nonMember = catchThrowable(() ->
            retrospectiveService.createRetrospective(team.getId(), "Sprint 2", memberId(other, 0), null));

        // Assert
        assertThat(unknownMember).isInstanceOf(ResourceNotFoundException.class).hasMessage("Member not found");
        assertThat(nonMember).isInstanceOf(RetroflowException.class).hasMessage("Member is not part of this team");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void creatingARetrospective_withABlankTitle_isRejected(String title) {
        // Arrange
        Team team = team("Alice");

        // Act
        Throwable thrown = catchThrowable(() ->
            retrospectiveService.createRetrospective(team.getId(), title, memberId(team, 0), null));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessageStartingWith("title: ");
    }

    @Test
    void creatingARetrospective_withA256CharacterTitle_isRejectedAndA255CharacterTitleAccepted() {
        // Arrange
        Team team = team("Alice");
        Team other = team("Carol");

        // Act
        Throwable tooLong = catchThrowable(() ->
            retrospectiveService.createRetrospective(team.getId(), "a".repeat(256), memberId(team, 0), null));
        Retrospective accepted = retrospectiveService.createRetrospective(
            other.getId(), "a".repeat(255), memberId(other, 0), null);

        // Assert
        assertThat(tooLong).isInstanceOf(RetroflowException.class).hasMessageStartingWith("title: ");
        assertThat(accepted.getTitle()).hasSize(255);
    }

    @Test
    void gettingARetrospective_returnsItOrIsNotFound() {
        // Arrange
        Team team = team("Alice");
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);

        // Act
        Retrospective found = retrospectiveService.getRetrospective(retro.getId());
        Throwable thrown = catchThrowable(() -> retrospectiveService.getRetrospective(UUID.randomUUID()));

        // Assert
        assertThat(found.getId()).isEqualTo(retro.getId());
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Retrospective not found");
    }

    @Test
    void closingARetrospective_marksItClosed() {
        // Arrange
        Team team = team("Alice");
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);

        // Act
        Retrospective closed = retrospectiveService.closeRetrospective(retro.getId(), memberId(team, 0));

        // Assert
        assertThat(closed.getStatus()).isEqualTo(RetrospectiveStatus.CLOSED);
        assertThat(retrospectiveService.getRetrospective(retro.getId()).getStatus())
            .isEqualTo(RetrospectiveStatus.CLOSED);
    }

    @Test
    void closingARetrospectiveTwice_isRejected() {
        // Arrange
        Team team = team("Alice");
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);
        retrospectiveService.closeRetrospective(retro.getId(), memberId(team, 0));

        // Act
        Throwable thrown = catchThrowable(() ->
            retrospectiveService.closeRetrospective(retro.getId(), memberId(team, 0)));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Retrospective already closed");
    }

    @Test
    void closingAClosedRetrospective_reportsItAsClosedBeforeCheckingMembership() {
        // Arrange
        Team team = team("Alice");
        Team other = team("Carol");
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);
        retrospectiveService.closeRetrospective(retro.getId(), memberId(team, 0));

        // Act
        Throwable thrown = catchThrowable(() ->
            retrospectiveService.closeRetrospective(retro.getId(), memberId(other, 0)));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Retrospective already closed");
    }

    @Test
    void closingARetrospective_asANonMember_isRejectedAndLeavesItOpen() {
        // Arrange
        Team team = team("Alice");
        Team other = team("Carol");
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);

        // Act
        Throwable thrown = catchThrowable(() ->
            retrospectiveService.closeRetrospective(retro.getId(), memberId(other, 0)));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Member is not part of this team");
        assertThat(retrospectiveService.getRetrospective(retro.getId()).getStatus())
            .isEqualTo(RetrospectiveStatus.OPEN);
    }

    @Test
    void closingARetrospective_withMissingResources_isNotFound() {
        // Arrange
        Team team = team("Alice");
        Retrospective retro = retrospectiveService.createRetrospective(team.getId(), "Sprint 1", memberId(team, 0), null);

        // Act
        Throwable unknownRetro = catchThrowable(() ->
            retrospectiveService.closeRetrospective(UUID.randomUUID(), UUID.randomUUID()));
        Throwable unknownMember = catchThrowable(() ->
            retrospectiveService.closeRetrospective(retro.getId(), UUID.randomUUID()));

        // Assert
        assertThat(unknownRetro).isInstanceOf(ResourceNotFoundException.class).hasMessage("Retrospective not found");
        assertThat(unknownMember).isInstanceOf(ResourceNotFoundException.class).hasMessage("Member not found");
    }

    @Test
    void listingRetrospectives_returnsTheTeamsOwnOldestFirst() {
        // Arrange
        Team team = team("Alice");
        Team other = team("Carol");
        UUID alice = memberId(team, 0);
        Retrospective first = retrospectiveService.createRetrospective(
            team.getId(), "Sprint 1", alice, RetrospectiveStatus.CLOSED);
        Retrospective second = retrospectiveService.createRetrospective(
            team.getId(), "Sprint 2", alice, RetrospectiveStatus.CLOSED);
        Retrospective third = retrospectiveService.createRetrospective(team.getId(), "Sprint 3", alice, null);
        retrospectiveService.createRetrospective(other.getId(), "Elsewhere", memberId(other, 0), null);

        // Act
        List<Retrospective> retros = retrospectiveService.listRetrospectives(team.getId());

        // Assert
        assertThat(retros).extracting(Retrospective::getId)
            .containsExactly(first.getId(), second.getId(), third.getId());
    }

    @Test
    void listingRetrospectives_forAMissingTeam_isNotFound() {
        // Act
        Throwable thrown = catchThrowable(() -> retrospectiveService.listRetrospectives(UUID.randomUUID()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Team not found");
    }

    private Team team(String... members) {
        return teamService.createTeam("T" + UUID.randomUUID().toString().replace("-", ""), List.of(members));
    }

    private static UUID memberId(Team team, int index) {
        return team.getMembers().get(index).getId();
    }
}
