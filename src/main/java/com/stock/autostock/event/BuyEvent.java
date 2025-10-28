package com.stock.autostock.event;

import com.stock.autostock.domain.TradableUs;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
public class BuyEvent extends Event {
    public BuyEvent(Instant at, TradableUs tradableUs, int quantity, BigDecimal limitPrice) {
        super(at, tradableUs, quantity, limitPrice);
    }
}
