package com.sqljudge.judgeservice.service;

import com.sqljudge.judgeservice.model.client.AiJudgeResult;
import com.sqljudge.judgeservice.model.client.ContainerInfo;
import com.sqljudge.judgeservice.model.client.ProblemDetail;
import com.sqljudge.judgeservice.model.client.SqlExecutionResult;
import com.sqljudge.judgeservice.model.dto.JudgeResult;
import com.sqljudge.judgeservice.model.message.JudgeTaskMessage;
import com.sqljudge.judgeservice.service.impl.JudgeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JudgeServiceTest {

    @Mock
    private ContainerClientService containerClientService;

    @Mock
    private ResultClientService resultClientService;

    @Mock
    private ProblemClientService problemClientService;

    @Mock
    private SqlExecutorService sqlExecutorService;

    @Mock
    private AiJudgeService aiJudgeService;

    @InjectMocks
    private JudgeServiceImpl judgeService;

    private JudgeTaskMessage testTask;
    private ContainerInfo testContainer;
    private ProblemDetail testProblem;

    @BeforeEach
    void setUp() {
        testTask = JudgeTaskMessage.builder()
                .messageId("test-message-id")
                .submissionId(1L)
                .problemId(100L)
                .sqlContent("SELECT * FROM users")
                .studentId(1001L)
                .timeLimit(30)
                .maxMemory(1024)
                .retryCount(0)
                .build();

        testContainer = ContainerInfo.builder()
                .containerId("container-1")
                .containerName("judge-container-1")
                .ipAddress("192.168.1.100")
                .mysqlPort(3306)
                .mysqlUser("judge")
                .connectionToken("test-token")
                .status("IN_USE")
                .build();

        testProblem = ProblemDetail.builder()
                .problemId(100L)
                .title("Test Problem")
                .description("Test problem description")
                .difficulty("MEDIUM")
                .sqlType("DQL")
                .aiAssisted(false)
                .initSql("CREATE TABLE users (id INT, name VARCHAR(50));")
                .standardAnswer("SELECT * FROM users;")
                .expectedResult("")
                .build();
    }

    @Test
    void testJudge_DQL_NoExpectedResult_ReturnsCorrect() {
        testProblem.setExpectedResult(null);

        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(testContainer);
        when(sqlExecutorService.executeInitSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder().success(true).build());
        when(sqlExecutorService.executeStudentSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder()
                        .success(true)
                        .resultSet(createMockResultSet())
                        .sqlType("DQL")
                        .build());

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertEquals(1L, result.getSubmissionId());
        assertEquals("CORRECT", result.getStatus());
        assertEquals(BigDecimal.valueOf(100), result.getScore());
        verify(containerClientService).releaseContainer(eq("container-1"), eq(true));
    }

    @Test
    void testJudge_DML_Correct() {
        testProblem.setSqlType("DML");
        testProblem.setExpectedResult("1");

        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(testContainer);
        when(sqlExecutorService.executeInitSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder().success(true).build());
        when(sqlExecutorService.executeStudentSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder()
                        .success(true)
                        .affectedRows(1)
                        .sqlType("DML")
                        .build());

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertEquals("CORRECT", result.getStatus());
        assertEquals(BigDecimal.valueOf(100), result.getScore());
    }

    @Test
    void testJudge_DML_InCorrect() {
        testProblem.setSqlType("DML");
        testProblem.setExpectedResult("5");

        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(testContainer);
        when(sqlExecutorService.executeInitSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder().success(true).build());
        when(sqlExecutorService.executeStudentSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder()
                        .success(true)
                        .affectedRows(1)
                        .sqlType("DML")
                        .build());

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertEquals("INCORRECT", result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getScore());
    }

    @Test
    void testJudge_DDL_WithAiAssisted() {
        testProblem.setSqlType("DDL");
        testProblem.setAiAssisted(true);
        testTask.setSqlContent("CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(50) NOT NULL);");

        AiJudgeResult aiResult = AiJudgeResult.builder()
                .isCorrect(true)
                .reason("Semantic equivalence detected")
                .confidence(0.95)
                .success(true)
                .status("AI_APPROVED")
                .build();

        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(testContainer);
        when(sqlExecutorService.executeInitSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder().success(true).build());
        when(sqlExecutorService.executeStudentSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder().success(true).sqlType("DDL").build());
        when(aiJudgeService.judgeWithAi(any(), any(), any(), any())).thenReturn(aiResult);

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertEquals("AI_APPROVED", result.getStatus());
        assertEquals(BigDecimal.valueOf(100), result.getScore());
    }

    @Test
    void testJudge_DDL_Fallback() {
        testProblem.setSqlType("DDL");
        testProblem.setAiAssisted(false);

        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(testContainer);
        when(sqlExecutorService.executeInitSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder().success(true).build());
        when(sqlExecutorService.executeStudentSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder().success(true).sqlType("DDL").build());

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertNotNull(result.getStatus());
    }

    @Test
    void testJudge_FailedToAcquireContainer() {
        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(null);

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertEquals("ERROR", result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getScore());
        assertTrue(result.getErrorMessage().contains("Failed to acquire container"));
    }

    @Test
    void testJudge_FailedToFetchProblem() {
        when(problemClientService.getProblemDetail(100L)).thenThrow(new RuntimeException("Problem service unavailable"));

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertEquals("ERROR", result.getStatus());
        assertEquals(BigDecimal.ZERO, result.getScore());
    }

    @Test
    void testJudge_InitSqlFailed() {
        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(testContainer);
        when(sqlExecutorService.executeInitSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder()
                        .success(false)
                        .errorMessage("Init SQL failed")
                        .build());

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertEquals("ERROR", result.getStatus());
        assertTrue(result.getErrorMessage().contains("Init SQL execution failed"));
        verify(containerClientService).releaseContainer(eq("container-1"), eq(true));
    }

    @Test
    void testJudge_StudentSqlFailed() {
        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(testContainer);
        when(sqlExecutorService.executeInitSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder().success(true).build());
        when(sqlExecutorService.executeStudentSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder()
                        .success(false)
                        .errorMessage("Student SQL syntax error")
                        .build());

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertEquals("ERROR", result.getStatus());
        assertTrue(result.getErrorMessage().contains("Student SQL execution failed"));
        verify(containerClientService).releaseContainer(eq("container-1"), eq(true));
    }

    @Test
    void testJudge_NoInitSql() {
        testProblem.setInitSql(null);

        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(testContainer);
        when(sqlExecutorService.executeStudentSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder()
                        .success(true)
                        .resultSet(createMockResultSet())
                        .sqlType("DQL")
                        .build());

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertEquals("CORRECT", result.getStatus());
        verify(sqlExecutorService, never()).executeInitSql(any(), any(), any(), anyInt());
    }

    @Test
    void testJudge_DCL_WithAiAssisted() {
        testProblem.setSqlType("DCL");
        testProblem.setAiAssisted(true);
        testTask.setSqlContent("GRANT SELECT ON users TO 'student'@'%';");

        AiJudgeResult aiResult = AiJudgeResult.builder()
                .isCorrect(true)
                .reason("DCL semantic equivalence detected")
                .confidence(0.92)
                .success(true)
                .status("AI_APPROVED")
                .build();

        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(testContainer);
        when(sqlExecutorService.executeInitSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder().success(true).build());
        when(sqlExecutorService.executeStudentSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder().success(true).sqlType("DCL").build());
        when(aiJudgeService.judgeWithAi(any(), any(), any(), any())).thenReturn(aiResult);

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertEquals("AI_APPROVED", result.getStatus());
        assertEquals(BigDecimal.valueOf(100), result.getScore());
    }

    @Test
    void testProcessTask() {
        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(testContainer);
        when(sqlExecutorService.executeInitSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder().success(true).build());
        when(sqlExecutorService.executeStudentSql(any(), any(), any(), anyInt()))
                .thenReturn(SqlExecutionResult.builder()
                        .success(true)
                        .resultSet(createMockResultSet())
                        .sqlType("DQL")
                        .build());

        judgeService.processTask(testTask);

        verify(resultClientService).submitResult(any(JudgeResult.class));
    }

    @Test
    void testJudge_ContainerReleaseOnException() {
        when(problemClientService.getProblemDetail(100L)).thenReturn(testProblem);
        when(containerClientService.acquireContainer(eq(100L), anyInt())).thenReturn(testContainer);
        when(sqlExecutorService.executeInitSql(any(), any(), any(), anyInt()))
                .thenThrow(new RuntimeException("Database connection lost"));

        JudgeResult result = judgeService.judge(testTask);

        assertNotNull(result);
        assertEquals("ERROR", result.getStatus());
        verify(containerClientService).releaseContainer(eq("container-1"), eq(true));
    }

    private List<Map<String, Object>> createMockResultSet() {
        List<Map<String, Object>> results = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        row.put("name", "John");
        results.add(row);
        return results;
    }
}