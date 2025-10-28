package com.stock.autostock.config;

import com.stock.autostock.service.TokenManagerLean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KisProps.class)
public class KisConfig {

    @Bean("kisRestClient")
    RestClient kisRestClient(RestClient.Builder builder, KisProps props) {
        return builder
                .baseUrl(props.baseUrl())
                .defaultHeader("appkey", props.appKey())
                .defaultHeader("appsecret", props.appSecret())
                .defaultHeader("custtype", props.custType())
                .build();
    }

    @Bean
    TokenManagerLean tokenManagerLean(@Qualifier("kisRestClient") RestClient rc, KisProps p) {
        return new TokenManagerLean(rc, p.appKey(), p.appSecret());
    }
}
