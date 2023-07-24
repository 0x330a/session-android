package org.thoughtcrime.securesms.dependencies

import dagger.Binds
import dagger.Module
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.session.libsession.messaging.sending_receiving.notifications.MessageNotifier
import org.session.libsession.messaging.sending_receiving.pollers.Poller
import org.session.libsession.utilities.AppTextSecurePreferences
import org.session.libsession.utilities.ConfigFactoryProtocol
import org.session.libsession.utilities.TextSecurePreferences
import org.session.libsignal.database.LokiAPIDatabaseProtocol
import org.thoughtcrime.securesms.components.TypingStatusSender
import org.thoughtcrime.securesms.database.LokiAPIDatabase
import org.thoughtcrime.securesms.logging.PersistentLogger
import org.thoughtcrime.securesms.repository.ConversationRepository
import org.thoughtcrime.securesms.repository.DefaultConversationRepository
import org.thoughtcrime.securesms.service.ExpiringMessageManager
import org.thoughtcrime.securesms.sskenvironment.ReadReceiptManager
import org.thoughtcrime.securesms.sskenvironment.TypingStatusRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Singleton
    @Binds
    abstract fun bindTextSecurePreferences(preferences: AppTextSecurePreferences): TextSecurePreferences

    @Singleton
    @Binds
    abstract fun bindConversationRepository(repository: DefaultConversationRepository): ConversationRepository

    @Singleton
    @Binds
    abstract fun bindConfigFactoryProtocol(configFactory: ConfigFactory): ConfigFactoryProtocol

    @Singleton
    @Binds
    abstract fun bindLokiAPIDatabaseProtocol(lokiAPIDatabase: LokiAPIDatabase): LokiAPIDatabaseProtocol

}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AppComponent {
    fun getPrefs(): TextSecurePreferences
    fun userPoller(): Poller
    fun messageNotifier(): MessageNotifier
    fun typingStatusRepository(): TypingStatusRepository
    fun typingStatusSender(): TypingStatusSender
    fun readReceiptManager(): ReadReceiptManager
    fun expiringMessageManager(): ExpiringMessageManager
    fun persistentLogger(): PersistentLogger
}