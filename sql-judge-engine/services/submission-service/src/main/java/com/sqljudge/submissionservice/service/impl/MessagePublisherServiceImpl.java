package com.sqljudge.submissionservice.service.impl;

import com.sqljudge.submissionservice.model.message.JudgeTaskMessage;
import com.sqljudge.submissionservice.service.MessagePublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MessagePublisherServiceImpl implements MessagePublisherService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.judge.exchange}")
    private String exchange;

    @Value("${app.judge.routing-key}")
    private String routingKey;

    @Override
    public void publishJudgeTask(JudgeTaskMessage message) {
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
    }
}
