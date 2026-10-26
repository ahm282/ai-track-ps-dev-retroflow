package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class RetrospectiveService {

    public Retrospective createRetrospective(UUID teamId, String title, UUID memberId,
                                              RetrospectiveStatus status) {
        throw new UnsupportedOperationException("Retrospective creation is not implemented yet");
    }

    public Retrospective createRetrospective(UUID teamId, String title) {
        throw new UnsupportedOperationException("Retrospective creation is not implemented yet");
    }

    public List<Retrospective> listRetrospectives(UUID teamId) {
        throw new UnsupportedOperationException("Retrospective listing is not implemented yet");
    }

    public Retrospective closeRetrospective(UUID id, UUID memberId) {
        throw new UnsupportedOperationException("Retrospective closing is not implemented yet");
    }

    public Retrospective closeRetrospective(UUID id) {
        throw new UnsupportedOperationException("Retrospective closing is not implemented yet");
    }

    public FeedbackItem addFeedbackItem(UUID retrospectiveId, String content, String type, String submitterName) {
        throw new UnsupportedOperationException("Feedback creation is not implemented yet");
    }

    public ActionItem addActionItem(UUID retrospectiveId, String content, String priority, String submitterName) {
        throw new UnsupportedOperationException("Action item creation is not implemented yet");
    }

    public ActionItem completeActionItem(UUID id) {
        throw new UnsupportedOperationException("Action item completion is not implemented yet");
    }

    public ActionItem uncompleteActionItem(UUID id) {
        throw new UnsupportedOperationException("Action item uncompletion is not implemented yet");
    }
}