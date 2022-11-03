package org.thoughtcrime.securesms.conversation.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import network.loki.messenger.R
import network.loki.messenger.databinding.ActivityConversationSettingsBinding
import org.session.libsignal.utilities.Log
import org.thoughtcrime.securesms.MediaOverviewActivity
import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity
import org.thoughtcrime.securesms.conversation.v2.ConversationActivityV2
import org.thoughtcrime.securesms.database.LokiThreadDatabase
import org.thoughtcrime.securesms.database.ThreadDatabase
import org.thoughtcrime.securesms.mms.GlideApp
import javax.inject.Inject

@AndroidEntryPoint
class ConversationSettingsActivity: PassphraseRequiredActionBarActivity(), View.OnClickListener {

    companion object {
        // used to trigger displaying conversation search in calling parent activity
        const val RESULT_SEARCH = 22
    }

    lateinit var binding: ActivityConversationSettingsBinding

    private val groupOptions: List<View>
    get() = with(binding) {
        listOf(
            groupMembers,
            groupMembersDivider.root,
            editGroup,
            editGroupDivider.root,
            leaveGroup,
            leaveGroupDivider.root
        )
    }

    @Inject lateinit var threadDb: ThreadDatabase
    @Inject lateinit var lokiThreadDb: LokiThreadDatabase
    @Inject lateinit var viewModelFactory: ConversationSettingsViewModel.AssistedFactory
    val viewModel: ConversationSettingsViewModel by viewModels {
        val threadId = intent.getLongExtra(ConversationActivityV2.THREAD_ID, -1L)
        if (threadId == -1L) {
            finish()
        }
        viewModelFactory.create(threadId)
    }

    private val notificationActivityCallback = registerForActivityResult(ConversationNotificationSettingsActivityContract()) {
        updateRecipientDisplay()
    }

    override fun onCreate(savedInstanceState: Bundle?, ready: Boolean) {
        super.onCreate(savedInstanceState, ready)
        binding = ActivityConversationSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.profilePictureView.root.glide = GlideApp.with(this)
        updateRecipientDisplay()
        binding.searchConversation.setOnClickListener(this)
        binding.allMedia.setOnClickListener(this)
        binding.pinConversation.setOnClickListener(this)
        binding.notificationSettings.setOnClickListener(this)
        binding.back.setOnClickListener(this)
        binding.autoDownloadMediaSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setTrusted(isChecked)
            updateRecipientDisplay()
        }
    }

    private fun updateRecipientDisplay() {
        val recipient = viewModel.recipient ?: return
        // Setup profile image
        binding.profilePictureView.root.update(recipient)
        // Setup name
        binding.conversationName.text = when {
            recipient.isLocalNumber -> getString(R.string.note_to_self)
            else -> recipient.toShortString()
        }
        // Setup group description (if group)
        binding.conversationSubtitle.isVisible = recipient.isClosedGroupRecipient.apply {
            binding.conversationSubtitle.text = "TODO: This is a test for group descriptions"
        }

        // Toggle group-specific settings
        val areGroupOptionsVisible = recipient.isClosedGroupRecipient
        groupOptions.forEach { v ->
            v.isVisible = areGroupOptionsVisible
        }

        // Group admin settings
        val isUserGroupAdmin = areGroupOptionsVisible && viewModel.isUserGroupAdmin()
        with (binding) {
            groupMembersDivider.root.isVisible = areGroupOptionsVisible && !isUserGroupAdmin
            groupMembers.isVisible = areGroupOptionsVisible && !isUserGroupAdmin
            adminControlsGroup.isVisible = isUserGroupAdmin
            deleteGroup.isVisible = isUserGroupAdmin
            clearMessagesDivider.root.isVisible = isUserGroupAdmin
        }

        // Set pinned state
        binding.pinConversation.setText(
            if (viewModel.isPinned()) R.string.conversation_settings_unpin_conversation
            else R.string.conversation_settings_pin_conversation
        )

        // Set auto-download state
        val trusted = viewModel.isTrusted()
        binding.autoDownloadMediaSwitch.isChecked = trusted

        // Set notification type
        val notifyTypes = resources.getStringArray(R.array.notify_types)
        val summary = notifyTypes.getOrNull(recipient.notifyType)
        binding.notificationsValue.text = summary
    }

    override fun onClick(v: View?) {
        when {
            v === binding.searchConversation -> {
                setResult(RESULT_SEARCH)
                finish()
            }
            v === binding.allMedia -> {
                val threadRecipient = viewModel.recipient ?: return
                val intent = Intent(this, MediaOverviewActivity::class.java).apply {
                    putExtra(MediaOverviewActivity.ADDRESS_EXTRA, threadRecipient.address)
                }
                startActivity(intent)
            }
            v === binding.pinConversation -> {
                viewModel.togglePin().invokeOnCompletion { e ->
                    if (e != null) {
                        // something happened
                        Log.e("ConversationSettings", "Failed to toggle pin on thread", e)
                    } else {
                        updateRecipientDisplay()
                    }
                }
            }
            v === binding.notificationSettings -> {
                notificationActivityCallback.launch(viewModel.threadId)
            }
            v === binding.back -> onBackPressed()
        }
    }
}