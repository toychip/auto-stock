package com.stock.autostock.event;

import com.stock.autostock.domain.TradableUs;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@RequiredArgsConstructor
public abstract class Event {

    private final Instant at;
    private final TradableUs tradableUs;
    private final int quantity;
    private final BigDecimal limitPrice;
}
