package com.sqljudge.submissionservice.service;

import com.sqljudge.submissionservice.model.message.JudgeTaskMessage;
import com.sqljudge.submissionservice.service.impl.MessagePublisherServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessagePublisherServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private MessagePublisherServiceImpl messagePublisherService;

    private static final String EXCHANGE = "judge-exchange";
    private static final String ROUTING_KEY = "judge.task";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(messagePublisherService, "exchange", EXCHANGE);
        ReflectionTestUtils.setField(messagePublisherService, "routingKey", ROUTING_KEY);
    }

    @Test
    void publishJudgeTask_Success() {
        JudgeTaskMessage message = JudgeTaskMessage.builder()
                .messageId("test-uuid")
                .submissionId(1L)
                .problemId(100L)
                .sqlContent("SELECT * FROM users")
                .studentId(1001L)
                .timeLimit(30)
                .maxMemory(1024)
                .retryCount(0)
                .timestamp("2024-01-15T10:30:00")
                .build();

        messagePublisherService.publishJudgeTask(message);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), eq(message));
    }

    @Test
    void publishJudgeTask_WithDifferentMessage() {
        JudgeTaskMessage message = JudgeTaskMessage.builder()
                .messageId("another-uuid")
                .submissionId(2L)
                .problemId(200L)
                .sqlContent("UPDATE users SET name = 'test'")
                .studentId(1002L)
                .timeLimit(60)
                .maxMemory(2048)
                .retryCount(1)
                .timestamp("2024-01-16T11:00:00")
                .build();

        messagePublisherService.publishJudgeTask(message);

        verify(rabbitTemplate).convertAndSend(eq(EXCHANGE), eq(ROUTING_KEY), eq(message));
    }
}