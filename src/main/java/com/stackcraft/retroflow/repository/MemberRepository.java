package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {

    List<Member> findByTeamIdAndActiveTrue(UUID teamId);
}
