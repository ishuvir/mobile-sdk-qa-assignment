# What I would add next

Four specs prove the pattern. These are the next four, in the order I would write them.

| Next spec | Scenario | The assertion that matters |
|---|---|---|
| AUTH-003 | A failed login | `GET /accounts` is 0 - no data fetched after a failed login |
| AUTH-004 | The refresh token is rejected | One refresh attempt, session cleared, no retry loop |
| LINK-003 | An expired invite, or one already used | Clear message, and `POST /invite/.*/accept` stays 0 |
| LINK-004 | Invite for another person while a different user is logged in | Asks them to log out; never accepted on the wrong account |

Then: a telemetry contract spec (event names and payload shape), and an upgrade spec so a token written
by N-1 is still readable by N.

I stopped at four because a reviewer should be able to read the whole suite in five minutes. Two specs
cover the SDK calling the backend, two cover what it decides with nothing sent.
