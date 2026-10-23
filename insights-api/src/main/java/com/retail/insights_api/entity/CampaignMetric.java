
package com.retail.insights_api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "campaign_metrics")
@IdClass(CampaignMetricId.class)
public class CampaignMetric {

    @Id
    @Column(name = "tenant_id")
    private String tenantId;

    @Id
    @Column(name = "campaign_id")
    private String campaignId;

    @Id
    @Column(name = "window_start")
    private Instant windowStart;

    @Column(nullable = false)
    private long impressions;

    @Column(nullable = false)
    private long clicks;

    @Column(name = "click_to_basket", nullable = false)
    private long clickToBasket;

    public CampaignMetric() {}

    public CampaignMetric(String tenantId, String campaignId,
                          Instant windowStart, long impressions,
                          long clicks, long clickToBasket) {
        this.tenantId = tenantId;
        this.campaignId = campaignId;
        this.windowStart = windowStart;
        this.impressions = impressions;
        this.clicks = clicks;
        this.clickToBasket = clickToBasket;
    }

    public String getTenantId() { return tenantId; }
    public String getCampaignId() { return campaignId; }
    public Instant getWindowStart() { return windowStart; }
    public long getImpressions() { return impressions; }
    public long getClicks() { return clicks; }
    public long getClickToBasket() { return clickToBasket; }
}
