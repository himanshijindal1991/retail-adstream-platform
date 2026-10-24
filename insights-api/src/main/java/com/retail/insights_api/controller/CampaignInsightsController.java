
package com.retail.insights_api.controller;

import com.retail.insights_api.service.CampaignInsightsService;
import com.retail.insights_api.config.TenantAccess;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
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
            @PathVariable String campaignId,
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
            @PathVariable String campaignId,
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
            @PathVariable String campaignId,
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
