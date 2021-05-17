package com.anaara.inappreview

import android.app.Activity

interface InAppReviewManager {

    fun startReview(activity: Activity)

    fun isEligibleForReview(): Boolean
}