package com.stock.autostock.config;

import com.stock.autostock.service.PriceIngestBatchRunner;
import lombok.RequiredArgsConstructor;
import org.quartz.SimpleTrigger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.MethodInvokingJobDetailFactoryBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

import java.util.Objects;

@Configuration
@RequiredArgsConstructor
public class QuartzBatchLauncherConfig {

    private final PriceIngestBatchRunner batchRunner;

    @Bean
    public MethodInvokingJobDetailFactoryBean priceIngestJobDetail() {
        var f = new MethodInvokingJobDetailFactoryBean();
        f.setTargetObject(batchRunner);
        f.setTargetMethod("run");
        f.setName("priceIngestInvoker");
        f.setConcurrent(false);
        return f;
    }

    @Bean
    public SimpleTriggerFactoryBean priceIngestTrigger(MethodInvokingJobDetailFactoryBean priceIngestJobDetail) {
        var t = new SimpleTriggerFactoryBean();
        t.setJobDetail(Objects.requireNonNull(priceIngestJobDetail.getObject()));
        t.setRepeatInterval(1000L);
        t.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        t.setStartDelay(0L);
        t.setName("priceIngestTrigger");
        return t;
    }

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(SimpleTriggerFactoryBean priceIngestTrigger,
                                                     MethodInvokingJobDetailFactoryBean priceIngestJobDetail) {
        var f = new SchedulerFactoryBean();
        f.setJobDetails(priceIngestJobDetail.getObject());
        f.setTriggers(priceIngestTrigger.getObject());
        return f;
    }
}
