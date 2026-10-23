
package com.retail.insights_api.repository;

import com.retail.insights_api.entity.CampaignMetric;
import com.retail.insights_api.entity.CampaignMetricId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampaignMetricRepository
        extends JpaRepository<CampaignMetric, CampaignMetricId> {

    // Check if campaign exists for a specific tenant
    boolean existsByTenantIdAndCampaignId(
        String tenantId,
        String campaignId
    );

    // Get total clicks for a campaign
    @Query("""
        SELECT COALESCE(SUM(c.clicks), 0)
        FROM CampaignMetric c
        WHERE c.tenantId = :tenantId
          AND c.campaignId = :campaignId
        """)
    Long getTotalClicks(
        @Param("tenantId") String tenantId,
        @Param("campaignId") String campaignId
    );

    // Get total impressions for a campaign
    @Query("""
        SELECT COALESCE(SUM(c.impressions), 0)
        FROM CampaignMetric c
        WHERE c.tenantId = :tenantId
          AND c.campaignId = :campaignId
        """)
    Long getTotalImpressions(
        @Param("tenantId") String tenantId,
        @Param("campaignId") String campaignId
    );

    // Get total click-to-basket conversions
    @Query("""
        SELECT COALESCE(SUM(c.clickToBasket), 0)
        FROM CampaignMetric c
        WHERE c.tenantId = :tenantId
          AND c.campaignId = :campaignId
        """)
    Long getTotalClickToBasket(
        @Param("tenantId") String tenantId,
        @Param("campaignId") String campaignId
    );
}
