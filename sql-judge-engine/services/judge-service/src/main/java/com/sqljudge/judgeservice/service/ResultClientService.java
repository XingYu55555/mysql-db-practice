package com.sqljudge.judgeservice.service;

import com.sqljudge.judgeservice.model.dto.JudgeResult;

public interface ResultClientService {
    void submitResult(JudgeResult result);
}
