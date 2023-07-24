package org.thoughtcrime.securesms.messagerequests

import android.content.Context
import android.database.Cursor
import org.thoughtcrime.securesms.database.ThreadDatabase
import org.thoughtcrime.securesms.util.AbstractCursorLoader

class MessageRequestsLoader(context: Context, private val threadDatabase: ThreadDatabase) : AbstractCursorLoader(context) {

    override fun getCursor(): Cursor {
        return threadDatabase.unapprovedConversationList
    }
}