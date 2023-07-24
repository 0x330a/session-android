package org.thoughtcrime.securesms.dependencies

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.thoughtcrime.securesms.components.TypingStatusSender
import org.thoughtcrime.securesms.sskenvironment.TypingStatusRepository
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object TypingStatusModule {

    @Provides
    @Singleton
    fun provideTypingStatusRepository() = TypingStatusRepository()

    @Provides
    @Singleton
    fun provideTypingStatusSender(@ApplicationContext context: Context) = TypingStatusSender(context)

}