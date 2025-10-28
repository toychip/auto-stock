package com.stock.autostock.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OverseasOrder(

        /* 계좌번호(앞 8자리) */
        @JsonProperty("CANO")
        String accountNumber,

        /* 계좌상품코드(뒤 2자리, 보통 "01") */
        @JsonProperty("ACNT_PRDT_CD")
        String accountProductCode,

        /* 해외거래소 코드 (예: NASD) */
        @JsonProperty("OVRS_EXCG_CD")
        String overseasExchangeCode,

        /* 종목 티커 (예: IREN, BITF, CLSK) */
        @JsonProperty("PDNO")
        String productTicker,

        /* 주문구분 코드 (00:지정가, 01:시장가 ...) */
        @JsonProperty("ORD_DVSN")
        String orderTypeCode,

        /* 주문 수량 (문자열로 전송) */
        @JsonProperty("ORD_QTY")
        String orderQuantity,

        /* 주문 단가 (시장가면 "0") */
        @JsonProperty("OVRS_ORD_UNPR")
        String overseasOrderUnitPrice,

        /* 주문서버구분코드 (보통 "0") */
        @JsonProperty("ORD_SVR_DVSN_CD")
        String orderServerDivisionCode
) {
    /** 시장가 주문 — 팩토리 1개 */
    public static OverseasOrder market(
            String accountNumber,
            String accountProductCode,
            TradableUs asset,
            int quantity
    ) {
        return OverseasOrder.builder()
                .accountNumber(accountNumber)
                .accountProductCode(accountProductCode)
                .overseasExchangeCode(asset.exchange())
                .productTicker(asset.ticker())
                .orderTypeCode(OrderType.MARKET.code())
                .orderQuantity(String.valueOf(quantity))
                .overseasOrderUnitPrice("0")
                .orderServerDivisionCode("0")
                .build();
    }

    /** 지정가 주문 — 팩토리 1개 */
    public static OverseasOrder limit(
            String accountNumber,
            String accountProductCode,
            TradableUs asset,
            int quantity,
            BigDecimal limitPrice
    ) {
        return OverseasOrder.builder()
                .accountNumber(accountNumber)
                .accountProductCode(accountProductCode)
                .overseasExchangeCode(asset.exchange())
                .productTicker(asset.ticker())
                .orderTypeCode(OrderType.LIMIT.code())
                .orderQuantity(String.valueOf(quantity))
                .overseasOrderUnitPrice(limitPrice.toPlainString())
                .orderServerDivisionCode("0")
                .build();
    }

    /** 주문구분 코드 (필요 시 IOC/FOK 등 확장 가능) */
    public enum OrderType {
        LIMIT("00"),
        MARKET("01");
        private final String code;
        OrderType(String code) { this.code = code; }
        public String code() { return code; }
    }
}
