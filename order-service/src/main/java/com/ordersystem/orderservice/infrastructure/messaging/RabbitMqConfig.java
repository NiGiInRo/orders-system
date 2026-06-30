package com.ordersystem.orderservice.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // TopicExchange permite routing por patrones — order.* cubre cualquier evento de órdenes
    @Bean
    public TopicExchange ordersExchange(@Value("${app.rabbitmq.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName);
    }

    // Serializa los eventos a JSON — cualquier servicio en cualquier lenguaje puede consumirlos
    @Bean
    public MessageConverter jackson2JsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}