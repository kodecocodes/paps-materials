package com.raywenderlich.inappreview.preferences

interface InAppReviewPreferences {
    fun hasUserRatedApp(): Boolean

    fun setUserRatedApp(hasRated: Boolean)

    fun hasUserChosenRateLater(): Boolean

    fun setUserChosenRateLater(hasUserChosenRateLater: Boolean)

    fun getRateLaterTime(): Long

    fun setRateLater(time: Long)

}