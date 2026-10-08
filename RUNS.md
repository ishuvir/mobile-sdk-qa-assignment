# Verification log

**Ishuvir Singh**

## Environment

- macOS, Java 17, Maven 3.9
- Date: 8th oct

## Command

```bash
cd contract-tests
mvn test
```

## Output

```text[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.sdkqa.SessionRefreshContractTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.236 s -- in com.example.sdkqa.SessionRefreshContractTest
[INFO] Running com.example.sdkqa.InviteLinkContractTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.030 s -- in com.example.sdkqa.InviteLinkContractTest
[INFO] Running com.example.sdkqa.LoginContractTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.023 s -- in com.example.sdkqa.LoginContractTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

## Not run

The Android Espresso and iOS XCUITest smoke tests need a host app to compile, so they were not executed
here. The Java contract tests are the executable proof.
