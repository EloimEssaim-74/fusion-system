package com.kb.article.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String TOPIC_EXCHANGE = "article.topic.exchange";
    public static final String TAG_QUEUE = "article.tag.queue";
    public static final String COMPLIANCE_QUEUE = "article.compliance.queue";
    public static final String TAG_ROUTING_KEY = "article.tag";
    public static final String COMPLIANCE_ROUTING_KEY = "article.compliance";
    public static final String PUBLISH_ROUTING_KEY = "article.publish";

    @Bean
    public TopicExchange articleTopicExchange() {
        return new TopicExchange(TOPIC_EXCHANGE, true, false);
    }

    @Bean
    public Queue tagQueue() {
        return QueueBuilder.durable(TAG_QUEUE).build();
    }

    @Bean
    public Queue complianceQueue() {
        return QueueBuilder.durable(COMPLIANCE_QUEUE).build();
    }

    @Bean
    public Binding tagBinding() {
        return BindingBuilder.bind(tagQueue())
                .to(articleTopicExchange())
                .with(TAG_ROUTING_KEY);
    }

    @Bean
    public Binding complianceBinding() {
        return BindingBuilder.bind(complianceQueue())
                .to(articleTopicExchange())
                .with(COMPLIANCE_ROUTING_KEY);
    }

    @Bean
    public Binding publishBinding() {
        return BindingBuilder.bind(tagQueue())
                .to(articleTopicExchange())
                .with(PUBLISH_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
