package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Member;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.MemberRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import jakarta.validation.Validator;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TeamService {

    private static final String NOT_A_MEMBER = "Member is not part of this team";

    private final TeamRepository teamRepository;
    private final MemberRepository memberRepository;
    private final Validator validator;

    public TeamService(TeamRepository teamRepository, MemberRepository memberRepository, Validator validator) {
        this.teamRepository = teamRepository;
        this.memberRepository = memberRepository;
        this.validator = validator;
    }

    public Team createTeam(String name, List<String> memberNames) {
        List<Member> members = new ArrayList<>();
        if (memberNames != null) {
            memberNames.forEach(memberName -> members.add(new Member(null, memberName, true, null)));
        }
        if (members.isEmpty()) {
            throw new RetroflowException("A team must have at least one member");
        }
        Team team = new Team(null, name, Team.nameKeyOf(name), members, null);
        EntityValidation.require(validator, team);
        if (teamRepository.existsByNameKey(team.getNameKey())) {
            throw new RetroflowException("Team name already exists");
        }
        try {
            return teamRepository.saveAndFlush(team);
        } catch (DataIntegrityViolationException exception) {
            throw new RetroflowException("Team name already exists");
        }
    }

    @Transactional(readOnly = true)
    public Team getTeam(UUID id) {
        return teamRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Team not found"));
    }

    @Transactional(readOnly = true)
    public List<Team> listTeams() {
        return teamRepository.findAllByOrderByCreatedAtAscIdAsc();
    }

    public Member addMember(UUID teamId, String name) {
        Team team = getTeam(teamId);
        Member member = new Member(null, name, true, team);
        EntityValidation.require(validator, member);
        team.getMembers().add(member);
        return memberRepository.save(member);
    }

    public Member renameMember(UUID teamId, UUID memberId, String name) {
        Member member = findActiveMemberOfTeam(teamId, memberId);
        member.setName(name);
        EntityValidation.require(validator, member);
        return memberRepository.save(member);
    }

    public void removeMember(UUID teamId, UUID memberId) {
        Member member = findActiveMemberOfTeam(teamId, memberId);
        if (memberRepository.findByTeamIdAndActiveTrue(teamId).size() <= 1) {
            throw new RetroflowException("A team must have at least one member");
        }
        member.setActive(false);
        memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public Member findMember(UUID memberId) {
        if (memberId == null) {
            throw new RetroflowException("Member ID is required");
        }
        return memberRepository.findById(memberId)
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }

    public Member requireMemberOf(UUID teamId, Member member) {
        if (!member.isActive() || !member.getTeam().getId().equals(teamId)) {
            throw new RetroflowException(NOT_A_MEMBER);
        }
        return member;
    }

    @Transactional(readOnly = true)
    public Member resolveMemberByName(UUID teamId, String name) {
        String wanted = name == null ? "" : name.strip();
        List<Member> matches = memberRepository.findByTeamIdAndActiveTrue(teamId).stream()
            .filter(member -> member.getName().strip().equalsIgnoreCase(wanted))
            .toList();
        if (matches.isEmpty()) {
            throw new RetroflowException(NOT_A_MEMBER);
        }
        if (matches.size() > 1) {
            throw new RetroflowException("Member name is ambiguous");
        }
        return matches.get(0);
    }

    private Member findActiveMemberOfTeam(UUID teamId, UUID memberId) {
        if (!teamRepository.existsById(teamId)) {
            throw new ResourceNotFoundException("Team not found");
        }
        return memberRepository.findById(memberId)
            .filter(member -> member.isActive() && member.getTeam().getId().equals(teamId))
            .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
    }
}