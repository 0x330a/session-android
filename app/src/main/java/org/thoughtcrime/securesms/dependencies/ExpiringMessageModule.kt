package org.thoughtcrime.securesms.dependencies

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.thoughtcrime.securesms.database.MmsDatabase
import org.thoughtcrime.securesms.database.MmsSmsDatabase
import org.thoughtcrime.securesms.database.SmsDatabase
import org.thoughtcrime.securesms.service.ExpiringMessageManager
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExpiringMessageModule {

    @Provides
    @Singleton
    fun provideExpiringMessageManager(
        @ApplicationContext context: Context,
        sms: SmsDatabase,
        mms: MmsDatabase,
        mmsSms: MmsSmsDatabase
    ) = ExpiringMessageManager(context, sms, mms, mmsSms)

}