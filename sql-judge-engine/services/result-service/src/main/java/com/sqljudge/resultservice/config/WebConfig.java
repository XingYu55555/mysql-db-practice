package com.sqljudge.resultservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.api-key}")
    private String apiKey;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new ApiKeyInterceptor(apiKey))
                .addPathPatterns("/api/result")
                .excludePathPatterns("/api/result/submission/**")
                .excludePathPatterns("/api/result/student/**")
                .excludePathPatterns("/api/result/problem/**/leaderboard")
                .excludePathPatterns("/api/result/leaderboard")
                .excludePathPatterns("/api/result/statistics");
    }
}