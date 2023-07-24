package org.session.libsession.snode

import org.session.libsignal.database.LokiAPIDatabaseProtocol

class SnodeModule(val storage: LokiAPIDatabaseProtocol) {

    companion object {
        lateinit var shared: SnodeModule

        val isInitialized: Boolean get() = Companion::shared.isInitialized

        fun configure(storage: LokiAPIDatabaseProtocol) {
            if (isInitialized) { return }
            shared = SnodeModule(storage)
        }
    }
}