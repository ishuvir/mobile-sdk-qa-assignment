package com.example.sdk.qa

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**

 * Full coverage lives in the four specs under specs/. This file shows the shape: a real
 * device, Espresso driving the SDK, and WireMock standing in for the backend.
 * Author: Ishuvir singh
 */
@RunWith(AndroidJUnit4::class)
class SdkSmokeTest {

    @get:Rule val wireMock = WireMockRule(BuildConfig.WIREMOCK_URL)

    @Test
    fun loginThenLoadAccounts() {
        wireMock.loadStubs("specs/auth/AUTH-001")

        onView(withId(R.id.username)).perform(replaceText("test.user"))
        onView(withId(R.id.password)).perform(replaceText("pw"))
        onView(withId(R.id.login_button)).perform(click())

        onView(withId(R.id.accounts_screen)).check(matches(isDisplayed()))

        // the SDK called the backend exactly as AUTH-001 expects
        wireMock.assertCounts("specs/auth/AUTH-001")
    }
}
