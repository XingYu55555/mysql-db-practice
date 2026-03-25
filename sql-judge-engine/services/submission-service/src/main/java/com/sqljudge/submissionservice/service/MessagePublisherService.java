package com.sqljudge.submissionservice.service;

import com.sqljudge.submissionservice.model.message.JudgeTaskMessage;

public interface MessagePublisherService {
    void publishJudgeTask(JudgeTaskMessage message);
}
