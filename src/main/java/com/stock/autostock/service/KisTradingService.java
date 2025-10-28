package com.stock.autostock.service;

import com.stock.autostock.config.KisProps;
import com.stock.autostock.dto.DomesticOrderReq;
import com.stock.autostock.dto.OverseasOrderReq;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KisTradingService {

    private static final String TR_BUY_US  = "JTTT1002U";
    private static final String TR_SELL_US = "JTTT1006U";
    private static final String NASD = "NASD";

    private static final Set<String> ALLOWED_US = Set.of("IREN","BITF","CLSK");

    private final RestClient rest;
    private final KisProps props;
    private final TokenManagerLean tokenManager;

    // ---------- 해외(나스닥 고정, IREN/BITF/CLSK만) ----------
    public String buyUsLimit(String ticker, int qty, BigDecimal price) {
        assertUsTicker(ticker);
        var body = OverseasOrderReq.limit(props.cano(), props.acntPrdtCd(), NASD, ticker, qty, price);
        return orderOverseas(body, TR_BUY_US);
    }
    public String buyUsMarket(String ticker, int qty) {
        assertUsTicker(ticker);
        var body = OverseasOrderReq.market(props.cano(), props.acntPrdtCd(), NASD, ticker, qty);
        return orderOverseas(body, TR_BUY_US);
    }
    public String sellUsLimit(String ticker, int qty, BigDecimal price) {
        assertUsTicker(ticker);
        var body = OverseasOrderReq.limit(props.cano(), props.acntPrdtCd(), NASD, ticker, qty, price);
        return orderOverseas(body, TR_SELL_US);
    }
    public String sellUsMarket(String ticker, int qty) {
        assertUsTicker(ticker);
        var body = OverseasOrderReq.market(props.cano(), props.acntPrdtCd(), NASD, ticker, qty);
        return orderOverseas(body, TR_SELL_US);
    }

    // ---------- 공통 ----------
    private void assertUsTicker(String t) {
        if (!ALLOWED_US.contains(t.toUpperCase())) {
            throw new IllegalArgumentException("허용되지 않은 미국 종목: " + t + " (허용: " + ALLOWED_US + ")");
        }
    }

    private String hashKey(Object jsonBody) { // DTO 그대로 직렬화해서 보냄
        var res = rest.post()
                .uri("/uapi/hashkey")
                .contentType(MediaType.APPLICATION_JSON)
                .header("appkey", props.appKey())
                .header("appsecret", props.appSecret())
                .body(jsonBody)
                .retrieve()
                .body(HashKey.class);
        if (res == null || res.HASH() == null) throw new IllegalStateException("hashkey 생성 실패");
        return res.HASH();
    }

    private String orderDomestic(DomesticOrderReq body, String trId) {
        try {
            var res = rest.post()
                    .uri("/uapi/domestic-stock/v1/trading/order-cash")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("authorization", tokenManager.bearer())
                    .header("appkey", props.appKey())
                    .header("appsecret", props.appSecret())
                    .header("custtype", props.custType())
                    .header("tr_id", trId)
                    .header("hashkey", hashKey(body))
                    .body(body)
                    .retrieve()
                    .body(OrderRes.class);

            if (res == null || !"0".equals(res.rt_cd())) {
                throw new IllegalStateException("국내주식 주문 실패: " +
                        (res != null ? res.msg_cd() + " / " + res.msg1() : "null"));
            }
            return res.output() != null ? res.output().ord_no() : "OK";
        } catch (HttpClientErrorException.Unauthorized e) {
            tokenManager.forceRefresh();
            var res = rest.post()
                    .uri("/uapi/domestic-stock/v1/trading/order-cash")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("authorization", tokenManager.bearer())
                    .header("appkey", props.appKey())
                    .header("appsecret", props.appSecret())
                    .header("custtype", props.custType())
                    .header("tr_id", trId)
                    .header("hashkey", hashKey(body))
                    .body(body)
                    .retrieve()
                    .body(OrderRes.class);
            if (res == null || !"0".equals(res.rt_cd())) {
                throw new IllegalStateException("국내주식 주문 실패(재시도): " +
                        (res != null ? res.msg_cd() + " / " + res.msg1() : "null"));
            }
            return res.output() != null ? res.output().ord_no() : "OK";
        }
    }

    private String orderOverseas(OverseasOrderReq body, String trId) {
        try {
            var res = rest.post()
                    .uri("/uapi/overseas-stock/v1/trading/order")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("authorization", tokenManager.bearer())
                    .header("appkey", props.appKey())
                    .header("appsecret", props.appSecret())
                    .header("custtype", props.custType())
                    .header("tr_id", trId)
                    .header("hashkey", hashKey(body))
                    .body(body)
                    .retrieve()
                    .body(OrderRes.class);

            if (res == null || !"0".equals(res.rt_cd())) {
                throw new IllegalStateException("해외주식 주문 실패: " +
                        (res != null ? res.msg_cd() + " / " + res.msg1() : "null"));
            }
            return res.output() != null ? res.output().ord_no() : "OK";
        } catch (HttpClientErrorException.Unauthorized e) {
            tokenManager.forceRefresh();
            var res = rest.post()
                    .uri("/uapi/overseas-stock/v1/trading/order")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("authorization", tokenManager.bearer())
                    .header("appkey", props.appKey())
                    .header("appsecret", props.appSecret())
                    .header("custtype", props.custType())
                    .header("tr_id", trId)
                    .header("hashkey", hashKey(body))
                    .body(body)
                    .retrieve()
                    .body(OrderRes.class);
            if (res == null || !"0".equals(res.rt_cd())) {
                throw new IllegalStateException("해외주식 주문 실패(재시도): " +
                        (res != null ? res.msg_cd() + " / " + res.msg1() : "null"));
            }
            return res.output() != null ? res.output().ord_no() : "OK";
        }
    }

    // 응답 DTO
    private record HashKey(String HASH) {}
    private record OrderRes(String rt_cd, String msg_cd, String msg1, Output output) {
        record Output(String ord_no) {}
    }
}
