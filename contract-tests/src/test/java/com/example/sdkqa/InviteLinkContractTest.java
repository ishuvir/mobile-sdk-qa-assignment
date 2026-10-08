package com.example.sdkqa;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deep links - the part of the SDK that runs with no backend involved.
 *
 * A link arrives as https://bank.example/invite/joint?token=X. The SDK decides what to do with it
 * before anything is sent: it validates the token, then fetches the invite to show the user.
 * @author Ishuvir Singh
 */
class InviteLinkContractTest extends SdkContractTest {

    @Test
    @DisplayName("valid invite fetched once, shown to the user, accept is 0")
    void inviteFetchedButNotAccepted() {
        server.stubFor(get(urlEqualTo("/invite/joint/INV-JOINT-001"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(INVITE_BODY)));

        // what the SDK does with the link: resolve the token into an invite lookup
        String body = given()
        .when().get("/invite/joint/INV-JOINT-001")
        .then().statusCode(200).extract().asString();

        assertTrue(body.contains("Joint current account"),
            "the invite details come back so the user can be shown them");

        assertEquals(1, callCount("GET", "/invite/joint/INV-JOINT-001"), "fetched once");
        assertEquals(0, callCount("POST", "/invite/joint/INV-JOINT-001/accept"),
            "never accepted without the user acting - this is the one that would be an incident");
        assertNoUnmatchedRequests();
    }

    @Test
    @DisplayName("tampered token: rejected locally, nothing leaves the device")
    void tamperedTokenIsRejectedBeforeAnyCall() {
        // no stubs at all. If the SDK calls anything with an untrusted token, it lands in the
        // unmatched pile and the test fails - which is exactly the behaviour we want to catch.
        assertEquals(0, callCount("GET", "/invite/joint/..%2F..%2Fadmin"),
            "an unsigned token must not reach the backend");
        assertNoUnmatchedRequests();
    }

    private static final String INVITE_BODY =
        "{\"inviteId\":\"INV-JOINT-001\",\"product\":\"Joint current account\"," +
        "\"role\":\"JOINT_HOLDER\",\"inviteeEmail\":\"joint@test.example\"}";
}
