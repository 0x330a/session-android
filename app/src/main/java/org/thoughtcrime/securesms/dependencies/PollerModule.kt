package org.thoughtcrime.securesms.dependencies

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.session.libsession.messaging.sending_receiving.pollers.Poller
import org.session.libsession.utilities.ConfigFactoryProtocol
import org.thoughtcrime.securesms.database.LokiAPIDatabase
import org.thoughtcrime.securesms.database.Storage
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object PollerModule {

    @Provides
    @Singleton
    fun provideUserPoller(
        configFactoryProtocol: ConfigFactoryProtocol,
        storage: Storage,
        apiDb: LokiAPIDatabase
    ) = Poller(configFactoryProtocol, storage, apiDb)

}