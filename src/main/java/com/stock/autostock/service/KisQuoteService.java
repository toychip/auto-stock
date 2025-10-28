package com.stock.autostock.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stock.autostock.domain.TradableUs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class KisQuoteService {

    private static final String PATH_PRICE = "/uapi/overseas-price/v1/quotations/price";
    private static final String HDR_AUTH = "authorization";
    private static final String HDR_TRID = "tr_id";
    private static final String TR_OVERSEAS_PRICE = "HHDFS00000300"; // 해외주식 현재가
    private static final Map<String, ZoneId> EXCHANGE_TZ = Map.of(
            "NASD", ZoneId.of("America/New_York"),
            "NYSE", ZoneId.of("America/New_York"),
            "AMEX", ZoneId.of("America/New_York")
    );
    private final RestClient kisRestClient;      // @Qualifier("kisRestClient") 로 등록됨
    private final TokenManagerLean tokenManager;

    private static Instant toMarketInstant(String xymd, String hms, String exchangeCode) {
        ZoneId zone = EXCHANGE_TZ.getOrDefault(exchangeCode, ZoneId.of("UTC"));
        var d = LocalDate.parse(xymd, DateTimeFormatter.BASIC_ISO_DATE);
        var t = LocalTime.parse(hms.substring(0, 6), DateTimeFormatter.ofPattern("HHmmss"));
        return ZonedDateTime.of(d, t, zone).toInstant();
    }

    public LastTick getLastTick(TradableUs asset) {
        return getLastTick(asset.exchange(), asset.ticker());
    }

    public LastTick getLastTick(String exchangeCode, String symbol) {
        var auth = tokenManager.bearer();
        try {
            var res = kisRestClient.get()
                    .uri(u -> u.path(PATH_PRICE)
                            .queryParam("AUTH", "")
                            .queryParam("EXCD", exchangeCode)
                            .queryParam("SYMB", symbol)
                            .build())
                    .header(HDR_AUTH, auth)
                    .header(HDR_TRID, TR_OVERSEAS_PRICE)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(PriceResponse.class);

            if (res == null || !"0".equals(res.rt_cd) || res.output == null || res.output.last == null) {
                throw new IllegalStateException("현재가 조회 실패: " + (res != null ? res.msg_cd + "/" + res.msg1 : "null"));
            }

            Instant marketTs = null;
            if (res.output.xymd != null && res.output.hms != null) {
                marketTs = toMarketInstant(res.output.xymd, res.output.hms, exchangeCode);
            }
            if (marketTs == null) marketTs = Instant.now(); // 최후 보정

            return new LastTick(res.output.last, marketTs);

        } catch (HttpClientErrorException.Unauthorized e) {
            tokenManager.forceRefresh();
            return getLastTick(exchangeCode, symbol);
        }
    }

    public record LastTick(BigDecimal last, Instant marketTs) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class PriceResponse {
        @JsonProperty("rt_cd") String rt_cd;
        @JsonProperty("msg_cd") String msg_cd;
        @JsonProperty("msg1")   String msg1;
        @JsonProperty("output") Output output;

        @JsonIgnoreProperties(ignoreUnknown = true)
        static final class Output {
            @JsonProperty("last") BigDecimal last;
            @JsonProperty("xymd") String xymd; // YYYYMMDD (거래소 현지)
            @JsonProperty("hms")  String hms;  // HHmmss   (거래소 현지)
        }
    }
}
