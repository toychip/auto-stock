package com.stock.autostock.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stock.autostock.config.KisProps;
import com.stock.autostock.domain.OverseasOrder;
import com.stock.autostock.domain.TradableUs;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Service
public class KisTradingService {

    private static final String TR_BUY_US = "JTTT1002U";
    private static final String TR_SELL_US = "JTTT1006U";

    private static final String PATH_HASHKEY = "/uapi/hashkey";
    private static final String PATH_ORDER_OVERSEAS = "/uapi/overseas-stock/v1/trading/order";

    private static final String HEADER_AUTHORIZATION = "authorization";
    private static final String HEADER_TR_ID = "tr_id";
    private static final String HEADER_HASHKEY = "hashkey";

    private final KisProps props;
    private final RestClient restClient;
    private final TokenManagerLean tokenManager;

    public KisTradingService(
            KisProps props,
            @Qualifier("kisRestClient") RestClient restClient,
            TokenManagerLean tokenManager
    ) {
        this.props = props;
        this.restClient = restClient;
        this.tokenManager = tokenManager;
    }

    public String buyUsLimit(TradableUs asset, int quantity, BigDecimal limitPrice) {
        OverseasOrder body = OverseasOrder.limit(
                props.cano(), props.acntPrdtCd(), asset, quantity, limitPrice
        );
        return orderOverseas(body, TR_BUY_US);
    }

    public String buyUsMarket(TradableUs asset, int quantity) {
        OverseasOrder body = OverseasOrder.market(
                props.cano(), props.acntPrdtCd(), asset, quantity
        );
        return orderOverseas(body, TR_BUY_US);
    }

    public String sellUsLimit(TradableUs asset, int quantity, BigDecimal limitPrice) {
        OverseasOrder body = OverseasOrder.limit(
                props.cano(), props.acntPrdtCd(), asset, quantity, limitPrice
        );
        return orderOverseas(body, TR_SELL_US);
    }

    public String sellUsMarket(TradableUs asset, int quantity) {
        OverseasOrder body = OverseasOrder.market(
                props.cano(), props.acntPrdtCd(), asset, quantity
        );
        return orderOverseas(body, TR_SELL_US);
    }

    private String authorizationValue() {
        return tokenManager.bearer();
    }

    private String computeHashKey(Object jsonBody) {
        HashKeyResponse res = restClient.post()
                .uri(PATH_HASHKEY)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonBody)
                .retrieve()
                .body(HashKeyResponse.class);

        if (res == null || res.value() == null) {
            throw new IllegalStateException("hashkey 생성 실패");
        }
        return res.value();
    }

    private OrderResponse executeOverseasOrder(OverseasOrder requestBody, String trId, String hashKey, String authorization) {
        return restClient.post()
                .uri(PATH_ORDER_OVERSEAS)
                .contentType(MediaType.APPLICATION_JSON)
                .header(HEADER_AUTHORIZATION, authorization)
                .header(HEADER_TR_ID, trId)
                .header(HEADER_HASHKEY, hashKey)
                .body(requestBody)
                .retrieve()
                .body(OrderResponse.class);
    }

    private String orderOverseas(OverseasOrder requestBody, String trId) {
        try {
            String hashKey = computeHashKey(requestBody);
            String authorization = authorizationValue();

            OrderResponse res = executeOverseasOrder(requestBody, trId, hashKey, authorization);

            if (res == null || res.isNotOk()) {
                throw new IllegalStateException("해외주식 주문 실패: " +
                        (res != null ? res.messageCode() + " / " + res.message() : "null"));
            }
            String ordNo = res.orderNoOrNull();
            return ordNo != null ? ordNo : "OK";

        } catch (HttpClientErrorException.Unauthorized e) {
            tokenManager.forceRefresh();

            String hashKey = computeHashKey(requestBody);
            String authorization = authorizationValue();

            OrderResponse res = executeOverseasOrder(requestBody, trId, hashKey, authorization);

            if (res == null || res.isNotOk()) {
                throw new IllegalStateException("해외주식 주문 실패(재시도): " +
                        (res != null ? res.messageCode() + " / " + res.message() : "null"));
            }
            String ordNo = res.orderNoOrNull();
            return ordNo != null ? ordNo : "OK";

        } catch (HttpClientErrorException e) {
            throw new IllegalStateException("해외주식 주문 실패: " + e.getStatusCode()
                    + " / " + e.getResponseBodyAsString(), e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OrderResponse(
            @JsonProperty("rt_cd") String resultCode,
            @JsonProperty("msg_cd") String messageCode,
            @JsonProperty("msg1") String message,
            @JsonProperty("output") Output output
    ) {
        boolean isNotOk() {
            return !"0".equals(resultCode);
        }

        String orderNoOrNull() {
            return output != null ? output.orderNo() : null;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Output(@JsonProperty("ord_no") String orderNo) {
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record HashKeyResponse(@JsonProperty("HASH") String value) {
    }
}
