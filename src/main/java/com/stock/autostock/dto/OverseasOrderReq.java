package com.stock.autostock.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.math.BigDecimal;

/**
 * @param ovrsExcgCd   예: NASD
 * @param pdno         티커
 * @param ordDvsn      00:지정가, 01:시장가
 * @param ordQty       문자열
 * @param ovrsOrdUnpr  단가(시장가면 "0")
 * @param ordSvrDvsnCd 보통 "0"
 */
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OverseasOrderReq(@JsonProperty("CANO") String cano, @JsonProperty("ACNT_PRDT_CD") String acntPrdtCd,
                               @JsonProperty("OVRS_EXCG_CD") String ovrsExcgCd, @JsonProperty("PDNO") String pdno,
                               @JsonProperty("ORD_DVSN") String ordDvsn, @JsonProperty("ORD_QTY") String ordQty,
                               @JsonProperty("OVRS_ORD_UNPR") String ovrsOrdUnpr,
                               @JsonProperty("ORD_SVR_DVSN_CD") String ordSvrDvsnCd) {
    public static OverseasOrderReq market(String cano, String acntPrdtCd, String exch, String ticker, int qty) {
        return OverseasOrderReq.builder()
                .cano(cano).acntPrdtCd(acntPrdtCd).ovrsExcgCd(exch)
                .pdno(ticker.toUpperCase())
                .ordDvsn("01").ordQty(String.valueOf(qty))
                .ovrsOrdUnpr("0").ordSvrDvsnCd("0")
                .build();
    }

    public static OverseasOrderReq limit(String cano, String acntPrdtCd, String exch, String ticker, int qty, BigDecimal price) {
        return OverseasOrderReq.builder()
                .cano(cano).acntPrdtCd(acntPrdtCd).ovrsExcgCd(exch)
                .pdno(ticker.toUpperCase())
                .ordDvsn("00").ordQty(String.valueOf(qty))
                .ovrsOrdUnpr(price.toPlainString()).ordSvrDvsnCd("0")
                .build();
    }
}
