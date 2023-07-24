package network.loki.messenger

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import network.loki.messenger.libsession_util.ConfigBase
import network.loki.messenger.libsession_util.Contacts
import network.loki.messenger.libsession_util.util.Contact
import network.loki.messenger.libsession_util.util.ExpiryMode
import network.loki.messenger.util.TestStorageModule
import network.loki.messenger.util.TestStorageModule.MESSAGING_MODULE_CONTEXT
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.session.libsession.messaging.MessagingModuleConfiguration
import org.session.libsession.snode.SnodeModule
import org.session.libsession.utilities.TextSecurePreferences
import org.session.libsignal.utilities.KeyHelper
import org.session.libsignal.utilities.hexEncodedPublicKey
import org.thoughtcrime.securesms.crypto.KeyPairUtilities
import org.thoughtcrime.securesms.database.LokiAPIDatabase
import org.thoughtcrime.securesms.database.Storage
import javax.inject.Inject
import javax.inject.Named
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
@HiltAndroidTest
class LibSessionTests {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var prefs: TextSecurePreferences
    @Inject
    lateinit var storage: Storage
    @Inject
    lateinit var apiDb: LokiAPIDatabase
    @Inject
    @Named(MESSAGING_MODULE_CONTEXT)
    lateinit var messagingModuleContext: Context

    private fun randomSeedBytes() = (0 until 16).map { Random.nextInt(UByte.MAX_VALUE.toInt()).toByte() }
    private fun randomKeyPair() = KeyPairUtilities.generate(randomSeedBytes().toByteArray())
    private fun randomSessionId() = randomKeyPair().x25519KeyPair.hexEncodedPublicKey

    private var fakeHashI = 0
    private val nextFakeHash: String
        get() = "fakehash${fakeHashI++}"

    private fun maybeGetUserInfo(): Pair<ByteArray, String>? {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val localUserPublicKey = prefs.getLocalNumber()
        val secretKey = with(appContext) {
            val edKey = KeyPairUtilities.getUserED25519KeyPair(this) ?: return null
            edKey.secretKey.asBytes
        }
        return if (localUserPublicKey == null || secretKey == null) null
        else secretKey to localUserPublicKey
    }

    private fun buildContactMessage(contactList: List<Contact>): ByteArray {
        val (key,_) = maybeGetUserInfo()!!
        val contacts = Contacts.Companion.newInstance(key)
        contactList.forEach { contact ->
            contacts.set(contact)
        }
        return contacts.push().config
    }

    private fun fakePollNewConfig(configBase: ConfigBase, toMerge: ByteArray) {
        configBase.merge(nextFakeHash to toMerge)
        MessagingModuleConfiguration.shared.configFactory.persist(configBase, System.currentTimeMillis())
    }

    @Before
    fun setupUser() {
        hiltRule.inject()
        // ** Dependency wrangling **
        TestStorageModule.storage = storage
        MessagingModuleConfiguration.configure(messagingModuleContext)
        SnodeModule.configure(apiDb)

        PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext.applicationContext).edit {
            putBoolean(TextSecurePreferences.HAS_FORCED_NEW_CONFIG, true).apply()
        }
        val newBytes = randomSeedBytes().toByteArray()
        val context = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        val kp = KeyPairUtilities.generate(newBytes)
        KeyPairUtilities.store(context, kp.seed, kp.ed25519KeyPair, kp.x25519KeyPair)
        val registrationID = KeyHelper.generateRegistrationId(false)
        TextSecurePreferences.setLocalRegistrationId(context, registrationID)
        TextSecurePreferences.setLocalNumber(context, kp.x25519KeyPair.hexEncodedPublicKey)
        TextSecurePreferences.setRestorationTime(context, 0)
        TextSecurePreferences.setHasViewedSeed(context, false)
    }

    @Test
    fun migration_one_to_ones() {

        val newContactId = randomSessionId()
        val singleContact = Contact(
            id = newContactId,
            approved = true,
            expiryMode = ExpiryMode.NONE
        )
        val newContactMerge = buildContactMessage(listOf(singleContact))
        val contacts = MessagingModuleConfiguration.shared.configFactory.contacts!!
        fakePollNewConfig(contacts, newContactMerge)
        verify(storage).addLibSessionContacts(argThat {
            first().let { it.id == newContactId && it.approved } && size == 1
        })
        verify(storage).setRecipientApproved(argThat { address.serialize() == newContactId }, eq(true))
    }

}