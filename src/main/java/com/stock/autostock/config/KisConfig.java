package com.stock.autostock.config;

import com.stock.autostock.service.TokenManagerLean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KisProps.class)
public class KisConfig {

    @Bean
    RestClient kisRestClient(KisProps props) {
        return RestClient.builder().baseUrl(props.baseUrl()).build();
    }

    @Bean
    TokenManagerLean tokenManagerLean(RestClient rc, KisProps p) {
        return new TokenManagerLean(rc, p.appKey(), p.appSecret());
    }
}
