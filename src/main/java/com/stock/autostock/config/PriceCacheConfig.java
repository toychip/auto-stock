package com.stock.autostock.config;

import com.stock.autostock.entity.SymbolPriceCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class PriceCacheConfig {

    @Bean
    public SymbolPriceCache symbolPriceCache() {
        return new SymbolPriceCache(Duration.ofSeconds(5), 600);
    }
}