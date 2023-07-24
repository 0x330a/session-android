package org.thoughtcrime.securesms.util

import org.session.libsession.utilities.recipients.Recipient
import org.thoughtcrime.securesms.database.ThreadDatabase

object ContactUtilities {

    @JvmStatic
    fun getAllContacts(threadDatabase: ThreadDatabase): Set<Recipient> {
        val cursor = threadDatabase.conversationList
        val result = mutableSetOf<Recipient>()
        threadDatabase.readerFor(cursor).use { reader ->
            while (reader.next != null) {
                val thread = reader.current
                val recipient = thread.recipient
                result.add(recipient)
            }
        }
        return result
    }

}