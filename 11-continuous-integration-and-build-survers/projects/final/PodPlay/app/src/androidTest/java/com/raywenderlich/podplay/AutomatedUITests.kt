package com.raywenderlich.podplay

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.raywenderlich.podplay.ui.PodcastActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy

@RunWith(AndroidJUnit4::class)
class AutomatedUITests {
    @Before
    fun setUp() {
        ActivityScenario.launch(PodcastActivity::class.java)
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())
    }

    @Test
    fun captureScreen() {
        Thread.sleep(5000)
        Screengrab.screenshot("podcast_activity")
    }
}