package com.sqljudge.judgeservice.service;

import com.sqljudge.judgeservice.model.client.ProblemDetail;

public interface ProblemClientService {
    ProblemDetail getProblemDetail(Long problemId);
}