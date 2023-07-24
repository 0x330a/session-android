package org.thoughtcrime.securesms.dependencies

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.session.libsession.messaging.sending_receiving.notifications.MessageNotifier
import org.thoughtcrime.securesms.notifications.DefaultMessageNotifier
import org.thoughtcrime.securesms.notifications.OptimizedMessageNotifier
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object NotifierModule {

    @Provides
    @Singleton
    fun provideMessageNotifier(): MessageNotifier = OptimizedMessageNotifier(DefaultMessageNotifier())

}