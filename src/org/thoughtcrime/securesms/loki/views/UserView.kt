package org.thoughtcrime.securesms.loki.views

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_conversation.view.profilePictureView
import kotlinx.android.synthetic.main.view_user.view.*
import network.loki.messenger.R
import org.thoughtcrime.securesms.database.DatabaseFactory
import org.thoughtcrime.securesms.groups.GroupManager
import org.thoughtcrime.securesms.loki.utilities.MentionManagerUtilities
import org.thoughtcrime.securesms.mms.GlideRequests
import org.thoughtcrime.securesms.recipients.Recipient
import org.whispersystems.signalservice.loki.protocol.mentions.MentionsManager

class UserView : LinearLayout {
    var openGroupThreadID: Long = -1 // FIXME: This is a bit ugly

    enum class ActionIndicator {
        None,
        Menu,
        Tick
    }

    // region Lifecycle
    constructor(context: Context) : super(context) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setUpViewHierarchy()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        setUpViewHierarchy()
    }

    private fun setUpViewHierarchy() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val contentView = inflater.inflate(R.layout.view_user, null)
        addView(contentView)
    }
    // endregion

    // region Updating
    fun bind(user: Recipient, glide: GlideRequests, actionIndicator: ActionIndicator, isSelected: Boolean = false) {
        fun getUserDisplayName(publicKey: String?): String? {
            if (publicKey == null || publicKey.isBlank()) {
                return null
            } else {
                var result = DatabaseFactory.getLokiUserDatabase(context).getDisplayName(publicKey)
                val publicChat = DatabaseFactory.getLokiThreadDatabase(context).getPublicChat(openGroupThreadID)
                if (result == null && publicChat != null) {
                    result = DatabaseFactory.getLokiUserDatabase(context).getServerDisplayName(publicChat.id, publicKey)
                }
                return result ?: publicKey
            }
        }
        val threadID = DatabaseFactory.getThreadDatabase(context).getThreadIdFor(user)
        MentionManagerUtilities.populateUserPublicKeyCacheIfNeeded(threadID, context) // FIXME: This is a bad place to do this
        val address = user.address.serialize()
        if (user.isGroupRecipient) {
            if ("Session Public Chat" == user.name || user.address.isRSSFeed) {
                profilePictureView.publicKey = ""
                profilePictureView.displayName = null
                profilePictureView.additionalPublicKey = null
                profilePictureView.isRSSFeed = true
            } else {
                val threadID = GroupManager.getThreadIDFromGroupID(address, context)
                val users = MentionsManager.shared.userPublicKeyCache[threadID]?.toList() ?: listOf()
                val randomUsers = users.sorted() // Sort to provide a level of stability
                val pk = randomUsers.getOrNull(0) ?: ""
                profilePictureView.publicKey = pk
                profilePictureView.displayName = getUserDisplayName(pk)
                val apk = randomUsers.getOrNull(1) ?: ""
                profilePictureView.additionalPublicKey = apk
                profilePictureView.additionalDisplayName = getUserDisplayName(apk)
                profilePictureView.isRSSFeed = false
            }
        } else {
            profilePictureView.publicKey = address
            profilePictureView.displayName = getUserDisplayName(address)
            profilePictureView.additionalPublicKey = null
            profilePictureView.isRSSFeed = false
        }
        actionIndicatorImageView.setImageResource(R.drawable.ic_baseline_edit_24)
        profilePictureView.glide = glide
        profilePictureView.update()
        nameTextView.text = if (user.isGroupRecipient) user.name else getUserDisplayName(address)
        when (actionIndicator) {
            ActionIndicator.None -> {
                actionIndicatorImageView.visibility = View.GONE
            }
            ActionIndicator.Menu -> {
                actionIndicatorImageView.visibility = View.VISIBLE
                actionIndicatorImageView.setImageResource(R.drawable.ic_more_horiz_white)
            }
            ActionIndicator.Tick -> {
                actionIndicatorImageView.visibility = View.VISIBLE
                actionIndicatorImageView.setImageResource(if (isSelected) R.drawable.ic_circle_check else R.drawable.ic_circle)
            }
        }
    }
    // endregion
}