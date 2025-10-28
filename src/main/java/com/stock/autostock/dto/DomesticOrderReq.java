package com.stock.autostock.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * @param ordDvsn 00:지정가, 01:시장가, ...
 * @param ordQty  숫자지만 문자열로
 * @param ordUnpr 단가(시장가면 "0")
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DomesticOrderReq(@JsonProperty("CANO") String cano, @JsonProperty("ACNT_PRDT_CD") String acntPrdtCd,
                               @JsonProperty("PDNO") String pdno, @JsonProperty("ORD_DVSN") String ordDvsn,
                               @JsonProperty("ORD_QTY") String ordQty, @JsonProperty("ORD_UNPR") String ordUnpr) {
    public static DomesticOrderReq market(String cano, String acntPrdtCd, String pdno, int qty) {
        return DomesticOrderReq.builder()
                .cano(cano).acntPrdtCd(acntPrdtCd).pdno(pdno)
                .ordDvsn("01").ordQty(String.valueOf(qty)).ordUnpr("0")
                .build();
    }

    public static DomesticOrderReq limit(String cano, String acntPrdtCd, String pdno, int qty, BigDecimal price) {
        return DomesticOrderReq.builder()
                .cano(cano).acntPrdtCd(acntPrdtCd).pdno(pdno)
                .ordDvsn("00").ordQty(String.valueOf(qty)).ordUnpr(price.toPlainString())
                .build();
    }
}
