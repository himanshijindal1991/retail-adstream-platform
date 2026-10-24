package com.retail.insights_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "demo.security.target-password=TestTargetPassword123!",
    "demo.security.walmart-password=TestWalmartPassword123!"
})
class CampaignInsightsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static String basicAuth(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(
            credentials.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void validCampaignReturns200() throws Exception {
        mockMvc.perform(get("/ad/CAMP101/clicks")
                .header("Authorization", basicAuth("target-user", "TestTargetPassword123!"))
                .header("X-Tenant-Id", "T001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.clicks").value(12000));
    }

    @Test
    void invalidCampaignIdReturns400() throws Exception {
        mockMvc.perform(get("/ad/INVALID@123/clicks")
                .header("Authorization", basicAuth("target-user", "TestTargetPassword123!"))
                .header("X-Tenant-Id", "T001"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void missingCampaignReturns404() throws Exception {
        mockMvc.perform(get("/ad/CAMP999/clicks")
                .header("Authorization", basicAuth("target-user", "TestTargetPassword123!"))
                .header("X-Tenant-Id", "T001"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.path").value("/ad/CAMP999/clicks"));
    }

    @Test
    void crossTenantAccessReturns403() throws Exception {
        mockMvc.perform(get("/ad/CAMP101/clicks")
                .header("Authorization", basicAuth("target-user", "TestTargetPassword123!"))
                .header("X-Tenant-Id", "T002"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(get("/ad/CAMP101/clicks")
                .header("X-Tenant-Id", "T001"))
            .andExpect(status().isUnauthorized());
    }
}
