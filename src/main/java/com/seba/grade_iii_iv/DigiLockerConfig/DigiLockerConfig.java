package com.seba.grade_iii_iv.DigiLockerConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
@Configuration
public class DigiLockerConfig {
	
	@Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
