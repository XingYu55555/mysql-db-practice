package com.sqljudge.judgeservice.service.impl;

import com.sqljudge.judgeservice.model.client.AiJudgeResult;
import com.sqljudge.judgeservice.model.client.ContainerInfo;
import com.sqljudge.judgeservice.model.client.ProblemDetail;
import com.sqljudge.judgeservice.model.client.SqlExecutionResult;
import com.sqljudge.judgeservice.model.dto.JudgeResult;
import com.sqljudge.judgeservice.model.message.JudgeTaskMessage;
import com.sqljudge.judgeservice.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class JudgeServiceImpl implements JudgeService {

    private final ContainerClientService containerClientService;
    private final ResultClientService resultClientService;
    private final ProblemClientService problemClientService;
    private final SqlExecutorService sqlExecutorService;
    private final AiJudgeService aiJudgeService;

    private static final int DEFAULT_TIMEOUT = 30000;
    private static final int MAX_RETRIES = 2;

    @Override
    public JudgeResult judge(JudgeTaskMessage task) {
        log.info("Starting judgment for submission: {}", task.getSubmissionId());
        long startTime = System.currentTimeMillis();

        ContainerInfo container = null;
        try {
            ProblemDetail problem = fetchProblemWithRetry(task.getProblemId());
            if (problem == null) {
                return buildErrorResult(task, "Failed to fetch problem details", startTime);
            }

            container = acquireContainerWithRetry(task.getProblemId(), task.getTimeLimit() != null ? task.getTimeLimit() * 1000 : DEFAULT_TIMEOUT);
            if (container == null) {
                return buildErrorResult(task, "Failed to acquire container", startTime);
            }
            log.info("Acquired container: {} for problem: {}", container.getContainerId(), task.getProblemId());

            SqlExecutionResult initResult = executeInitSqlWithRetry(container, task.getProblemId(), problem.getInitSql(), task.getTimeLimit());
            if (!initResult.isSuccess()) {
                log.error("Init SQL execution failed: {}", initResult.getErrorMessage());
                return buildErrorResult(task, "Init SQL execution failed: " + initResult.getErrorMessage(), startTime);
            }

            SqlExecutionResult studentResult = executeStudentSqlWithRetry(container, task.getProblemId(), task.getSqlContent(), task.getTimeLimit());
            if (!studentResult.isSuccess()) {
                return buildErrorResult(task, "Student SQL execution failed: " + studentResult.getErrorMessage(), startTime);
            }

            return compareResults(task, problem, studentResult, startTime);

        } catch (Exception e) {
            log.error("Judge error for submission: {}", task.getSubmissionId(), e);
            return buildErrorResult(task, "Judge error: " + e.getMessage(), startTime);
        } finally {
            releaseContainer(container);
        }
    }

    @Override
    public void processTask(JudgeTaskMessage task) {
        JudgeResult result = judge(task);
        resultClientService.submitResult(result);
    }

    private ProblemDetail fetchProblemWithRetry(Long problemId) {
        int attempts = 0;
        while (attempts < MAX_RETRIES + 1) {
            try {
                attempts++;
                return problemClientService.getProblemDetail(problemId);
            } catch (Exception e) {
                log.warn("Failed to fetch problem {}, attempt {}/{}: {}", problemId, attempts, MAX_RETRIES + 1, e.getMessage());
                if (attempts >= MAX_RETRIES + 1) {
                    return null;
                }
                try {
                    Thread.sleep(1000 * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private ContainerInfo acquireContainerWithRetry(Long problemId, Integer timeout) {
        int attempts = 0;
        while (attempts < MAX_RETRIES + 1) {
            try {
                attempts++;
                return containerClientService.acquireContainer(problemId, timeout);
            } catch (Exception e) {
                log.warn("Failed to acquire container for problem {}, attempt {}/{}: {}", problemId, attempts, MAX_RETRIES + 1, e.getMessage());
                if (attempts >= MAX_RETRIES + 1) {
                    return null;
                }
                try {
                    Thread.sleep(1000 * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private SqlExecutionResult executeInitSqlWithRetry(ContainerInfo container, Long problemId, String initSql, Integer timeLimit) {
        if (initSql == null || initSql.trim().isEmpty()) {
            log.info("No init SQL to execute for problem {}", problemId);
            return SqlExecutionResult.builder().success(true).build();
        }

        int timeout = timeLimit != null ? timeLimit * 1000 : DEFAULT_TIMEOUT;
        int attempts = 0;
        while (attempts < MAX_RETRIES + 1) {
            try {
                attempts++;
                return sqlExecutorService.executeInitSql(container, problemId, initSql, timeout);
            } catch (Exception e) {
                log.warn("Failed to execute init SQL, attempt {}/{}: {}", attempts, MAX_RETRIES + 1, e.getMessage());
                if (attempts >= MAX_RETRIES + 1) {
                    return SqlExecutionResult.builder()
                            .success(false)
                            .errorMessage("Init SQL failed after " + MAX_RETRIES + " retries: " + e.getMessage())
                            .build();
                }
                try {
                    Thread.sleep(1000 * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return SqlExecutionResult.builder()
                            .success(false)
                            .errorMessage("Init SQL execution interrupted")
                            .build();
                }
            }
        }
        return SqlExecutionResult.builder().success(false).errorMessage("Unknown error").build();
    }

    private SqlExecutionResult executeStudentSqlWithRetry(ContainerInfo container, Long problemId, String studentSql, Integer timeLimit) {
        int timeout = timeLimit != null ? timeLimit * 1000 : DEFAULT_TIMEOUT;
        int attempts = 0;
        while (attempts < MAX_RETRIES + 1) {
            try {
                attempts++;
                return sqlExecutorService.executeStudentSql(container, problemId, studentSql, timeout);
            } catch (Exception e) {
                log.warn("Failed to execute student SQL, attempt {}/{}: {}", attempts, MAX_RETRIES + 1, e.getMessage());
                if (attempts >= MAX_RETRIES + 1) {
                    return SqlExecutionResult.builder()
                            .success(false)
                            .errorMessage("Student SQL failed after " + MAX_RETRIES + " retries: " + e.getMessage())
                            .build();
                }
                try {
                    Thread.sleep(1000 * attempts);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return SqlExecutionResult.builder()
                            .success(false)
                            .errorMessage("Student SQL execution interrupted")
                            .build();
                }
            }
        }
        return SqlExecutionResult.builder().success(false).errorMessage("Unknown error").build();
    }

    private void releaseContainer(ContainerInfo container) {
        if (container != null) {
            try {
                containerClientService.releaseContainer(container.getContainerId(), true);
                log.info("Released container: {}", container.getContainerId());
            } catch (Exception e) {
                log.error("Failed to release container: {}", container.getContainerId(), e);
            }
        }
    }

    private JudgeResult compareResults(JudgeTaskMessage task, ProblemDetail problem, SqlExecutionResult studentResult, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        String sqlType = problem.getSqlType();

        if ("DQL".equalsIgnoreCase(sqlType)) {
            return compareDqlResults(task, problem, studentResult, executionTime);
        } else if ("DML".equalsIgnoreCase(sqlType)) {
            return compareDmlResults(task, problem, studentResult, executionTime);
        } else if ("DDL".equalsIgnoreCase(sqlType)) {
            return compareDdlResults(task, problem, studentResult, executionTime);
        } else if ("DCL".equalsIgnoreCase(sqlType)) {
            return compareDclResults(task, problem, studentResult, executionTime);
        }

        return buildErrorResult(task, "Unknown SQL type: " + sqlType, startTime);
    }

    private JudgeResult compareDqlResults(JudgeTaskMessage task, ProblemDetail problem, SqlExecutionResult studentResult, long executionTime) {
        List<Map<String, Object>> expectedResult = parseExpectedResult(problem.getExpectedResult());
        List<Map<String, Object>> actualResult = studentResult.getResultSet();

        if (expectedResult == null || expectedResult.isEmpty()) {
            return JudgeResult.builder()
                    .submissionId(task.getSubmissionId())
                    .score(BigDecimal.valueOf(100))
                    .status("CORRECT")
                    .executionTimeMs(executionTime)
                    .build();
        }

        boolean isCorrect = compareResultSets(expectedResult, actualResult);

        return JudgeResult.builder()
                .submissionId(task.getSubmissionId())
                .score(isCorrect ? BigDecimal.valueOf(100) : BigDecimal.ZERO)
                .status(isCorrect ? "CORRECT" : "INCORRECT")
                .executionTimeMs(executionTime)
                .build();
    }

    private JudgeResult compareDmlResults(JudgeTaskMessage task, ProblemDetail problem, SqlExecutionResult studentResult, long executionTime) {
        if (studentResult.getAffectedRows() == null || studentResult.getAffectedRows() == 0) {
            return JudgeResult.builder()
                    .submissionId(task.getSubmissionId())
                    .score(BigDecimal.valueOf(100))
                    .status("CORRECT")
                    .executionTimeMs(executionTime)
                    .build();
        }

        String expectedAffected = problem.getExpectedResult();
        int expected = expectedAffected != null ? Integer.parseInt(expectedAffected.trim()) : 0;
        int actual = studentResult.getAffectedRows();

        boolean isCorrect = expected == actual;

        return JudgeResult.builder()
                .submissionId(task.getSubmissionId())
                .score(isCorrect ? BigDecimal.valueOf(100) : BigDecimal.ZERO)
                .status(isCorrect ? "CORRECT" : "INCORRECT")
                .executionTimeMs(executionTime)
                .build();
    }

    private JudgeResult compareDdlResults(JudgeTaskMessage task, ProblemDetail problem, SqlExecutionResult studentResult, long executionTime) {
        Boolean aiAssisted = problem.getAiAssisted();
        String standardAnswer = problem.getStandardAnswer();
        String studentAnswer = task.getSqlContent();

        if (aiAssisted != null && aiAssisted) {
            return judgeWithAi(task, problem, standardAnswer, studentAnswer, "DDL", executionTime);
        }

        return ddlFallbackCompare(task, standardAnswer, studentAnswer, executionTime);
    }

    private JudgeResult compareDclResults(JudgeTaskMessage task, ProblemDetail problem, SqlExecutionResult studentResult, long executionTime) {
        Boolean aiAssisted = problem.getAiAssisted();
        String standardAnswer = problem.getStandardAnswer();
        String studentAnswer = task.getSqlContent();

        if (aiAssisted != null && aiAssisted) {
            return judgeWithAi(task, problem, standardAnswer, studentAnswer, "DCL", executionTime);
        }

        return dclFallbackCompare(task, standardAnswer, studentAnswer, executionTime);
    }

    private JudgeResult judgeWithAi(JudgeTaskMessage task, ProblemDetail problem, String standardAnswer, String studentAnswer, String sqlType, long executionTime) {
        try {
            AiJudgeResult aiResult = aiJudgeService.judgeWithAi(
                    problem.getDescription(),
                    standardAnswer,
                    studentAnswer,
                    sqlType
            );

            if (aiResult == null) {
                return buildErrorResult(task, "AI judging returned null result", executionTime);
            }

            String status = aiResult.getStatus();
            BigDecimal score = "AI_APPROVED".equals(status) || "CORRECT".equals(status)
                    ? BigDecimal.valueOf(100) : BigDecimal.ZERO;

            return JudgeResult.builder()
                    .submissionId(task.getSubmissionId())
                    .score(score)
                    .status(status)
                    .errorMessage(aiResult.getReason())
                    .executionTimeMs(executionTime)
                    .build();

        } catch (Exception e) {
            log.error("AI judging failed for submission: {}", task.getSubmissionId(), e);
            return buildErrorResult(task, "AI judging failed: " + e.getMessage(), executionTime);
        }
    }

    private JudgeResult ddlFallbackCompare(JudgeTaskMessage task, String standardAnswer, String studentAnswer, long executionTime) {
        String normalizedStandard = normalizeForComparison(standardAnswer);
        String normalizedStudent = normalizeForComparison(studentAnswer);

        boolean isCorrect = normalizedStandard.equals(normalizedStudent);

        return JudgeResult.builder()
                .submissionId(task.getSubmissionId())
                .score(isCorrect ? BigDecimal.valueOf(100) : BigDecimal.ZERO)
                .status(isCorrect ? "CORRECT" : "INCORRECT")
                .executionTimeMs(executionTime)
                .build();
    }

    private JudgeResult dclFallbackCompare(JudgeTaskMessage task, String standardAnswer, String studentAnswer, long executionTime) {
        String upperStandard = (standardAnswer != null ? standardAnswer : "").toUpperCase();
        String upperStudent = (studentAnswer != null ? studentAnswer : "").toUpperCase();

        boolean standardHasGrant = upperStandard.contains("GRANT");
        boolean standardHasRevoke = upperStandard.contains("REVOKE");
        boolean studentHasGrant = upperStudent.contains("GRANT");
        boolean studentHasRevoke = upperStudent.contains("REVOKE");

        boolean isCorrect = (standardHasGrant == studentHasGrant) && (standardHasRevoke == studentHasRevoke);

        return JudgeResult.builder()
                .submissionId(task.getSubmissionId())
                .score(isCorrect ? BigDecimal.valueOf(100) : BigDecimal.ZERO)
                .status(isCorrect ? "CORRECT" : "INCORRECT")
                .executionTimeMs(executionTime)
                .build();
    }

    private List<Map<String, Object>> parseExpectedResult(String expectedResult) {
        if (expectedResult == null || expectedResult.trim().isEmpty()) {
            return null;
        }
        return null;
    }

    private boolean compareResultSets(List<Map<String, Object>> expected, List<Map<String, Object>> actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;
        if (expected.size() != actual.size()) return false;

        for (int i = 0; i < expected.size(); i++) {
            Map<String, Object> expectedRow = expected.get(i);
            Map<String, Object> actualRow = actual.get(i);

            if (!expectedRow.equals(actualRow)) {
                return false;
            }
        }
        return true;
    }

    private String normalizeForComparison(String sql) {
        if (sql == null) return "";
        return sql.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("(?i)\\b(VARCHAR|INT|INTEGER|BIGINT|SMALLINT|TINYINT)\\b", "TYPE")
                .replaceAll("(?i)\\b(NOT NULL|NULL|AUTO_INCREMENT|PRIMARY KEY)\\b", "CONSTRAINT")
                .replaceAll("(?i)\\s+", " ")
                .toUpperCase();
    }

    private JudgeResult buildErrorResult(JudgeTaskMessage task, String errorMessage, long startTime) {
        long executionTime = System.currentTimeMillis() - startTime;
        return JudgeResult.builder()
                .submissionId(task.getSubmissionId())
                .score(BigDecimal.ZERO)
                .status("ERROR")
                .errorMessage(errorMessage)
                .executionTimeMs(executionTime)
                .build();
    }
}