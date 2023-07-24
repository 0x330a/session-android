package org.thoughtcrime.securesms.dependencies

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.session.libsession.messaging.sending_receiving.notifications.MessageNotifier
import org.session.libsession.messaging.sending_receiving.pollers.Poller
import org.thoughtcrime.securesms.database.LokiThreadDatabase
import org.thoughtcrime.securesms.database.MmsSmsDatabase
import org.thoughtcrime.securesms.database.ThreadDatabase
import org.thoughtcrime.securesms.notifications.DefaultMessageNotifier
import org.thoughtcrime.securesms.notifications.OptimizedMessageNotifier
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NotifierModule {

    @Provides
    @Singleton
    fun provideMessageNotifier(
        poller: Poller,
        threadDb: ThreadDatabase,
        mmsSmsDb: MmsSmsDatabase,
        lokiThreadDb: LokiThreadDatabase,
    ): MessageNotifier = OptimizedMessageNotifier(
        DefaultMessageNotifier(threadDb,mmsSmsDb,lokiThreadDb),
        poller
    )

}