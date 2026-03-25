package com.sqljudge.judgeservice.service;

import com.sqljudge.judgeservice.model.client.AiJudgeResult;

public interface AiJudgeService {
    AiJudgeResult judgeWithAi(String problemDescription, String standardAnswer, String studentAnswer, String sqlType);
}