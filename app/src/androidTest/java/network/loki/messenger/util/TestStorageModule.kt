package network.loki.messenger.util

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import network.loki.messenger.libsession_util.ConfigBase
import network.loki.messenger.libsession_util.UserProfile
import org.mockito.kotlin.spy
import org.session.libsession.utilities.ConfigFactoryUpdateListener
import org.session.libsession.utilities.TextSecurePreferences
import org.session.libsession.utilities.WindowDebouncer
import org.thoughtcrime.securesms.database.Storage
import org.thoughtcrime.securesms.database.ThreadDatabase
import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper
import org.thoughtcrime.securesms.dependencies.ConfigFactory
import org.thoughtcrime.securesms.dependencies.ConfigModule
import org.thoughtcrime.securesms.dependencies.StorageModule
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [StorageModule::class, ConfigModule::class]
)
object TestStorageModule {
    @Singleton
    @Provides
    fun provideSpiedStorage(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        configFactory: ConfigFactory,
        threadDatabase: ThreadDatabase,
        debouncer: WindowDebouncer,
    ): Storage {
        val storage = Storage(context, openHelper, configFactory, debouncer)
        threadDatabase.setUpdateListener(storage)
        return spy(storage)
    }

    @Singleton
    @Provides
    fun provideConfigModule(prefs: TextSecurePreferences, storage: Storage) = object: ConfigFactoryUpdateListener {
        override fun notifyUpdates(forConfigObject: ConfigBase) {
            if (forConfigObject is UserProfile && !prefs.getConfigurationMessageSynced()) {
                prefs.setConfigurationMessageSynced(true)
            }
            storage.notifyConfigUpdates(forConfigObject)
        }
    }

}