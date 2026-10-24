package com.retail.insights_api.config;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class TenantAccess {
    public void check(String tenantId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() ||
            auth.getAuthorities().stream().noneMatch(a -> a.getAuthority().equals("ROLE_" + tenantId))) {
            throw new AccessDeniedException("Tenant access denied");
        }
    }
}
