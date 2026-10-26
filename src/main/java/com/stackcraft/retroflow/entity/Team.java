package com.stackcraft.retroflow.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Entity
@Table(name = "teams", uniqueConstraints = {
    @UniqueConstraint(name = "uk_teams_name_key", columnNames = "name_key")
})
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Pattern(regexp = "^[A-Za-z0-9]+(?: +[A-Za-z0-9]+)*$")
    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "name_key", nullable = false, length = 255)
    private String nameKey;

    @NotEmpty
    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY,
               cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<@NotNull @Valid Member> members = new ArrayList<>();

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY,
               cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<Retrospective> retrospectives = new ArrayList<>();

    public Team() {
    }

    public Team(UUID id, String name, String nameKey, List<Member> members,
                List<Retrospective> retrospectives) {
        this.id = id;
        this.name = name;
        this.nameKey = nameKey;
        setMembers(members);
        setRetrospectives(retrospectives);
    }

    @AssertTrue(message = "A team must have at least one active member")
    public boolean isHasActiveMember() {
        return members != null && members.stream().anyMatch(Member::isActive);
    }

    @PrePersist
    @PreUpdate
    private void updateNameKey() {
        if (name != null) {
            nameKey = name.strip().toLowerCase(Locale.ROOT);
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameKey() {
        return nameKey;
    }

    public void setNameKey(String nameKey) {
        this.nameKey = nameKey;
    }

    public List<Member> getMembers() {
        return members;
    }

    public void setMembers(List<Member> members) {
        this.members = members == null ? new ArrayList<>() : new ArrayList<>(members);
        this.members.stream().filter(member -> member != null).forEach(member -> member.setTeam(this));
    }

    public List<Retrospective> getRetrospectives() {
        return retrospectives;
    }

    public void setRetrospectives(List<Retrospective> retrospectives) {
        this.retrospectives = retrospectives == null ? new ArrayList<>() : new ArrayList<>(retrospectives);
        this.retrospectives.stream()
            .filter(retrospective -> retrospective != null)
            .forEach(retrospective -> retrospective.setTeam(this));
    }
}
