
package com.retail.insights_api.controller;

import com.retail.insights_api.service.CampaignInsightsService;
import com.retail.insights_api.config.TenantAccess;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/ad")
public class CampaignInsightsController {

    private final CampaignInsightsService service;
    private final TenantAccess tenantAccess;

    public CampaignInsightsController(
            CampaignInsightsService service, TenantAccess tenantAccess) {
        this.service = service;
        this.tenantAccess = tenantAccess;
    }

    @GetMapping("/{campaignId}/clicks")
    public Map<String, Object> getClicks(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}", message = "Invalid campaign ID") String campaignId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        tenantAccess.check(tenantId);
        return Map.of(
            "campaignId", campaignId,
            "tenantId", tenantId,
            "clicks", service.getClicks(tenantId, campaignId)
        );
    }

    @GetMapping("/{campaignId}/impressions")
    public Map<String, Object> getImpressions(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}", message = "Invalid campaign ID") String campaignId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        tenantAccess.check(tenantId);
        return Map.of(
            "campaignId", campaignId,
            "tenantId", tenantId,
            "impressions", service.getImpressions(tenantId, campaignId)
        );
    }

    @GetMapping("/{campaignId}/clickToBasket")
    public Map<String, Object> getClickToBasket(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}", message = "Invalid campaign ID") String campaignId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

        tenantAccess.check(tenantId);
        return Map.of(
            "campaignId", campaignId,
            "tenantId", tenantId,
            "clickToBasket",
            service.getClickToBasket(tenantId, campaignId)
        );
    }
}
