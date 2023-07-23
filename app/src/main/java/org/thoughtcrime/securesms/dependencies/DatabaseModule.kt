package org.thoughtcrime.securesms.dependencies

import android.content.Context
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.session.libsession.database.MessageDataProvider
import org.session.libsession.messaging.MessagingModuleConfiguration
import org.session.libsession.utilities.TextSecurePreferences
import org.session.libsession.utilities.WindowDebouncer
import org.thoughtcrime.securesms.attachments.DatabaseAttachmentProvider
import org.thoughtcrime.securesms.crypto.AttachmentSecret
import org.thoughtcrime.securesms.crypto.AttachmentSecretProvider
import org.thoughtcrime.securesms.crypto.DatabaseSecretProvider
import org.thoughtcrime.securesms.crypto.KeyPairUtilities
import org.thoughtcrime.securesms.database.*
import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper
import org.thoughtcrime.securesms.sskenvironment.ProfileManager
import org.thoughtcrime.securesms.webrtc.CallMessageProcessor
import java.util.Timer
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @JvmStatic
    fun init() {
        System.loadLibrary("sqlcipher")
    }

    @Provides
    @Singleton
    fun provideWindowDebouncer(@ApplicationContext context: Context) =
        WindowDebouncer(1000, Timer())

    @Provides
    @Singleton
    fun provideAttachmentSecret(@ApplicationContext context: Context) =
        AttachmentSecretProvider.getInstance(context).orCreateAttachmentSecret

    @Provides
    @Singleton
    fun provideOpenHelper(@ApplicationContext context: Context): SQLCipherOpenHelper {
        val dbSecret = DatabaseSecretProvider(context).orCreateDatabaseSecret
        SQLCipherOpenHelper.migrateSqlCipher3To4IfNeeded(context, dbSecret)
        return SQLCipherOpenHelper(context, dbSecret)
    }

    @Provides
    @Singleton
    fun provideSmsDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = SmsDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideMmsDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = MmsDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideAttachmentDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        attachmentSecret: AttachmentSecret,
        debouncer: WindowDebouncer
    ) = AttachmentDatabase(context, openHelper, attachmentSecret, debouncer)

    @Provides
    @Singleton
    fun provideMediaDatbase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = MediaDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideThread(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = ThreadDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideMmsSms(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = MmsSmsDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideDraftDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = DraftDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun providePushDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = PushDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideGroupDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = GroupDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideRecipientDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = RecipientDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideGroupReceiptDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = GroupReceiptDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun searchDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = SearchDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideLokiApiDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = LokiAPIDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideLokiMessageDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = LokiMessageDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideLokiThreadDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = LokiThreadDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideLokiUserDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = LokiUserDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideLokiBackupFilesDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = LokiBackupFilesDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideSessionJobDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = SessionJobDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideSessionContactDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = SessionContactDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideBlindedIdMappingDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = BlindedIdMappingDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideGroupMemberDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = GroupMemberDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideReactionDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = ReactionDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideEmojiSearchDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ) = EmojiSearchDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideAttachmentProvider(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ): MessageDataProvider = DatabaseAttachmentProvider(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun provideConfigDatabase(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        debouncer: WindowDebouncer
    ): ConfigDatabase = ConfigDatabase(context, openHelper, debouncer)

    @Provides
    @Singleton
    fun profileManager(@ApplicationContext context: Context, configFactory: ConfigFactory) =
        ProfileManager(context, configFactory)

    @Provides
    @Singleton
    fun messagingModuleConfiguration(
        @ApplicationContext context: Context,
        storage: Storage,
        messageDataProvider: MessageDataProvider,
        configFactory: ConfigFactory
    ) = MessagingModuleConfiguration(
        context,
        storage,
        messageDataProvider,
        { KeyPairUtilities.getUserED25519KeyPair(context) },
        configFactory
    )

    @Provides
    @Singleton
    fun callMessageProcessor(
        @ApplicationContext context: Context,
        prefs: TextSecurePreferences,
        storage: Storage,
    ) = CallMessageProcessor(context, prefs, ProcessLifecycleOwner.get().lifecycle, storage)

}