package network.loki.messenger.util

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication
import org.thoughtcrime.securesms.dependencies.DatabaseModule

class HiltTestRunner: AndroidJUnitRunner() {

    companion object {
        init {
            DatabaseModule.init()
        }
    }

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}