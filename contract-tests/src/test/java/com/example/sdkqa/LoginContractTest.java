package com.example.sdkqa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AUTH-001 - the login path, checked at the HTTP boundary.
 * @author Ishuvir Singh
 */
class LoginContractTest extends SdkContractTest {

    @Test
    @DisplayName("login succeeds, then one accounts call with the new token")
    void loginThenLoadAccounts() {
        // /auth/login only matches when the device is identified - device binding is part of the
        // login contract, so a request without it fails the test instead of sliding through.
        server.stubFor(post(urlEqualTo("/auth/login"))
            .withRequestBody(matchingJsonPath("$.username", equalTo("test.user")))
            .withRequestBody(matchingJsonPath("$.deviceId"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(LOGIN_BODY)));

        server.stubFor(get(urlEqualTo("/accounts"))
            .withHeader("Authorization", equalTo("Bearer AT-login"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(ACCOUNTS_BODY)));

        given().contentType("application/json")
            .body("{\"username\":\"test.user\",\"password\":\"pw\",\"deviceId\":\"device-123\"}")
        .when().post("/auth/login")
        .then().statusCode(200);

        given().header("Authorization", "Bearer AT-login")
        .when().get("/accounts")
        .then().statusCode(200);

        assertEquals(1, callCount("POST", "/auth/login"), "login sent once");
        assertEquals(1, callCount("GET", "/accounts"), "accounts loaded once, not twice");
        assertNoUnmatchedRequests();
    }

    private static final String LOGIN_BODY =
        "{\"access_token\":\"AT-login\",\"refresh_token\":\"RT-login\",\"expires_in\":900}";
    private static final String ACCOUNTS_BODY =
        "{\"accounts\":[{\"id\":\"acc-1\",\"name\":\"Current account\"," +
        "\"iban\":\"NL00TEST0000000001\"}]}";
}
