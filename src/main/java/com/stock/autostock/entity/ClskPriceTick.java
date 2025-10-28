package com.stock.autostock.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "tick_clsk",
        indexes = {
                @Index(name = "idx_clsk_market_ts", columnList = "market_ts DESC"),
                @Index(name = "idx_clsk_ingested_at", columnList = "ingested_at DESC")
        },
        uniqueConstraints = @UniqueConstraint(name = "uniq_clsk_market_ts", columnNames = {"market_ts"}))
public class ClskPriceTick extends BaseSymbolTick {

    public ClskPriceTick() {
    }

    public ClskPriceTick(String exchange, BigDecimal last, Instant marketTs,
                         Instant ingestedAt, PriceSource source) {
        super(exchange, last, marketTs, ingestedAt, source);
    }

    @Override
    public String ticker() {
        return "CLSK";
    }
}