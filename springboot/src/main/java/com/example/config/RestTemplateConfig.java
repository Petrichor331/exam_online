package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate配置
 * 用于调用Python AI评分服务
 */
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 设置连接超时时间为10秒
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        // 设置读取超时时间为60秒（AI评分可能需要较长时间）
        factory.setReadTimeout((int) Duration.ofSeconds(60).toMillis());
        
        return new RestTemplate(factory);
    }
}
