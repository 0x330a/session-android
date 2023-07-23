package org.thoughtcrime.securesms.dependencies

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import org.session.libsession.utilities.WindowDebouncer
import org.thoughtcrime.securesms.database.Storage
import org.thoughtcrime.securesms.database.ThreadDatabase
import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides
    @Singleton
    fun provideStorage(
        @ApplicationContext context: Context,
        openHelper: SQLCipherOpenHelper,
        configFactory: ConfigFactory,
        threadDatabase: ThreadDatabase,
        debouncer: WindowDebouncer
    ): Storage {
        val storage = Storage(context, openHelper, configFactory, debouncer)
        threadDatabase.setUpdateListener(storage)
        return storage
    }
}