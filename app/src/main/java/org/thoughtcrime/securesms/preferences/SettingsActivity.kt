package org.thoughtcrime.securesms.preferences

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.SparseArray
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.view.isVisible
import dagger.hilt.android.AndroidEntryPoint
import network.loki.messenger.BuildConfig
import network.loki.messenger.R
import network.loki.messenger.databinding.ActivitySettingsBinding
import network.loki.messenger.libsession_util.util.UserPic
import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.all
import nl.komponents.kovenant.ui.alwaysUi
import nl.komponents.kovenant.ui.successUi
import org.session.libsession.avatars.AvatarHelper
import org.session.libsession.avatars.ProfileContactPhoto
import org.session.libsession.messaging.MessagingModuleConfiguration
import org.session.libsession.snode.SnodeAPI
import org.session.libsession.utilities.*
import org.session.libsession.utilities.SSKEnvironment.ProfileManagerProtocol
import org.session.libsession.utilities.recipients.Recipient
import org.thoughtcrime.securesms.PassphraseRequiredActionBarActivity
import org.thoughtcrime.securesms.avatar.AvatarSelection
import org.thoughtcrime.securesms.components.ProfilePictureView
import org.thoughtcrime.securesms.dependencies.ConfigFactory
import org.thoughtcrime.securesms.home.PathActivity
import org.thoughtcrime.securesms.messagerequests.MessageRequestsActivity
import org.thoughtcrime.securesms.mms.GlideApp
import org.thoughtcrime.securesms.mms.GlideRequests
import org.thoughtcrime.securesms.permissions.Permissions
import org.thoughtcrime.securesms.preferences.appearance.AppearanceSettingsActivity
import org.thoughtcrime.securesms.profiles.ProfileMediaConstraints
import org.thoughtcrime.securesms.showSessionDialog
import org.thoughtcrime.securesms.util.BitmapDecodingException
import org.thoughtcrime.securesms.util.BitmapUtil
import org.thoughtcrime.securesms.util.ConfigurationMessageUtilities
import org.thoughtcrime.securesms.util.disableClipping
import org.thoughtcrime.securesms.util.push
import org.thoughtcrime.securesms.util.show
import java.io.File
import java.security.SecureRandom
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : PassphraseRequiredActionBarActivity() {

    @Inject
    lateinit var configFactory: ConfigFactory

    private lateinit var binding: ActivitySettingsBinding
    private var displayNameEditActionMode: ActionMode? = null
        set(value) { field = value; handleDisplayNameEditActionModeChanged() }
    private lateinit var glide: GlideRequests
    private var tempFile: File? = null

    private val hexEncodedPublicKey: String
        get() {
            return TextSecurePreferences.getLocalNumber(this)!!
        }

    companion object {
        const val updatedProfileResultCode = 1234
        private const val SCROLL_STATE = "SCROLL_STATE"
    }

    // region Lifecycle
    override fun onCreate(savedInstanceState: Bundle?, isReady: Boolean) {
        super.onCreate(savedInstanceState, isReady)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val displayName = getDisplayName()
        glide = GlideApp.with(this)
        with(binding) {
            setupProfilePictureView(profilePictureView)
            profilePictureView.setOnClickListener { showEditProfilePictureUI() }
            ctnGroupNameSection.setOnClickListener { startActionMode(DisplayNameEditActionModeCallback()) }
            btnGroupNameDisplay.text = displayName
            publicKeyTextView.text = hexEncodedPublicKey
            copyButton.setOnClickListener { copyPublicKey() }
            shareButton.setOnClickListener { sharePublicKey() }
            pathButton.setOnClickListener { showPath() }
            pathContainer.disableClipping()
            privacyButton.setOnClickListener { showPrivacySettings() }
            notificationsButton.setOnClickListener { showNotificationSettings() }
            messageRequestsButton.setOnClickListener { showMessageRequests() }
            chatsButton.setOnClickListener { showChatSettings() }
            appearanceButton.setOnClickListener { showAppearanceSettings() }
            inviteFriendButton.setOnClickListener { sendInvitation() }
            helpButton.setOnClickListener { showHelp() }
            seedButton.setOnClickListener { showSeed() }
            clearAllDataButton.setOnClickListener { clearAllData() }
            versionTextView.text = String.format(getString(R.string.version_s), "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        }
    }

    private fun getDisplayName(): String =
        TextSecurePreferences.getProfileName(this) ?: truncateIdForDisplay(hexEncodedPublicKey)

    private fun setupProfilePictureView(view: ProfilePictureView) {
        view.apply {
            publicKey = hexEncodedPublicKey
            displayName = getDisplayName()
            isLarge = true
            update()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val scrollBundle = SparseArray<Parcelable>()
        binding.scrollView.saveHierarchyState(scrollBundle)
        outState.putSparseParcelableArray(SCROLL_STATE, scrollBundle)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        savedInstanceState.getSparseParcelableArray<Parcelable>(SCROLL_STATE)?.let { scrollBundle ->
            binding.scrollView.restoreHierarchyState(scrollBundle)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.settings_general, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_qr_code -> {
                showQRCode()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            AvatarSelection.REQUEST_CODE_AVATAR -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }
                val outputFile = Uri.fromFile(File(cacheDir, "cropped"))
                var inputFile: Uri? = data?.data
                if (inputFile == null && tempFile != null) {
                    inputFile = Uri.fromFile(tempFile)
                }
                AvatarSelection.circularCropImage(this, inputFile, outputFile, R.string.CropImageActivity_profile_avatar)
            }
            AvatarSelection.REQUEST_CODE_CROP_IMAGE -> {
                if (resultCode != Activity.RESULT_OK) {
                    return
                }
                AsyncTask.execute {
                    try {
                        val profilePictureToBeUploaded = BitmapUtil.createScaledBytes(this@SettingsActivity, AvatarSelection.getResultUri(data), ProfileMediaConstraints()).bitmap
                        Handler(Looper.getMainLooper()).post {
                            updateProfile(true, profilePictureToBeUploaded)
                        }
                    } catch (e: BitmapDecodingException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Permissions.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
    }
    // endregion

    // region Updating
    private fun handleDisplayNameEditActionModeChanged() {
        val isEditingDisplayName = this.displayNameEditActionMode !== null

        binding.btnGroupNameDisplay.visibility = if (isEditingDisplayName) View.INVISIBLE else View.VISIBLE
        binding.displayNameEditText.visibility = if (isEditingDisplayName) View.VISIBLE else View.INVISIBLE

        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if (isEditingDisplayName) {
            binding.displayNameEditText.setText(binding.btnGroupNameDisplay.text)
            binding.displayNameEditText.selectAll()
            binding.displayNameEditText.requestFocus()
            inputMethodManager.showSoftInput(binding.displayNameEditText, 0)
        } else {
            inputMethodManager.hideSoftInputFromWindow(binding.displayNameEditText.windowToken, 0)
        }
    }

    private fun updateProfile(
        isUpdatingProfilePicture: Boolean,
        profilePicture: ByteArray? = null,
        displayName: String? = null
    ) {
        binding.loader.isVisible = true
        val promises = mutableListOf<Promise<*, Exception>>()
        if (displayName != null) {
            TextSecurePreferences.setProfileName(this, displayName)
            configFactory.user?.setName(displayName)
        }
        val encodedProfileKey = ProfileKeyUtil.generateEncodedProfileKey(this)
        if (isUpdatingProfilePicture) {
            if (profilePicture != null) {
                promises.add(ProfilePictureUtilities.upload(profilePicture, encodedProfileKey, this))
            } else {
                MessagingModuleConfiguration.shared.storage.clearUserPic()
            }
        }
        val compoundPromise = all(promises)
        compoundPromise.successUi { // Do this on the UI thread so that it happens before the alwaysUi clause below
            val userConfig = configFactory.user
            if (isUpdatingProfilePicture) {
                AvatarHelper.setAvatar(this, Address.fromSerialized(TextSecurePreferences.getLocalNumber(this)!!), profilePicture)
                TextSecurePreferences.setProfileAvatarId(this, profilePicture?.let { SecureRandom().nextInt() } ?: 0 )
                ProfileKeyUtil.setEncodedProfileKey(this, encodedProfileKey)
                // new config
                val url = TextSecurePreferences.getProfilePictureURL(this)
                val profileKey = ProfileKeyUtil.getProfileKey(this)
                if (profilePicture == null) {
                    userConfig?.setPic(UserPic.DEFAULT)
                } else if (!url.isNullOrEmpty() && profileKey.isNotEmpty()) {
                    userConfig?.setPic(UserPic(url, profileKey))
                }
            }
            if (userConfig != null && userConfig.needsDump()) {
                configFactory.persist(userConfig, SnodeAPI.nowWithOffset)
            }
            ConfigurationMessageUtilities.forceSyncConfigurationNowIfNeeded(this@SettingsActivity)
        }
        compoundPromise.alwaysUi {
            if (displayName != null) {
                binding.btnGroupNameDisplay.text = displayName
            }
            if (isUpdatingProfilePicture) {
                binding.profilePictureView.recycle() // Clear the cached image before updating
                binding.profilePictureView.update()
            }
            binding.loader.isVisible = false
        }
    }
    // endregion

    // region Interaction

    /**
     * @return true if the update was successful.
     */
    private fun saveDisplayName(): Boolean {
        val displayName = binding.displayNameEditText.text.toString().trim()
        if (displayName.isEmpty()) {
            Toast.makeText(this, R.string.activity_settings_display_name_missing_error, Toast.LENGTH_SHORT).show()
            return false
        }
        if (displayName.toByteArray().size > ProfileManagerProtocol.Companion.NAME_PADDED_LENGTH) {
            Toast.makeText(this, R.string.activity_settings_display_name_too_long_error, Toast.LENGTH_SHORT).show()
            return false
        }
        updateProfile(false, displayName = displayName)
        return true
    }

    private fun showQRCode() {
        val intent = Intent(this, QRCodeActivity::class.java)
        push(intent)
    }

    private fun showEditProfilePictureUI() {
        showSessionDialog {
            title(R.string.activity_settings_set_display_picture)
            view(R.layout.dialog_change_avatar)
            button(R.string.activity_settings_upload) { startAvatarSelection() }
            if (TextSecurePreferences.getProfileAvatarId(context) != 0) {
                button(R.string.activity_settings_remove) { removeAvatar() }
            }
            cancelButton()
        }.apply {
                val profilePic = findViewById<ProfilePictureView>(R.id.profile_picture_view)
                    ?.also(::setupProfilePictureView)

                val pictureIcon = findViewById<View>(R.id.ic_pictures)

                val recipient = Recipient.from(context, Address.fromSerialized(hexEncodedPublicKey), false)

                val photoSet = (recipient.contactPhoto as ProfileContactPhoto).avatarObject !in setOf("0", "")

                profilePic?.isVisible = photoSet
                pictureIcon?.isVisible = !photoSet
            }
    }

    private fun removeAvatar() {
        updateProfile(true)
    }

    private fun startAvatarSelection() {
        // Ask for an optional camera permission.
        Permissions.with(this)
            .request(Manifest.permission.CAMERA)
            .onAnyResult {
                tempFile = AvatarSelection.startAvatarSelection(this, false, true)
            }
            .execute()
    }

    private fun copyPublicKey() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Session ID", hexEncodedPublicKey)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
    }

    private fun sharePublicKey() {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        intent.putExtra(Intent.EXTRA_TEXT, hexEncodedPublicKey)
        intent.type = "text/plain"
        val chooser = Intent.createChooser(intent, getString(R.string.share))
        startActivity(chooser)
    }

    private fun showPrivacySettings() {
        val intent = Intent(this, PrivacySettingsActivity::class.java)
        push(intent)
    }

    private fun showNotificationSettings() {
        val intent = Intent(this, NotificationSettingsActivity::class.java)
        push(intent)
    }

    private fun showMessageRequests() {
        val intent = Intent(this, MessageRequestsActivity::class.java)
        push(intent)
    }

    private fun showChatSettings() {
        val intent = Intent(this, ChatSettingsActivity::class.java)
        push(intent)
    }

    private fun showAppearanceSettings() {
        val intent = Intent(this, AppearanceSettingsActivity::class.java)
        push(intent)
    }

    private fun sendInvitation() {
        val intent = Intent()
        intent.action = Intent.ACTION_SEND
        val invitation = "Hey, I've been using Session to chat with complete privacy and security. Come join me! Download it at https://getsession.org/. My Session ID is $hexEncodedPublicKey !"
        intent.putExtra(Intent.EXTRA_TEXT, invitation)
        intent.type = "text/plain"
        val chooser = Intent.createChooser(intent, getString(R.string.activity_settings_invite_button_title))
        startActivity(chooser)
    }

    private fun showHelp() {
        val intent = Intent(this, HelpSettingsActivity::class.java)
        push(intent)
    }

    private fun showPath() {
        val intent = Intent(this, PathActivity::class.java)
        show(intent)
    }

    private fun showSeed() {
        SeedDialog().show(supportFragmentManager, "Recovery Phrase Dialog")
    }

    private fun clearAllData() {
        ClearAllDataDialog().show(supportFragmentManager, "Clear All Data Dialog")
    }

    // endregion

    private inner class DisplayNameEditActionModeCallback: ActionMode.Callback {

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.title = getString(R.string.activity_settings_display_name_edit_text_hint)
            mode.menuInflater.inflate(R.menu.menu_apply, menu)
            this@SettingsActivity.displayNameEditActionMode = mode
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            this@SettingsActivity.displayNameEditActionMode = null
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.applyButton -> {
                    if (this@SettingsActivity.saveDisplayName()) {
                        mode.finish()
                    }
                    return true
                }
            }
            return false;
        }
    }
}