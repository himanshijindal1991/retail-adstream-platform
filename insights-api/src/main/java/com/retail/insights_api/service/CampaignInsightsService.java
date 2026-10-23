
package com.retail.insights_api.service;

import com.retail.insights_api.exception.CampaignNotFoundException;
import com.retail.insights_api.repository.CampaignMetricRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CampaignInsightsService {

    private final CampaignMetricRepository repository;

    public CampaignInsightsService(
            CampaignMetricRepository repository) {
        this.repository = repository;
    }

    private void validateCampaign(
            String tenantId, String campaignId) {

        if (!repository.existsByTenantIdAndCampaignId(
                tenantId, campaignId)) {
            throw new CampaignNotFoundException(
                tenantId, campaignId);
        }
    }

    public long getClicks(String tenantId, String campaignId) {
        validateCampaign(tenantId, campaignId);
        return repository.getTotalClicks(tenantId, campaignId);
    }

    public long getImpressions(String tenantId, String campaignId) {
        validateCampaign(tenantId, campaignId);
        return repository.getTotalImpressions(tenantId, campaignId);
    }

    public long getClickToBasket(String tenantId, String campaignId) {
        validateCampaign(tenantId, campaignId);
        return repository.getTotalClickToBasket(tenantId, campaignId);
    }
}
