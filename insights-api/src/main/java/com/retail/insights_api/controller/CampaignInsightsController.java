
package com.retail.insights_api.controller;

import com.retail.insights_api.service.CampaignInsightsService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/ad")
public class CampaignInsightsController {

    private final CampaignInsightsService service;

    public CampaignInsightsController(
            CampaignInsightsService service) {
        this.service = service;
    }

    @GetMapping("/{campaignId}/clicks")
    public Map<String, Object> getClicks(
            @PathVariable String campaignId,
            @RequestHeader("X-Tenant-Id") String tenantId) {

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

        return Map.of(
            "campaignId", campaignId,
            "tenantId", tenantId,
            "clickToBasket",
            service.getClickToBasket(tenantId, campaignId)
        );
    }
}
