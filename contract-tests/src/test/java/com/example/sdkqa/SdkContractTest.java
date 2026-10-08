package com.example.sdkqa;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.http.RequestMethod;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;

/**
 * Shared setup for the contract tests.
 *
 * WireMock runs inside the test JVM on a random free port, so the suite needs nothing started
 * beforehand and cannot clash with anything already on 8080.
 *
 * @author Ishuvir Singh
 */
abstract class SdkContractTest {

    protected WireMockServer server;

    @BeforeEach
    void startServer() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
        WireMock.configureFor("localhost", server.port());
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = server.port();
    }

    @AfterEach
    void stopServer() {
        server.stop();
    }

    /**
     * How many times the SDK actually called an endpoint.
     *
     * Counted off the request journal rather than by matching a pattern, so it stays valid
     * across WireMock 3.x and gives the same number the platform runners report.
     */
    protected int callCount(String method, String url) {
        RequestMethod wanted = RequestMethod.fromString(method);
        List<LoggedRequest> calls = server.getAllServeEvents().stream()
            .map(e -> e.getRequest())
            .toList();
        return (int) calls.stream()
            .filter(r -> r.getMethod().equals(wanted))
            .filter(r -> r.getUrl().equals(url))
            .count();
    }

    /**
     * Fails the test if a request arrived that no stub expected.
     *
     * This is the assertion that catches an SDK calling something it should not: a new endpoint,
     * a wrong path, or a body that no longer matches what the backend agreed to.
     */
    protected void assertNoUnmatchedRequests() {
        var unmatched = server.findAllUnmatchedRequests();
        if (!unmatched.isEmpty()) {
            throw new AssertionError("Unmatched requests: " + unmatched);
        }
    }
}
