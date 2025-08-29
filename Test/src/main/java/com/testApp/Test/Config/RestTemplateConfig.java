package com.testApp.Test.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate(
            @Value("${http.connect.timeout:8000}") int connectTimeout,
            @Value("${http.read.timeout:12000}") int readTimeout) {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(connectTimeout);
        f.setReadTimeout(readTimeout);
        return new RestTemplate(f);
    }
}

