package org.thoughtcrime.securesms.contacts

import android.content.Context
import org.thoughtcrime.securesms.database.ThreadDatabase
import org.thoughtcrime.securesms.util.AsyncLoader
import org.thoughtcrime.securesms.util.ContactUtilities

class SelectContactsLoader(context: Context, private val threadDatabase: ThreadDatabase, private val usersToExclude: Set<String>) : AsyncLoader<List<String>>(context) {

    override fun loadInBackground(): List<String> {
        val contacts = ContactUtilities.getAllContacts(threadDatabase)
        return contacts.filter {
            !it.isGroupRecipient && !usersToExclude.contains(it.address.toString()) && it.hasApprovedMe()
        }.map {
            it.address.toString()
        }
    }
}