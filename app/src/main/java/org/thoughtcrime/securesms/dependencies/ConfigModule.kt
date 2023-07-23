package org.thoughtcrime.securesms.dependencies

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.session.libsession.utilities.ConfigFactoryUpdateListener
import javax.inject.Singleton
import org.thoughtcrime.securesms.ApplicationContext as AppContext

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Singleton
    @Provides
    fun configUpdateListener(@ApplicationContext context: Context): ConfigFactoryUpdateListener = context as AppContext

}