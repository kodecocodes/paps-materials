package com.anaara.inappreview.di

import android.content.Context
import android.content.SharedPreferences
import com.anaara.inappreview.preferences.InAppReviewPreferencesImpl.Companion.KEY_IN_APP_REVIEW_PREFERENCES
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


/**
 * Provides In App Review Android-based dependencies.
 * */
@Module
@InstallIn(SingletonComponent::class)
class InAppReviewProviders {

    /**
     * Provides In App Review Preferences.
     * */
    @Provides
    @Singleton
    fun provideInAppReviewPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences(KEY_IN_APP_REVIEW_PREFERENCES, Context.MODE_PRIVATE)
    }

    /**
     * Provides Review Manager
     * */
    @Provides
    @Singleton
    fun provideReviewManager(@ApplicationContext context: Context): ReviewManager {
        return ReviewManagerFactory.create(context)
    }
}