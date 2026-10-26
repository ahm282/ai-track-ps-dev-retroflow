package com.stackcraft.retroflow.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "retrospectives")
public class Retrospective {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255, updatable = false)
    private String title;

    @NotNull
    @Column(nullable = false, updatable = false)
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RetrospectiveStatus status;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @OneToMany(mappedBy = "retrospective", fetch = FetchType.LAZY,
               cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<@Valid FeedbackItem> feedbackItems = new ArrayList<>();

    public Retrospective() {
    }

    public Retrospective(UUID id, String title, LocalDate date, RetrospectiveStatus status,
                         Team team, List<FeedbackItem> feedbackItems) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.status = status;
        this.team = team;
        setFeedbackItems(feedbackItems);
    }

    @PrePersist
    private void applyDefaults() {
        if (date == null) {
            date = LocalDate.now();
        }
        if (status == null) {
            status = RetrospectiveStatus.OPEN;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public RetrospectiveStatus getStatus() {
        return status;
    }

    public void setStatus(RetrospectiveStatus status) {
        this.status = status;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
    }

    public List<FeedbackItem> getFeedbackItems() {
        return feedbackItems;
    }

    public void setFeedbackItems(List<FeedbackItem> feedbackItems) {
        this.feedbackItems = feedbackItems == null ? new ArrayList<>() : new ArrayList<>(feedbackItems);
        this.feedbackItems.stream()
            .filter(feedbackItem -> feedbackItem != null)
            .forEach(feedbackItem -> feedbackItem.setRetrospective(this));
    }
}
