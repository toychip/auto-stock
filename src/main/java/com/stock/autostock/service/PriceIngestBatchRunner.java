package com.stock.autostock.service;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PriceIngestBatchRunner {

    private final JobLauncher jobLauncher;
    private final Job priceIngestJob;

    public void run() throws Exception {
        jobLauncher.run(
                priceIngestJob,
                new JobParametersBuilder().addLong("ts", System.currentTimeMillis()).toJobParameters()
        );
    }
}
