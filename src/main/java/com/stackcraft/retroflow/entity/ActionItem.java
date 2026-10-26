package com.stackcraft.retroflow.entity;

import java.util.UUID;

/**
 * Action-item view of a persisted FeedbackItem; action items are not a
 * separate JPA entity.
 */
public class ActionItem {

    private UUID id;
    private String content;
    private Priority priority;
    private Boolean completed;

    public ActionItem() {
    }

    public ActionItem(UUID id, String content, Priority priority, Boolean completed) {
        this.id = id;
        this.content = content;
        this.priority = priority;
        this.completed = completed;
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

    public static ActionItem from(FeedbackItem feedbackItem) {
        if (feedbackItem == null || feedbackItem.getType() != FeedbackType.ACTION_ITEM) {
            throw new IllegalArgumentException("Feedback item must have type ACTION_ITEM");
        }
        return new ActionItem(
            feedbackItem.getId(),
            feedbackItem.getContent(),
            feedbackItem.getPriority(),
            feedbackItem.getCompleted()
        );
    }
}
