
package com.retail.insights_api.service;

import com.retail.insights_api.exception.CampaignNotFoundException;
import com.retail.insights_api.repository.CampaignMetricRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CampaignInsightsServiceTest {

    @Mock
    private CampaignMetricRepository repository;

    @InjectMocks
    private CampaignInsightsService service;

    private static final String TENANT_ID = "T001";
    private static final String CAMPAIGN_ID = "CAMP101";

    @Test
    void shouldReturnClicks() {
        when(repository.existsByTenantIdAndCampaignId(
                TENANT_ID, CAMPAIGN_ID)).thenReturn(true);

        when(repository.getTotalClicks(
                TENANT_ID, CAMPAIGN_ID)).thenReturn(12000L);

        long result = service.getClicks(TENANT_ID, CAMPAIGN_ID);

        assertEquals(12000L, result);
        verify(repository).getTotalClicks(TENANT_ID, CAMPAIGN_ID);
    }

    @Test
    void shouldReturnImpressions() {
        when(repository.existsByTenantIdAndCampaignId(
                TENANT_ID, CAMPAIGN_ID)).thenReturn(true);

        when(repository.getTotalImpressions(
                TENANT_ID, CAMPAIGN_ID)).thenReturn(100000L);

        long result = service.getImpressions(TENANT_ID, CAMPAIGN_ID);

        assertEquals(100000L, result);
        verify(repository).getTotalImpressions(
                TENANT_ID, CAMPAIGN_ID);
    }

    @Test
    void shouldReturnClickToBasket() {
        when(repository.existsByTenantIdAndCampaignId(
                TENANT_ID, CAMPAIGN_ID)).thenReturn(true);

        when(repository.getTotalClickToBasket(
                TENANT_ID, CAMPAIGN_ID)).thenReturn(2500L);

        long result = service.getClickToBasket(
                TENANT_ID, CAMPAIGN_ID);

        assertEquals(2500L, result);
        verify(repository).getTotalClickToBasket(
                TENANT_ID, CAMPAIGN_ID);
    }

    @Test
    void shouldReturnZeroWhenNoClicks() {
        when(repository.existsByTenantIdAndCampaignId(
                TENANT_ID, CAMPAIGN_ID)).thenReturn(true);

        when(repository.getTotalClicks(
                TENANT_ID, CAMPAIGN_ID)).thenReturn(0L);

        long result = service.getClicks(TENANT_ID, CAMPAIGN_ID);

        assertEquals(0L, result);
    }

    @Test
    void shouldThrowExceptionWhenCampaignDoesNotExist() {
        when(repository.existsByTenantIdAndCampaignId(
                TENANT_ID, "UNKNOWN")).thenReturn(false);

        assertThrows(
            CampaignNotFoundException.class,
            () -> service.getClicks(TENANT_ID, "UNKNOWN")
        );

        verify(repository, never())
                .getTotalClicks(TENANT_ID, "UNKNOWN");
    }

    @Test
    void shouldNotAccessAnotherTenantsCampaign() {
        when(repository.existsByTenantIdAndCampaignId(
                "T002", CAMPAIGN_ID)).thenReturn(false);

        assertThrows(
            CampaignNotFoundException.class,
            () -> service.getClicks("T002", CAMPAIGN_ID)
        );

        verify(repository, never())
                .getTotalClicks("T002", CAMPAIGN_ID);
    }
}
