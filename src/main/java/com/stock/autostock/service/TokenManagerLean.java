package com.stock.autostock.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

@RequiredArgsConstructor
public class TokenManagerLean {

    private static final long REFRESH_MARGIN_SEC = 60; // 만료 60초 전이면 갱신
    private final RestClient rest;
    private final String appKey;
    private final String appSecret;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile Token token; // 읽기 빠르게

    /** 항상 유효한 Authorization 문자열("Bearer ...") 반환 */
    public String bearer() {
        Token t = token;
        if (t != null && Instant.now().isBefore(t.expires().minusSeconds(REFRESH_MARGIN_SEC))) {
            return t.value();
        }
        lock.lock();
        try {
            t = token;
            if (t == null || Instant.now().isAfter(t.expires().minusSeconds(REFRESH_MARGIN_SEC))) {
                token = fetchToken();
            }
            return token.value();
        } finally {
            lock.unlock();
        }
    }

    /** 401 등 즉시 재발급이 필요할 때 사용(선택) */
    public String forceRefresh() {
        lock.lock();
        try {
            token = fetchToken();
            return token.value();
        } finally {
            lock.unlock();
        }
    }

    private Token fetchToken() {
        var body = Map.of(
                "grant_type", "client_credentials",
                "appkey", appKey,
                "appsecret", appSecret
        );
        var res = rest.post()
                .uri("/oauth2/tokenP")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(TokenResponse.class);
        if (res == null || res.access_token() == null) {
            throw new IllegalStateException("KIS 토큰 발급 실패");
        }
        long ttl = Math.min(res.expires_in(), 23 * 3600); // 안전하게 23h로 캡
        return new Token(res.token_type() + " " + res.access_token(),
                Instant.now().plusSeconds(ttl));
    }

    private record TokenResponse(String access_token, String token_type, long expires_in) {}
    private record Token(String value, Instant expires) {}
}
