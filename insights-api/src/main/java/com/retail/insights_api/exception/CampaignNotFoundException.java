
package com.retail.insights_api.exception;

public class CampaignNotFoundException extends RuntimeException {

    public CampaignNotFoundException(
            String tenantId, String campaignId) {
        super("Campaign " + campaignId +
              " not found for tenant " + tenantId);
    }
}
