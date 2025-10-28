package com.stock.autostock.config;

import com.stock.autostock.domain.TradableUs;
import com.stock.autostock.entity.BaseSymbolTick.PriceSource;
import com.stock.autostock.entity.*;
import com.stock.autostock.service.KisQuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Future;

@Configuration
@RequiredArgsConstructor
public class PriceIngestJobConfig {

    private final KisQuoteService kis;
    private final SymbolPriceCache cache;
    private final IrenTickRepository irenRepo;
    private final BitfTickRepository bitfRepo;
    private final ClskTickRepository clskRepo;

    @Bean
    public TaskExecutor priceIngestExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setThreadNamePrefix("price-ingest-");
        ex.setCorePoolSize(4);
        ex.setMaxPoolSize(4);
        ex.setQueueCapacity(16);
        ex.initialize();
        return ex;
    }

    @Bean
    @StepScope
    public ItemReader<TradableUs> priceItemReader() {
        return new ListItemReader<>(List.of(TradableUs.IREN, TradableUs.BITF, TradableUs.CLSK));
    }

    @Bean
    public ItemProcessor<TradableUs, TickItem> priceItemProcessor() {
        return asset -> {
            var s = kis.getLastTick(asset);
            var now = Instant.now();
            return new TickItem(asset, s.last(), s.marketTs(), now, PriceSource.PRICE_LAST);
        };
    }

    @Bean
    public AsyncItemProcessor<TradableUs, TickItem> asyncProcessor(ItemProcessor<TradableUs, TickItem> delegate,
                                                                   TaskExecutor priceIngestExecutor) {
        AsyncItemProcessor<TradableUs, TickItem> p = new AsyncItemProcessor<>();
        p.setDelegate(delegate);
        p.setTaskExecutor(priceIngestExecutor);
        return p;
    }

    @Bean
    public ItemWriter<TickItem> priceItemWriter() {
        return items -> {
            for (var it : items) {
                switch (it.asset()) {
                    case IREN -> {
                        var e = new IrenPriceTick(it.asset().exchange(), it.last(), it.marketTs(), it.ingestedAt(), it.source());
                        irenRepo.save(e);
                        cache.put(it.asset().ticker(), e);
                    }
                    case BITF -> {
                        var e = new BitfPriceTick(it.asset().exchange(), it.last(), it.marketTs(), it.ingestedAt(), it.source());
                        bitfRepo.save(e);
                        cache.put(it.asset().ticker(), e);
                    }
                    case CLSK -> {
                        var e = new ClskPriceTick(it.asset().exchange(), it.last(), it.marketTs(), it.ingestedAt(), it.source());
                        clskRepo.save(e);
                        cache.put(it.asset().ticker(), e);
                    }
                }
            }
        };
    }

    @Bean
    public AsyncItemWriter<TickItem> asyncWriter(ItemWriter<TickItem> delegate) {
        AsyncItemWriter<TickItem> w = new AsyncItemWriter<>();
        w.setDelegate(delegate);
        return w;
    }

    /* ===== Step & Job ===== */
    @Bean
    public Step priceIngestStep(JobRepository repo,
                                PlatformTransactionManager tx,
                                ItemReader<TradableUs> priceItemReader,
                                AsyncItemProcessor<TradableUs, TickItem> asyncProcessor,
                                AsyncItemWriter<TickItem> asyncWriter) {

        // AsyncItemProcessor 는 Future<TickItem>을 배출하므로, Step 제네릭을 Future로 맞춰야 함
        return new StepBuilder("priceIngestStep", repo)
                .<TradableUs, Future<TickItem>>chunk(3, tx) // 3종 동시
                .reader(priceItemReader)
                .processor(asyncProcessor)
                .writer(asyncWriter)
                .build();
    }

    @Bean
    public Job priceIngestJob(JobRepository repo, Step priceIngestStep) {
        return new JobBuilder("priceIngestJob", repo)
                .start(priceIngestStep)
                .build();
    }

    /* ===== DTO ===== */
    public record TickItem(TradableUs asset,
                           java.math.BigDecimal last,
                           java.time.Instant marketTs,
                           java.time.Instant ingestedAt,
                           PriceSource source) {}
}
