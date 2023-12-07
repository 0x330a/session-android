package org.thoughtcrime.securesms.dependencies

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.plus
import network.loki.messenger.libsession_util.util.GroupInfo
import org.session.libsession.messaging.sending_receiving.pollers.ClosedGroupPoller
import org.session.libsignal.utilities.SessionId
import java.util.concurrent.ConcurrentHashMap

class PollerFactory(private val scope: CoroutineScope,
                    private val executor: CoroutineDispatcher,
                    private val configFactory: ConfigFactory) {

    private val pollers = ConcurrentHashMap<SessionId, ClosedGroupPoller>()

    fun pollerFor(sessionId: SessionId): ClosedGroupPoller? {
        // Check if the group is currently in our config, don't start if it isn't
        configFactory.userGroups?.getClosedGroup(sessionId.hexString()) ?: return null

        return pollers.getOrPut(sessionId) {
            ClosedGroupPoller(scope + SupervisorJob(), executor, sessionId, configFactory)
        }
    }

    fun startAll() {
        configFactory.userGroups?.allClosedGroupInfo()?.filterNot(GroupInfo.ClosedGroupInfo::invited)?.forEach {
            pollerFor(it.groupSessionId)?.start()
        }
    }

    fun stopAll() {
        pollers.forEach { (_, poller) ->
            poller.stop()
        }
    }

    fun updatePollers() {
        val currentGroups = configFactory.userGroups?.allClosedGroupInfo()?.filterNot(GroupInfo.ClosedGroupInfo::invited) ?: return
        val toRemove = pollers.filter { (id, _) -> id !in currentGroups.map { it.groupSessionId } }
        toRemove.forEach { (id, _) ->
            pollers.remove(id)?.stop()
        }
        startAll()
    }

}