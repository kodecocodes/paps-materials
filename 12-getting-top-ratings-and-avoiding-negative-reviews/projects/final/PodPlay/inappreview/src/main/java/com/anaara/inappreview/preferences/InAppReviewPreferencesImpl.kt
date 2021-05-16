package com.anaara.inappreview.preferences

import android.content.SharedPreferences
import androidx.core.content.edit
import javax.inject.Inject

class InAppReviewPreferencesImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : InAppReviewPreferences {

    override fun hasUserRatedApp(): Boolean =
        sharedPreferences.getBoolean(KEY_HAS_RATED_APP, false)

    override fun setUserRatedApp(hasRated: Boolean) =
        sharedPreferences.edit { putBoolean(KEY_HAS_RATED_APP, hasRated) }

    override fun hasUserChosenRateLater(): Boolean =
        sharedPreferences.getBoolean(KEY_CHOSEN_RATE_LATER, false)

    override fun setUserChosenRateLater(hasUserChosenRateLater: Boolean) =
        sharedPreferences.edit { putBoolean(KEY_CHOSEN_RATE_LATER, hasUserChosenRateLater) }

    override fun getRateLaterTime(): Long =
        sharedPreferences.getLong(KEY_RATE_LATER_TIME, System.currentTimeMillis())

    override fun setRateLater(time: Long) =
        sharedPreferences.edit { putLong(KEY_RATE_LATER_TIME, time) }

    companion object {
        private const val KEY_HAS_RATED_APP = "hasRatedApp"
        private const val KEY_CHOSEN_RATE_LATER = "hasRatedApp"
        private const val KEY_RATE_LATER_TIME = "hasRatedApp"
    }

}