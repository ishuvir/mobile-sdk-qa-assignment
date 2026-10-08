import XCTest

/// Full coverage lives in the four specs under specs/. This file shows the shape: a real
/// simulator, XCUITest driving the SDK, and WireMock standing in for the backend.
///Author: Ishuvir singh

final class SdkSmokeUITests: XCTestCase {

    let wireMock = WireMockClient(
        url: URL(string: ProcessInfo.processInfo.environment["WIREMOCK_URL"] ?? "http://localhost:8080")!)

    @MainActor
    func testLoginThenLoadAccounts() async throws {
        try await wireMock.reset()
        try await wireMock.loadStubs("specs/auth/AUTH-001")

        let app = XCUIApplication()
        app.launchEnvironment = ["SDK_TEST_BASE_URL": wireMock.url.absoluteString]
        app.launch()

        app.textFields["username"].tap()
        app.textFields["username"].typeText("test.user")
        app.secureTextFields["password"].tap()
        app.secureTextFields["password"].typeText("pw")
        app.buttons["login_button"].tap()

        XCTAssertTrue(app.otherElements["accounts_screen"].waitForExistence(timeout: 10))

        // the SDK called the backend exactly as AUTH-001 expects
        try await wireMock.assertCounts("specs/auth/AUTH-001")
    }
}
