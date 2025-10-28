package com.stock.autostock.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "kis")
public record KisProps(
        String baseUrl,
        String appKey,
        String appSecret,
        String cano,
        String acntPrdtCd,
        String custType // 보통 "P"
) {}
