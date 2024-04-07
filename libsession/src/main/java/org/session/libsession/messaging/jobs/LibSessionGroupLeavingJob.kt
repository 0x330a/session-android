package org.session.libsession.messaging.jobs

import org.session.libsession.messaging.MessagingModuleConfiguration
import org.session.libsession.messaging.utilities.Data
import org.session.libsession.messaging.utilities.UpdateMessageData
import org.session.libsignal.utilities.SessionId

class LibSessionGroupLeavingJob(val sessionId: SessionId): Job {


    override var delegate: JobDelegate? = null
    override var id: String? = null
    override var failureCount: Int = 0
    override val maxFailureCount: Int = 4

    override suspend fun execute(dispatcherName: String) {
        val storage = MessagingModuleConfiguration.shared.storage
        // start leaving
        // create message ID with leaving state
        val messageId = storage.insertGroupInfoLeaving(sessionId) ?: run {
            delegate?.handleJobFailedPermanently(
                this,
                dispatcherName,
                Exception("Couldn't insert GroupInfoLeaving message in leaving group job")
            )
            return
        }
        // do actual group leave request
        // on success
        storage.deleteConversation(/*Group's conversation ID*/ 0)
        // on error
        storage.updateGroupInfoChange(messageId, UpdateMessageData.Kind.GroupErrorQuit)
        delegate?.handleJobSucceeded(this, dispatcherName)
    }

    override fun serialize(): Data =
        Data.Builder()
            .putString(SESSION_ID_KEY, sessionId.hexString())
            .build()

    class Factory : Job.Factory<LibSessionGroupLeavingJob> {
        override fun create(data: Data): LibSessionGroupLeavingJob {
            return LibSessionGroupLeavingJob(
                SessionId.from(data.getString(SESSION_ID_KEY))
            )
        }
    }

    override fun getFactoryKey(): String = KEY

    companion object {
        const val KEY = "LibSessionGroupLeavingJob"
        private const val SESSION_ID_KEY = "SessionId"
    }

}