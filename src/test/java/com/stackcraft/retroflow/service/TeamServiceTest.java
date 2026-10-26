package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Member;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.MemberRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class TeamServiceTest {

    @Autowired
    private TeamService teamService;

    @Autowired
    private Validator validator;

    @Test
    void creatingATeam_storesItWithItsActiveMembers() {
        // Arrange
        String name = uniqueName();

        // Act
        Team team = teamService.createTeam(name, List.of("Alice", "Bob"));

        // Assert
        Team stored = teamService.getTeam(team.getId());
        assertThat(stored.getName()).isEqualTo(name);
        assertThat(stored.getMembers()).extracting(Member::getName).containsExactlyInAnyOrder("Alice", "Bob");
        assertThat(stored.getMembers()).allMatch(Member::isActive);
    }

    @ParameterizedTest
    @MethodSource("duplicateNameVariants")
    void creatingATeam_withADuplicateName_isRejected(UnaryOperator<String> variant) {
        // Arrange
        String name = uniqueName();
        teamService.createTeam(name, List.of("Alice"));

        // Act
        Throwable thrown = catchThrowable(() -> teamService.createTeam(variant.apply(name), List.of("Bob")));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Team name already exists");
    }

    static Stream<Arguments> duplicateNameVariants() {
        return Stream.of(
            Arguments.of(Named.of("same case", (UnaryOperator<String>) name -> name)),
            Arguments.of(Named.of("other case", (UnaryOperator<String>) String::toUpperCase)),
            Arguments.of(Named.of("surrounding spaces", (UnaryOperator<String>) name -> "  " + name + "  ")));
    }

    @Test
    void creatingATeam_whenTheDatabaseRejectsTheNameAfterTheCheck_reportsADuplicate() {
        // Arrange
        TeamRepository teamRepository = mock(TeamRepository.class);
        when(teamRepository.existsByNameKey(any())).thenReturn(false);
        when(teamRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_teams_name_key"));
        TeamService racingService = new TeamService(teamRepository, mock(MemberRepository.class), validator);

        // Act
        Throwable thrown = catchThrowable(() -> racingService.createTeam(uniqueName(), List.of("Alice")));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Team name already exists");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void creatingATeam_withoutMembers_isRejected(List<String> members) {
        // Act
        Throwable thrown = catchThrowable(() -> teamService.createTeam(uniqueName(), members));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("A team must have at least one member");
    }

    @Test
    void creatingATeam_withA255CharacterName_isAccepted() {
        // Arrange
        String name = "a".repeat(240) + uniqueName().substring(0, 15);

        // Act
        Team team = teamService.createTeam(name, List.of("Alice"));

        // Assert
        assertThat(team.getName()).hasSize(255);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "Team_One", "Café"})
    void creatingATeam_withAnInvalidName_isRejected(String name) {
        // Act
        Throwable thrown = catchThrowable(() -> teamService.createTeam(name, List.of("Alice")));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessageStartingWith("name: ");
    }

    @Test
    void creatingATeam_withANameOver255RawCharacters_isRejected() {
        // Arrange
        String name = "  " + "a".repeat(254);

        // Act
        Throwable thrown = catchThrowable(() -> teamService.createTeam(name, List.of("Alice")));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessageStartingWith("name: ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A_B"})
    void creatingATeam_withAnInvalidMemberName_isRejected(String memberName) {
        // Act
        Throwable thrown = catchThrowable(() -> teamService.createTeam(uniqueName(), List.of(memberName)));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessageContaining("members[0].name: ");
    }

    @Test
    void gettingAMissingTeam_isNotFound() {
        // Act
        Throwable thrown = catchThrowable(() -> teamService.getTeam(UUID.randomUUID()));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Team not found");
    }

    @Test
    void listingTeams_returnsThemOldestFirst() {
        // Arrange
        Team first = teamService.createTeam(uniqueName(), List.of("Alice"));
        Team second = teamService.createTeam(uniqueName(), List.of("Bob"));

        // Act
        List<Team> teams = teamService.listTeams();

        // Assert
        List<UUID> ids = teams.stream().map(Team::getId).toList();
        assertThat(ids).contains(first.getId(), second.getId());
        assertThat(ids.indexOf(first.getId())).isLessThan(ids.indexOf(second.getId()));
    }

    @Test
    void addingAMember_addsItToTheTeam_evenWithADuplicateName() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice"));

        // Act
        Member added = teamService.addMember(team.getId(), "Alice");

        // Assert
        Team stored = teamService.getTeam(team.getId());
        assertThat(added.getId()).isNotNull();
        assertThat(stored.getMembers()).extracting(Member::getId).contains(added.getId());
        assertThat(stored.getMembers()).hasSize(2);
    }

    @Test
    void addingAMember_withAnInvalidName_isRejected() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice"));

        // Act
        Throwable thrown = catchThrowable(() -> teamService.addMember(team.getId(), "Bob_1"));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessageStartingWith("name: ");
    }

    @Test
    void addingAMember_toAMissingTeam_isNotFound() {
        // Act
        Throwable thrown = catchThrowable(() -> teamService.addMember(UUID.randomUUID(), "Bob"));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Team not found");
    }

    @Test
    void renamingAMember_changesItsName() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice"));
        UUID aliceId = team.getMembers().get(0).getId();

        // Act
        Member renamed = teamService.renameMember(team.getId(), aliceId, "Alicia");

        // Assert
        assertThat(renamed.getName()).isEqualTo("Alicia");
        assertThat(teamService.findMember(aliceId).getName()).isEqualTo("Alicia");
    }

    @Test
    void renamingAMember_toAnInvalidName_isRejected() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice"));
        UUID aliceId = team.getMembers().get(0).getId();

        // Act
        Throwable thrown = catchThrowable(() -> teamService.renameMember(team.getId(), aliceId, "  "));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessageStartingWith("name: ");
    }

    @Test
    void renamingAMemberOfAnotherTeam_isNotFound() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice"));
        Team other = teamService.createTeam(uniqueName(), List.of("Carol"));
        UUID carolId = other.getMembers().get(0).getId();

        // Act
        Throwable thrown = catchThrowable(() -> teamService.renameMember(team.getId(), carolId, "Caroline"));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Member not found");
    }

    @Test
    void renamingAMember_ofAMissingTeam_isNotFound() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice"));
        UUID aliceId = team.getMembers().get(0).getId();

        // Act
        Throwable thrown = catchThrowable(() -> teamService.renameMember(UUID.randomUUID(), aliceId, "Alicia"));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Team not found");
    }

    @Test
    void removingAMember_marksItInactiveAndKeepsTheRow() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice", "Bob"));
        UUID bobId = team.getMembers().get(1).getId();

        // Act
        teamService.removeMember(team.getId(), bobId);

        // Assert
        assertThat(teamService.findMember(bobId).isActive()).isFalse();
        assertThat(teamService.getTeam(team.getId()).getMembers()).extracting(Member::getName)
            .containsExactlyInAnyOrder("Alice", "Bob");
    }

    @Test
    void removingTheLastActiveMember_isRejected() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice", "Bob"));
        UUID aliceId = team.getMembers().get(0).getId();
        UUID bobId = team.getMembers().get(1).getId();
        teamService.removeMember(team.getId(), aliceId);

        // Act
        Throwable thrown = catchThrowable(() -> teamService.removeMember(team.getId(), bobId));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("A team must have at least one member");
        assertThat(teamService.findMember(bobId).isActive()).isTrue();
    }

    @Test
    void renamingOrRemovingAnInactiveMember_isNotFound() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice", "Bob"));
        UUID bobId = team.getMembers().get(1).getId();
        teamService.removeMember(team.getId(), bobId);

        // Act
        Throwable renaming = catchThrowable(() -> teamService.renameMember(team.getId(), bobId, "Robert"));
        Throwable removing = catchThrowable(() -> teamService.removeMember(team.getId(), bobId));

        // Assert
        assertThat(renaming).isInstanceOf(ResourceNotFoundException.class).hasMessage("Member not found");
        assertThat(removing).isInstanceOf(ResourceNotFoundException.class).hasMessage("Member not found");
    }

    @Test
    void removingAMemberOfAnotherTeam_isNotFound() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice"));
        Team other = teamService.createTeam(uniqueName(), List.of("Carol", "Dave"));
        UUID daveId = other.getMembers().get(1).getId();

        // Act
        Throwable thrown = catchThrowable(() -> teamService.removeMember(team.getId(), daveId));

        // Assert
        assertThat(thrown).isInstanceOf(ResourceNotFoundException.class).hasMessage("Member not found");
        assertThat(teamService.findMember(daveId).isActive()).isTrue();
    }

    @Test
    void findingAMember_withoutAnIdOrWithAnUnknownId_isRejected() {
        // Act
        Throwable withoutId = catchThrowable(() -> teamService.findMember(null));
        Throwable unknownId = catchThrowable(() -> teamService.findMember(UUID.randomUUID()));

        // Assert
        assertThat(withoutId).isInstanceOf(RetroflowException.class).hasMessage("Member ID is required");
        assertThat(unknownId).isInstanceOf(ResourceNotFoundException.class).hasMessage("Member not found");
    }

    @Test
    void anActiveMemberOfTheTeam_passesTheMembershipCheck() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice"));
        Member alice = teamService.findMember(team.getMembers().get(0).getId());

        // Act
        Member checked = teamService.requireMemberOf(team.getId(), alice);

        // Assert
        assertThat(checked.getId()).isEqualTo(alice.getId());
    }

    @Test
    void anInactiveMember_isNotPartOfTheTeam() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice", "Bob"));
        UUID bobId = team.getMembers().get(1).getId();
        teamService.removeMember(team.getId(), bobId);
        Member bob = teamService.findMember(bobId);

        // Act
        Throwable thrown = catchThrowable(() -> teamService.requireMemberOf(team.getId(), bob));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Member is not part of this team");
    }

    @Test
    void aMemberOfAnotherTeam_isNotPartOfTheTeam() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice"));
        Team other = teamService.createTeam(uniqueName(), List.of("Carol"));
        Member carol = teamService.findMember(other.getMembers().get(0).getId());

        // Act
        Throwable thrown = catchThrowable(() -> teamService.requireMemberOf(team.getId(), carol));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Member is not part of this team");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Alice", "  aLiCe  "})
    void resolvingAMemberByName_matchesTrimmedAndCaseInsensitively(String name) {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice", "Bob"));
        UUID aliceId = team.getMembers().get(0).getId();

        // Act
        Member resolved = teamService.resolveMemberByName(team.getId(), name);

        // Assert
        assertThat(resolved.getId()).isEqualTo(aliceId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Zed", "   "})
    @NullSource
    void resolvingAMemberByName_withNoMatch_isRejected(String name) {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Alice", "Bob"));

        // Act
        Throwable thrown = catchThrowable(() -> teamService.resolveMemberByName(team.getId(), name));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Member is not part of this team");
    }

    @Test
    void resolvingAMemberByName_withTwoMatches_isAmbiguous() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Sam", "sam"));

        // Act
        Throwable thrown = catchThrowable(() -> teamService.resolveMemberByName(team.getId(), "SAM"));

        // Assert
        assertThat(thrown).isInstanceOf(RetroflowException.class).hasMessage("Member name is ambiguous");
    }

    @Test
    void resolvingAMemberByName_ignoresInactiveMembers() {
        // Arrange
        Team team = teamService.createTeam(uniqueName(), List.of("Sam", "sam"));
        UUID firstId = team.getMembers().get(0).getId();
        UUID secondId = team.getMembers().get(1).getId();
        teamService.removeMember(team.getId(), firstId);

        // Act
        Member resolved = teamService.resolveMemberByName(team.getId(), "sam");

        // Assert
        assertThat(resolved.getId()).isEqualTo(secondId);
    }

    private static String uniqueName() {
        return "T" + UUID.randomUUID().toString().replace("-", "");
    }
}
