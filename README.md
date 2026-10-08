# Mobile SDK - quality engineering exercise

**Ishuvir Singh** - submission for the Senior Mobile QA Engineer (Platform / SDK) role.

## The idea in one line

A capability is described once in `specs/` and proved by a Java contract test that runs in seconds with
nothing installed. The same expectation is what the Espresso and XCUITest runners read on Android and
iOS.

## Running the tests

You need **Java 17 or later** and **Maven**. Nothing else - no Docker, no server to start, no emulator.
WireMock runs inside the test JVM on a random free port, so it cannot clash with anything.

```bash
java -version      # need 17 or later
mvn -version       # if missing:  brew install maven openjdk
```

### 1. Get the code

```bash
git clone 
cd mobile-sdk-qa-assignment

# or, from the zip
unzip mobile-sdk-qa-assignment.zip && cd mobile-sdk-qa-assignment
```

### 2. Run the contract tests

```bash
cd contract-tests
mvn test
```

Expect **Tests run: 4, Failures: 0, Errors: 0, Skipped: 0**.

For a rendered report:

```bash
mvn surefire-report:report && open target/site/surefire-report.html
# or just read the text:  cat target/surefire-reports/*.txt
```

### What you cannot run here

The Android and iOS files (`android/`, `ios/`) are one smoke test each. They need a host app to compile
against, so they were not executed - see *Not done, and why* below.

## What is where

| Path | What it is |
|---|---|
| presentation/ | The 7-slide deck: strategy, CI/CD, two examples, investigation |
| specs/ | Four behaviour specs, numbered in sequence |
| contract-tests/ | Java + REST Assured + WireMock. Runs with `mvn test` |
| android/ | One Espresso smoke test - the Android half of the same behaviour |
| ios/ | One XCUITest smoke test - the iOS half of the same behaviour |
| docs/ | Test strategy, and the scenarios I would add next |
| .github/workflows/ | PR and release pipelines |
| RUNS.md | The verification log: what was run, and what was not |

## The specs

| Spec | Scenario | The assertion that matters |
|---|---|---|
| AUTH-001 | Login with username and password | Login sent once, accounts loaded once - not twice |
| AUTH-002 | Auth and Onboarding both get a 401 together | `POST /auth/token` is **1** across **both modules** - not one each |
| LINK-001 | A joint-holder invite link is opened | Fetched once; `POST /invite/.*/accept` is **0** - never auto-accepted |
| LINK-002 | A tampered or unsigned invite token | Zero calls to `/invite/*` - rejected on the device |

Four is deliberate: two cover the SDK calling the backend, two cover what it decides with no backend
involved. AUTH-002 is the one I would keep if I could keep only one - it puts two modules on the same
session, has both hit an expired token at the same moment, and asserts a single refresh between them.
A single-module test cannot see that class of failure. More scenarios are listed in
`docs/WHAT_I_WOULD_ADD_NEXT.md`.

## Design decisions

1. **Counts, not just status codes.** The interesting banking bugs are the calls that should not happen:
   a second refresh that invalidates the first token, a duplicate account fetch, an invite accepted
   without the customer. WireMock records every request, so those assertions are cheap.
2. **The stub checks the request.** `/auth/login` only matches with a `deviceId` in the body. If the SDK
   stops sending device details the request does not match, and the test fails instead of passing
   silently.
3. **Header matching, not WireMock scenarios.** Scenario state advances on every matched call, which
   makes concurrent 401s non-deterministic. Matching on the bearer token keeps the test honest.
4. **The refresh delay is deliberate.** The refresh stub answers after 300ms. Without that window the two
   requests rarely overlap and the test passes whether or not the refresh is serialised - which is worse
   than having no test at all.
5. **The tampered-token test has no stubs at all.** With nothing registered, any call the SDK makes lands
   in the unmatched pile and fails the test, so 'nothing left the device' is enforced by the framework.
6. **WireMock stays when a real backend exists.** It is what makes call counting and forced error paths
   testable. The real API validates the stubs nightly against its OpenAPI spec.

## Assumptions, risks and trade-offs

- **Assumed:** native SDK in Kotlin and Swift; modules versioned with semver; the HTTP client, secure
  store and config sit behind interfaces so they can be pointed at WireMock and seed a session.
- **Risks:** the stubs drift from the real backend (nightly OpenAPI check, no exceptions); a thin UI
  layer means the apps team suite has to catch what E2E used to; quarantine can hide a real bug.
- **Trade-offs:** shared specs cost maintenance on more than one runner; a small compatibility matrix
  leaves rare combinations untested by design; four specs prove the pattern but leave scenarios unwritten.

## Not done, and why

- The Android and iOS files are one smoke test each and need a host app to compile. The Java contract
  tests are the executable proof.
- Version compatibility (a new SDK against an old app) is designed in the deck - the version matrix and
  the upgrade test - but not implemented here.
- Real-device link handling (App Links and Universal Links verification, and the app-not-installed path)
  needs real devices and stays exploratory.
- The CI workflows are samples: runner labels and the device-cloud step are placeholders.

## AI usage

AI helped draft the structure and scaffold the test code. I chose the scenarios, wrote the acceptance
criteria, and ran the contract tests on macOS with Java 17 - the output is in `RUNS.md`. The design
reasoning is mine, from testing banking SDKs on both platforms.
