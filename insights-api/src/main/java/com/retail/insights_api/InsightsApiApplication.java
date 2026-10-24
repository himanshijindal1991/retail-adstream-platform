package com.retail.insights_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.retail.insights_api.config.TenantConfig;

@SpringBootApplication
@EnableConfigurationProperties(TenantConfig.class)
public class InsightsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(InsightsApiApplication.class, args);
	}

}
