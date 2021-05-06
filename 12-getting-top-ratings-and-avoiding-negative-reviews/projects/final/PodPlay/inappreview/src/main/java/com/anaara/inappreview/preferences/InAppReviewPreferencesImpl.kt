package com.anaara.inappreview.preferences

import android.content.SharedPreferences
import javax.inject.Inject

class InAppReviewPreferencesImpl @Inject constructor(
    private val sharedPreferences: SharedPreferences
) : InAppReviewPreferences {

    companion object {
        private const val KEY_HAS_RATED_APP = "hasRatedApp"
        private const val KEY_CHOSEN_RATE_LATER = "hasRatedApp"
        private const val KEY_RATE_LATER_TIME = "hasRatedApp"
    }

}