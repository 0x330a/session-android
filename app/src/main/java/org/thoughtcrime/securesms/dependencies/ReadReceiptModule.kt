package org.thoughtcrime.securesms.dependencies

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.thoughtcrime.securesms.sskenvironment.ReadReceiptManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ReadReceiptModule {

    @Provides
    @Singleton
    fun provideReadReceipt() = ReadReceiptManager()

}