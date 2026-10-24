package com.retail.insights_api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@ConfigurationProperties(prefix = "retail")
public record TenantConfig(Map<String, TenantSettings> tenants) {
    public record TenantSettings(int attributionHours, int retentionDays, int apiQuotaPerMinute) {}
}
