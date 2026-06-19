package com.spotz.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class AppConfig {
    private final EntityManager em;

    @Bean
    public JPAQueryFactory jpaQueryFactory() { return new JPAQueryFactory(em); }

    @Bean
    public RestTemplate restTemplate() { return new RestTemplate(); }
}
