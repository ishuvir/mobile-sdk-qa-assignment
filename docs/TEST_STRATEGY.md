# Test strategy

## The short version

I don't want a big end-to-end suite. It's slow, it breaks, and when it fails it rarely tells you which
team broke it. What it can't see is a change in one module breaking another — and that is the problem
this platform actually has.

So: keep the UI suite thin, and put the effort into tests that check what the SDK sends and how often.
Those run in seconds and they fail loudly and specifically.

The team already has WireMock, Espresso and XCUITest. I'm not asking anyone to learn a new tool.

## Levels, and who owns each

These are the levels across the platform, not five things I would run myself. Module teams own their
unit tests. App teams own their suites. The middle is mine.

| Level | What it covers | Tools | Who owns it | When |
|---|---|---|---|---|
| Module (unit) | Link parsing, token expiry, request building | XCTest, JUnit5 | Module teams | Every PR |
| API / contract | Login, token, accounts, invite APIs; error codes | WireMock, REST Assured | Platform QA | Every PR |
| Integration and app journeys | Modules together on one session, plus the app's flows | Espresso, XCUITest, WireMock | Platform QA with the app team | PR for changed modules, nightly for all |
| Consumer apps | The released SDK inside real apps | Their smoke suites | App teams | Every app PR |
| Exploratory | Real links, real networks, OS betas | Real devices | Platform QA | Each release |

## What I would build first

The contract and integration level. Unit tests already exist per module, and the consumer suites belong
to other teams, so the gap worth closing is the middle. It is also where the brief's problems live.

Four specs to start. Four is enough to show the pattern and few enough that a reviewer can read them all.

| Spec | The assertion that matters |
|---|---|
| AUTH-001 | Login sent once, accounts loaded once |
| AUTH-002 | GET /accounts is 0 after a failed login |
| AUTH-002 | POST /auth/token is 1 across two modules on one session |
| LINK-001 | Invite fetched once; accept is 0 |

The last two are the ones I care about. AUTH-002 puts auth and onboarding on the same session and makes
both hit a 401 at the same moment — one refresh, not one each. That is the shape that catches a
module-pair regression, which a single-module test cannot see. LINK-001 asserts the SDK never accepts an
invite on the user's behalf, which in a banking app matters more than whether the screen appeared.

## Why WireMock, and why it stays

People ask this once a real backend exists. WireMock isn't standing in for the backend. It is what makes
two things testable that a live server is not.

One: **counting calls.** "Exactly one refresh for two concurrent 401s" cannot be verified against a real
API. Nor can you inject a 300ms delay into production to widen the race window.

Two: **forcing the failure you want.** Locked account, expired invite, revoked device key, tampered
token. A real server will not produce those on demand.

What the real backend is for is checking that my stubs still match it. That is the nightly OpenAPI check.
Without it a green suite can quietly stop reflecting reality, and green-but-wrong is worse than red.

## Automate, go native, or explore

**Automate:** API contracts and error codes, token and session lifetime, and data written by the previous
release still being readable.

**Native-only:** keychain and keystore, app lifecycle, permissions, App Links and Universal Links. These
genuinely differ per platform, and a shared abstraction would hide the difference the test exists to find.

**Explore:** an invite link opened from mail, SMS or WhatsApp; the app-not-installed path; a network switch
mid-flow; OS betas; a module's first integration into a consumer app. Anything recurring becomes a spec.

## Keeping it fast

- Contract tests own their WireMock in-process. `mvn test` needs nothing started and no port reserved.
- Where WireMock runs externally: one instance per CI shard, reset before each test, and an unmatched
  request fails the test rather than passing quietly.
- Integration tests run only for the modules a PR touches.
- 15 minutes is the PR budget at p90. Blowing it is a defect, not an inconvenience.
- Every spec has an owner.

## Assumptions

- Native SDK, Kotlin and Swift. Modules versioned separately with semver.
- The HTTP client, secure store and config sit behind interfaces, so tests can point them at WireMock and
  seed a session. If that isn't true today, it is my first recommendation.
- A test session can be seeded without going through the UI.

## Risks

**The stubs drift.** The one that would worry me most. Everything else I can see failing in CI, but if the
fake stops matching the backend the tests go green and lie. Nightly OpenAPI check, no exceptions.

**Bugs get found later.** A thin UI layer means some detection moves into the apps team's pipeline.
Nothing is lost, but it is found in someone else's build. Acceptable only because they run on every PR.

**Quarantine hides something real.** "Flaky" sometimes means an intermittent product bug. Owner, ticket,
14-day expiry — and never for login, session or payment.

## Trade-offs I'm accepting

- One spec, two or three runners. The maintenance lands on the platform team, on me. That is why the
  pilot comes before rolling it out everywhere.
- I don't test every version combination. RC, previous minor, oldest still in use. If nobody is on some
  odd pairing I haven't tested it, and I would say so rather than pretend.
- Four specs prove the pattern. The rest are prioritised in `WHAT_I_WOULD_ADD_NEXT.md`, not forgotten.

## What this doesn't cover

| Not covered | Whose job |
|---|---|
| Does the screen render | Espresso and XCUITest in the app repo |
| Is the backend API itself correct | The backend team |
| Does a specific app work with a new SDK | That app team, against my version set |
| Does an old SDK version still work | The version matrix — designed, not built |
| Real keychain and keystore behaviour | Native tests on real devices |
