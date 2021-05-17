package com.anaara.inappreview.di

import com.anaara.inappreview.preferences.InAppReviewPreferences
import com.anaara.inappreview.preferences.InAppReviewPreferencesImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class InAppReviewBinds {

    /**
     * Provides Preferences wrapper.
     * */
    @Binds
    @Singleton
    abstract fun bindInAppReviewPreferences(
        inAppReviewPreferencesImpl: InAppReviewPreferencesImpl
    ): InAppReviewPreferences

}