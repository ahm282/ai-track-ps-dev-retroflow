package com.stackcraft.retroflow.entity;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feedback_items")
public class FeedbackItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 4000)
    @Column(nullable = false, length = 4000)
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 32, updatable = false)
    private FeedbackType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Priority priority;

    private Boolean completed;

    @NotNull
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitter_id", nullable = false, updatable = false)
    private Member submitter;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retrospective_id", nullable = false)
    private Retrospective retrospective;

    public FeedbackItem() {
    }

    public FeedbackItem(UUID id, String content, FeedbackType type, Priority priority,
                        Boolean completed, Instant createdAt, Member submitter,
                        Retrospective retrospective) {
        this.id = id;
        this.content = content;
        this.type = type;
        this.priority = priority;
        this.completed = completed;
        this.createdAt = createdAt;
        this.submitter = submitter;
        this.retrospective = retrospective;
        applyActionItemDefaults();
    }

    @PrePersist
    private void applyDefaults() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        applyActionItemDefaults();
    }

    private void applyActionItemDefaults() {
        if (type == FeedbackType.ACTION_ITEM && completed == null) {
            completed = false;
        }
    }

    @AssertTrue(message = "Action items require priority and completion state; other feedback must not have them")
    public boolean isActionFieldsValid() {
        if (type == null) {
            return true;
        }
        if (type == FeedbackType.ACTION_ITEM) {
            return priority != null && completed != null;
        }
        return priority == null && completed == null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public FeedbackType getType() {
        return type;
    }

    public void setType(FeedbackType type) {
        this.type = type;
        applyActionItemDefaults();
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public Member getSubmitter() {
        return submitter;
    }

    public void setSubmitter(Member submitter) {
        this.submitter = submitter;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Retrospective getRetrospective() {
        return retrospective;
    }

    public void setRetrospective(Retrospective retrospective) {
        this.retrospective = retrospective;
    }
}
