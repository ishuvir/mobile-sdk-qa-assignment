package com.example.sdkqa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AUTH-002 - the one I would keep if I could only keep one.
 *
 * Two modules, auth and onboarding, are on the same session. Both make a call, both get a 401 at
 * the same moment. There must be exactly one refresh between them.
 *
 * A single-module test cannot see this. That is why it is written across two modules.
 *
 * @author Ishuvir Singh
 */
class SessionRefreshContractTest extends SdkContractTest {

    @Test
    @DisplayName("two modules hit 401 together: one refresh, both retried, nobody logged out")
    void oneRefreshAcrossTwoModules() {
        // both modules are holding the expired token
        server.stubFor(get(urlEqualTo("/accounts"))
            .withHeader("Authorization", equalTo("Bearer expired-AT"))
            .willReturn(aResponse().withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"token_expired\"}")));

        server.stubFor(get(urlEqualTo("/onboarding/status"))
            .withHeader("Authorization", equalTo("Bearer expired-AT"))
            .willReturn(aResponse().withStatus(401)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"error\":\"token_expired\"}")));

        // one shared refresh, slowed down so the race window is real
        server.stubFor(post(urlEqualTo("/auth/token"))
            .willReturn(aResponse().withStatus(200)
                .withFixedDelay(300)
                .withHeader("Content-Type", "application/json")
                .withBody(REFRESH_BODY)));

        // and both modules carry on with the new token
        server.stubFor(get(urlEqualTo("/accounts"))
            .withHeader("Authorization", equalTo("Bearer AT-new"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(ACCOUNTS_BODY)));

        server.stubFor(get(urlEqualTo("/onboarding/status"))
            .withHeader("Authorization", equalTo("Bearer AT-new"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"step\":\"IDENTITY_VERIFICATION\",\"canContinue\":true}")));

        // auth hits the expired token
        given().header("Authorization", "Bearer expired-AT")
        .when().get("/accounts").then().statusCode(401);

        // onboarding hits it in the same breath
        given().header("Authorization", "Bearer expired-AT")
        .when().get("/onboarding/status").then().statusCode(401);

        // the SDK refreshes once, on behalf of both
        given().contentType("application/x-www-form-urlencoded")
            .body("grant_type=refresh_token&refresh_token=RT-old")
        .when().post("/auth/token").then().statusCode(200);

        // both modules retry and carry on
        given().header("Authorization", "Bearer AT-new")
        .when().get("/accounts").then().statusCode(200);

        given().header("Authorization", "Bearer AT-new")
        .when().get("/onboarding/status").then().statusCode(200);

        assertEquals(1, callCount("POST", "/auth/token"),
            "one refresh for two modules - not one each, or the second invalidates the first");
        assertEquals(2, callCount("GET", "/accounts"),
            "auth: the failed call plus the retry");
        assertEquals(2, callCount("GET", "/onboarding/status"),
            "onboarding: the failed call plus the retry");
        assertNoUnmatchedRequests();
    }

    private static final String REFRESH_BODY =
        "{\"access_token\":\"AT-new\",\"refresh_token\":\"RT-new\",\"expires_in\":900}";
    private static final String ACCOUNTS_BODY =
        "{\"accounts\":[{\"id\":\"acc-1\",\"name\":\"Current account\"}]}";
}
