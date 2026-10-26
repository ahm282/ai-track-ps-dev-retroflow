package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.FeedbackResponse;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;

final class FeedbackMapper {

    private FeedbackMapper() {
    }

    static FeedbackResponse toResponse(FeedbackItem item) {
        boolean action = item.getType() == FeedbackType.ACTION_ITEM;
        return new FeedbackResponse(item.getId(), item.getRetrospective().getId(), item.getContent(),
            item.getType(), item.getSubmitter().getId(), item.getSubmitterName(),
            action ? item.getPriority() : null, action ? item.getCompleted() : null);
    }
}
