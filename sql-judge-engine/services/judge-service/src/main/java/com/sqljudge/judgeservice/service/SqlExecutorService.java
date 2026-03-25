package com.sqljudge.judgeservice.service;

import com.sqljudge.judgeservice.model.client.ContainerInfo;
import com.sqljudge.judgeservice.model.client.SqlExecutionResult;

public interface SqlExecutorService {
    SqlExecutionResult executeInitSql(ContainerInfo container, Long problemId, String initSql, Integer timeout);
    SqlExecutionResult executeStudentSql(ContainerInfo container, Long problemId, String studentSql, Integer timeout);
}