package com.sqljudge.judgeservice.service;

import com.sqljudge.judgeservice.model.client.ContainerInfo;
import com.sqljudge.judgeservice.model.dto.JudgeResult;
import com.sqljudge.judgeservice.model.message.JudgeTaskMessage;

public interface JudgeService {
    JudgeResult judge(JudgeTaskMessage task);
    void processTask(JudgeTaskMessage task);
}
