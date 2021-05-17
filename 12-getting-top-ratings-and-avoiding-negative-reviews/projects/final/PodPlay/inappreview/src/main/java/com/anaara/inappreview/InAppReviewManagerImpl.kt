package com.anaara.inappreview

import android.app.Activity
import android.content.Context
import android.util.Log
import com.anaara.inappreview.preferences.InAppReviewPreferences
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.tasks.Task
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.abs

class InAppReviewManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reviewManager: ReviewManager,
    private val inAppReviewPreferences: InAppReviewPreferences
) : InAppReviewManager {

    companion object {
        private const val KEY_REVIEW = "reviewFlow"
    }

    private var reviewInfo: ReviewInfo? = null

    init {
        if (isEligibleForReview()) {
            reviewManager.requestReviewFlow().addOnCompleteListener {
                if (it.isComplete && it.isSuccessful) {
                    this.reviewInfo = it.result
                }
            }

        }
    }

    override fun startReview(activity: Activity) {
        val myReviewInfo = reviewInfo
        myReviewInfo.let {
            if (myReviewInfo != null) {
                reviewManager.launchReviewFlow(activity, myReviewInfo)
                    .addOnCompleteListener { reviewFlow ->
                        onReviewFlowLaunchCompleted(reviewFlow)
                    }
            }
        }
    }

    private fun onReviewFlowLaunchCompleted(reviewFlow: Task<Void>) {
        if (reviewFlow.isSuccessful) {
            logSuccess()
        }

    }

    private fun logSuccess() {
        if (BuildConfig.DEBUG) {
            Log.d(KEY_REVIEW, "Review complete!")
        }
    }

    override fun isEligibleForReview(): Boolean {
        return (!inAppReviewPreferences.hasUserRatedApp() &&
                !inAppReviewPreferences.hasUserChosenRateLater()
                || (inAppReviewPreferences.hasUserChosenRateLater() && enoughTimePassed()))
    }

    private fun enoughTimePassed(): Boolean {
        val rateLaterTimeStamp = inAppReviewPreferences.getRateLaterTime()

        return abs(rateLaterTimeStamp - System.currentTimeMillis()) >= TimeUnit.DAYS.toMillis(14)
    }
}