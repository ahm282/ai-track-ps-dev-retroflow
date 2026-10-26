package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateFeedbackRequest;
import com.stackcraft.retroflow.dto.FeedbackResponse;
import com.stackcraft.retroflow.dto.UpdateFeedbackRequest;
import com.stackcraft.retroflow.service.FeedbackService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/retrospectives/{retrospectiveId}/feedback")
    public ResponseEntity<FeedbackResponse> createFeedback(
            @PathVariable UUID retrospectiveId, @Valid @RequestBody CreateFeedbackRequest request) {
        FeedbackResponse feedback = FeedbackMapper.toResponse(feedbackService.createFeedback(retrospectiveId,
            request.content(), request.type(), request.submitterId(), request.priority(), request.completed()));
        return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
    }

    @GetMapping("/retrospectives/{retrospectiveId}/feedback")
    public List<FeedbackResponse> listFeedback(@PathVariable UUID retrospectiveId) {
        return feedbackService.listFeedback(retrospectiveId).stream().map(FeedbackMapper::toResponse).toList();
    }

    @PutMapping("/feedback/{id}")
    public FeedbackResponse updateFeedback(@PathVariable UUID id, @Valid @RequestBody UpdateFeedbackRequest request) {
        return FeedbackMapper.toResponse(feedbackService.updateFeedback(
            id, request.content(), request.priority(), request.completed()));
    }

    @DeleteMapping("/feedback/{id}")
    public ResponseEntity<Void> deleteFeedback(@PathVariable UUID id) {
        feedbackService.deleteFeedback(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/action-items")
    public List<FeedbackResponse> listActionItems(
            @RequestParam(required = false) String priority, @RequestParam(required = false) String completed) {
        return feedbackService.listActionItems(priority, completed).stream().map(FeedbackMapper::toResponse).toList();
    }

    @PutMapping("/action-items/{id}/complete")
    public FeedbackResponse completeActionItem(@PathVariable UUID id) {
        return FeedbackMapper.toResponse(feedbackService.completeActionItem(id));
    }
}
