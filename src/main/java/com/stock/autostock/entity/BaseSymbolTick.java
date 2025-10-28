package com.stock.autostock.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@MappedSuperclass
@Getter
@NoArgsConstructor
public abstract class BaseSymbolTick {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(length = 8, nullable = false)
    private String exchange;
    @Column(precision = 19, scale = 8, nullable = false)
    private BigDecimal last;
    @Column(name = "market_ts", nullable = false)
    private Instant marketTs;
    @Column(name = "ingested_at", nullable = false)
    private Instant ingestedAt;
    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private PriceSource source;
    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    protected BaseSymbolTick(String exchange, BigDecimal last,
                             Instant marketTs, Instant ingestedAt, PriceSource source) {
        this.exchange = exchange;
        this.last = last;
        this.marketTs = marketTs;
        this.ingestedAt = ingestedAt;
        this.source = source;
        this.latencyMs = Math.max(0, ingestedAt.toEpochMilli() - marketTs.toEpochMilli());
    }

    // 각 엔티티에서 상수로 반환
    @Transient
    public abstract String ticker();

    public enum PriceSource { PRICE_LAST, TICK_STREAM }
}
