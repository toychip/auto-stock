package com.stock.autostock.domain;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum TradableUs {
    IREN("IREN", "NASD"),
    BITF("BITF", "NASD"),
    CLSK("CLSK", "NASD");

    private final String ticker;   // PDNO에 들어갈 값
    private final String exchange; // OVRS_EXCG_CD에 들어갈 값

    public String ticker()   { return ticker; }
    public String exchange() { return exchange; }
}

