package com.sqljudge.judgeservice.consumer;

import com.rabbitmq.client.Channel;
import com.sqljudge.judgeservice.model.message.JudgeTaskMessage;
import com.sqljudge.judgeservice.service.JudgeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JudgeTaskConsumerTest {

    @Mock
    private JudgeService judgeService;

    @Mock
    private Message message;

    @Mock
    private MessageProperties messageProperties;

    @Mock
    private Channel channel;

    @InjectMocks
    private JudgeTaskConsumer judgeTaskConsumer;

    private JudgeTaskMessage testTask;

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
    }

    @Test
    void testHandleJudgeTask_Success() throws Exception {
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(1L);
        doNothing().when(judgeService).processTask(testTask);

        judgeTaskConsumer.handleJudgeTask(testTask, message, channel);

        verify(judgeService).processTask(testTask);
        verify(channel).basicAck(1L, false);
        verify(channel, never()).basicNack(anyLong(), anyBoolean(), anyBoolean());
    }

    @Test
    void testHandleJudgeTask_Failure() throws Exception {
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(1L);
        doThrow(new RuntimeException("Judge service error")).when(judgeService).processTask(testTask);

        judgeTaskConsumer.handleJudgeTask(testTask, message, channel);

        verify(judgeService).processTask(testTask);
        verify(channel).basicNack(1L, false, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }

    @Test
    void testHandleJudgeTask_NackFailure() throws Exception {
        when(message.getMessageProperties()).thenReturn(messageProperties);
        when(messageProperties.getDeliveryTag()).thenReturn(1L);
        doThrow(new RuntimeException("Judge service error")).when(judgeService).processTask(testTask);
        doThrow(new java.io.IOException("Nack failed")).when(channel).basicNack(anyLong(), anyBoolean(), anyBoolean());

        judgeTaskConsumer.handleJudgeTask(testTask, message, channel);

        verify(judgeService).processTask(testTask);
        verify(channel).basicNack(1L, false, false);
    }
}