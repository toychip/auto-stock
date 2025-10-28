package com.stock.autostock.entity;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

public class SymbolPriceCache {

    private final Cache<String, Snapshot> latest;          // 티커별 최신 1건 (시장시각 기준)
    private final Cache<String, Deque<Snapshot>> history;  // 티커별 최근 N개 히스토리
    private final int histCap;
    /**
     * @param staleness 최신 스냅샷의 신선도(예: 5초). 초과 시 latest에서 자동 만료
     * @param histCap   히스토리 최대 보관 개수(예: 600 = 10분치 @1s)
     */
    public SymbolPriceCache(Duration staleness, int histCap) {
        this.histCap = histCap;

        // 최신값: 쓰기 후 staleness 지나면 자동 만료
        this.latest = Caffeine.newBuilder()
                .expireAfterWrite(staleness)
                .initialCapacity(16)
                .maximumSize(1024)          // 티커 수가 적으니 충분
                .recordStats()              // 원하면 메트릭 확인 가능
                .build();

        // 히스토리: 접근 없으면 장시간 지나 자동 청소 (예: staleness의 몇 배)
        this.history = Caffeine.newBuilder()
                .expireAfterAccess(staleness.multipliedBy(12)) // 1분 정도
                .initialCapacity(16)
                .maximumSize(1024)          // 티커 수 만큼만 키가 생김
                .build();
    }

    private static Snapshot toSnapshot(String ticker, BaseSymbolTick t) {
        return new Snapshot(
                ticker,
                t.getExchange(),
                t.getLast(),
                t.getMarketTs(),
                t.getIngestedAt(),
                t.getLatencyMs()
        );
    }

    /**
     * 최신/히스토리 병합. 시장시각(marketTs) 우선, 동일하면 ingestedAt 늦은 쪽 채택
     */
    public void put(String ticker, BaseSymbolTick t) {
        Snapshot incoming = toSnapshot(ticker, t);

        // 최신값 병합 (원자적)
        latest.asMap().compute(ticker, (k, prev) -> {
            if (prev == null) return incoming;
            int cmp = incoming.marketTs().compareTo(prev.marketTs());
            if (cmp > 0) return incoming;
            if (cmp < 0) return prev;
            return incoming.ingestedAt().isAfter(prev.ingestedAt()) ? incoming : prev;
        });

        // 히스토리 덱 (티커별) — 캐시에 없으면 생성
        Deque<Snapshot> dq = history.get(ticker, k -> new ArrayDeque<>(histCap));
        // ArrayDeque는 동시성 컬렉션이 아니라서 덱 자체 수정은 동기화
        synchronized (dq) {
            // 역전 방지(더 과거 것이 뒤에 들어오면 무시)
            Snapshot tail = dq.peekLast();
            if (tail != null && incoming.marketTs().isBefore(tail.marketTs())) {
                return;
            }
            dq.addLast(incoming);
            if (dq.size() > histCap) dq.removeFirst();
        }
    }

    /**
     * 신선도 자동 관리: 만료되면 null 반환(getIfPresent=lazy eviction)
     */
    public Optional<Snapshot> currentFresh(String ticker) {
        return Optional.ofNullable(latest.getIfPresent(ticker));
    }

    /**
     * 특정 시각 이전(<=) 가장 최근 스냅샷 — 히스토리에서 즉시 탐색, 없으면 empty
     */
    public Optional<Snapshot> atOrBefore(String ticker, Instant target) {
        Deque<Snapshot> dq = history.getIfPresent(ticker);
        if (dq == null) return Optional.empty();
        synchronized (dq) {
            var it = dq.descendingIterator();
            while (it.hasNext()) {
                Snapshot s = it.next();
                if (!s.marketTs().isAfter(target)) return Optional.of(s);
            }
        }
        return Optional.empty();
    }

    /**
     * (선택) 캐시 메트릭 노출용
     */
    public CacheStats latestStats() {
        return latest.stats();
    }

    public CacheStats historyStats() {
        return history.stats();
    }

    public record Snapshot(
            String ticker, String exchange,
            BigDecimal last, Instant marketTs,
            Instant ingestedAt, long latencyMs) {
    }
}
