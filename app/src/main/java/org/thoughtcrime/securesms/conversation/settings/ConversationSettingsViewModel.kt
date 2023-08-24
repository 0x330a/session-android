package org.thoughtcrime.securesms.conversation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.launch
import org.session.libsession.database.StorageProtocol
import org.session.libsession.utilities.Address
import org.session.libsession.utilities.TextSecurePreferences

class ConversationSettingsViewModel(
    val threadId: Long,
    private val storage: StorageProtocol,
    private val prefs: TextSecurePreferences
): ViewModel() {

    val recipient get() = storage.getRecipientForThread(threadId)

    fun isPinned() = storage.isPinned(threadId)

    fun togglePin() = viewModelScope.launch {
        val isPinned = storage.isPinned(threadId)
        storage.setPinned(threadId, !isPinned)
    }

    fun autoDownloadAttachments() = recipient?.let { recipient -> storage.shouldAutoDownloadAttachments(recipient) } ?: false

    fun setAutoDownloadAttachments(shouldDownload: Boolean) {
        recipient?.let { recipient -> storage.setAutoDownloadAttachments(recipient, shouldDownload) }
    }

    fun isUserGroupAdmin(): Boolean = recipient?.let { recipient ->
        if (!recipient.isLegacyClosedGroupRecipient || !recipient.isClosedGroupRecipient) return@let false
        val localUserAddress = prefs.getLocalNumber() ?: return@let false
        val group = storage.getGroup(recipient.address.toGroupString())
        group?.admins?.contains(Address.fromSerialized(localUserAddress)) ?: false // this will have to be replaced for new closed groups
    } ?: false

    fun clearMessages(forAll: Boolean) {
        if (forAll && !isUserGroupAdmin()) return

        if (!forAll) {
            viewModelScope.launch {
                storage.clearMessages(threadId)
            }
        } else {
            // do a send message here and on success do a clear messages
            viewModelScope.launch {
                storage.clearMessages(threadId)
            }
        }
    }

    // DI-related
    @dagger.assisted.AssistedFactory
    interface AssistedFactory {
        fun create(threadId: Long): Factory
    }
    class Factory @AssistedInject constructor(
        @Assisted private val threadId: Long,
        private val storage: StorageProtocol,
        private val prefs: TextSecurePreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ConversationSettingsViewModel(threadId, storage, prefs) as T
        }
    }

}