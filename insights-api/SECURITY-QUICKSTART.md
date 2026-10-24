# Demo tenant security and configuration

This is **HTTP Basic demo authentication**, not the proposed production OAuth2/OIDC/JWT integration. Two demo users have tenant-specific roles. Passwords must be provided as environment variables. Do not use real credentials.

```bash
export TARGET_DEMO_PASSWORD='choose-a-local-demo-password'
export WALMART_DEMO_PASSWORD='choose-a-different-local-demo-password'
./mvnw spring-boot:run
```

In another terminal:

```bash
curl -i -u target-user:$TARGET_DEMO_PASSWORD -H 'X-Tenant-Id: T001' http://localhost:8080/ad/CAMP101/clicks
# 200 OK, clicks 12000
curl -i -u target-user:$TARGET_DEMO_PASSWORD -H 'X-Tenant-Id: T002' http://localhost:8080/ad/CAMP101/clicks
# 403 Forbidden
curl -i -H 'X-Tenant-Id: T001' http://localhost:8080/ad/CAMP101/clicks
# 401 Unauthorized
```

Tenant configuration is in `application.properties` under `retail.tenants.*`. It models attribution windows, retention and quota settings; **these values are not enforced by this reporting API**. Production requires OIDC JWT validation, per-tenant rate limiting, and Flink configuration integration. Run existing tests and adjust controller tests to provide mock authentication where needed.
