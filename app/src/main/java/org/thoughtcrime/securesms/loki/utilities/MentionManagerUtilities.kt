package org.thoughtcrime.securesms.loki.utilities

import android.content.Context
import org.thoughtcrime.securesms.database.DatabaseFactory
import org.thoughtcrime.securesms.database.model.MessageRecord
import org.thoughtcrime.securesms.util.TextSecurePreferences
import org.session.libsignal.service.loki.protocol.mentions.MentionsManager

object MentionManagerUtilities {

    fun populateUserPublicKeyCacheIfNeeded(threadID: Long, context: Context) {
        val result = mutableSetOf<String>()
        val recipient = DatabaseFactory.getThreadDatabase(context).getRecipientForThreadId(threadID)
        if (recipient != null && recipient.address.isClosedGroup) {
            val members = DatabaseFactory.getGroupDatabase(context).getGroupMembers(recipient.address.toGroupString(), false).map { it.address.serialize() }
            result.addAll(members)
        } else {
            if (MentionsManager.shared.userPublicKeyCache[threadID] != null) { return }
            val messageDatabase = DatabaseFactory.getMmsSmsDatabase(context)
            val reader = messageDatabase.readerFor(messageDatabase.getConversation(threadID))
            var record: MessageRecord? = reader.next
            while (record != null) {
                result.add(record.individualRecipient.address.serialize())
                try {
                    record = reader.next
                } catch (exception: Exception) {
                    record = null
                }
            }
            reader.close()
            result.add(TextSecurePreferences.getLocalNumber(context))
        }
        MentionsManager.shared.userPublicKeyCache[threadID] = result
    }
}