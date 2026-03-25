package com.sqljudge.judgeservice.consumer;

import com.rabbitmq.client.Channel;
import com.sqljudge.judgeservice.model.message.JudgeTaskMessage;
import com.sqljudge.judgeservice.service.JudgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JudgeTaskConsumer {

    private final JudgeService judgeService;

    @RabbitListener(queues = "${app.judge.queue}")
    public void handleJudgeTask(JudgeTaskMessage task, Message message, Channel channel) {
        log.info("Received judge task: messageId={}, submissionId={}",
                task.getMessageId(), task.getSubmissionId());

        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        try {
            judgeService.processTask(task);
            channel.basicAck(deliveryTag, false);
            log.info("Completed judge task: submissionId={}", task.getSubmissionId());

        } catch (Exception e) {
            log.error("Failed to process judge task: submissionId={}", task.getSubmissionId(), e);
            try {
                channel.basicNack(deliveryTag, false, false);
            } catch (IOException ioe) {
                log.error("Failed to nack message: {}", ioe.getMessage(), ioe);
            }
        }
    }
}