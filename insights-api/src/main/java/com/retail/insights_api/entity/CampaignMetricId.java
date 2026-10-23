
package com.retail.insights_api.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class CampaignMetricId implements Serializable {

    private String tenantId;
    private String campaignId;
    private Instant windowStart;

    public CampaignMetricId() {}

    public CampaignMetricId(String tenantId, String campaignId,
                            Instant windowStart) {
        this.tenantId = tenantId;
        this.campaignId = campaignId;
        this.windowStart = windowStart;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof CampaignMetricId other)) return false;

        return Objects.equals(tenantId, other.tenantId)
            && Objects.equals(campaignId, other.campaignId)
            && Objects.equals(windowStart, other.windowStart);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tenantId, campaignId, windowStart);
    }
}
