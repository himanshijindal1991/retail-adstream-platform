
package com.retail.insights_api.config;

import com.retail.insights_api.entity.CampaignMetric;
import com.retail.insights_api.repository.CampaignMetricRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner loadSampleData(
            CampaignMetricRepository repository) {

        return args -> {

            if (repository.count() == 0) {

                repository.save(new CampaignMetric(
                    "T001",
                    "CAMP101",
                    Instant.parse("2026-10-23T00:00:00Z"),
                    100000,
                    12000,
                    2500
                ));

                repository.save(new CampaignMetric(
                    "T001",
                    "CAMP102",
                    Instant.parse("2026-10-23T00:00:00Z"),
                    80000,
                    9000,
                    1800
                ));

                repository.save(new CampaignMetric(
                    "T002",
                    "CAMP101",
                    Instant.parse("2026-10-23T00:00:00Z"),
                    50000,
                    6000,
                    1000
                ));
            }
        };
    }
}
