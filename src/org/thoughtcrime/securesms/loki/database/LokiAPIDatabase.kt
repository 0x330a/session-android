package org.thoughtcrime.securesms.loki.database

import android.content.ContentValues
import android.content.Context
import android.util.Log
import org.thoughtcrime.securesms.database.Database
import org.thoughtcrime.securesms.database.helpers.SQLCipherOpenHelper
import org.thoughtcrime.securesms.loki.utilities.*
import org.thoughtcrime.securesms.util.TextSecurePreferences
import org.whispersystems.signalservice.loki.api.Snode
import org.whispersystems.signalservice.loki.database.LokiAPIDatabaseProtocol
import org.whispersystems.signalservice.loki.protocol.shelved.multidevice.DeviceLink

class LokiAPIDatabase(context: Context, helper: SQLCipherOpenHelper) : Database(context, helper), LokiAPIDatabaseProtocol {

    companion object {
        // Shared
        private val publicKey = "public_key"
        private val timestamp = "timestamp"
        private val snode = "snode"
        // Snode pool
        private val snodePoolTable = "loki_snode_pool_cache"
        private val dummyKey = "dummy_key"
        private val snodePool = "snode_pool_key"
        @JvmStatic val createSnodePoolTableCommand = "CREATE TABLE $snodePoolTable ($dummyKey TEXT PRIMARY KEY, $snodePool TEXT);"
        // Onion request paths
        private val onionRequestPathTable = "loki_path_cache"
        private val indexPath = "index_path"
        @JvmStatic val createOnionRequestPathTableCommand = "CREATE TABLE $onionRequestPathTable ($indexPath TEXT PRIMARY KEY, $snode TEXT);"
        // Swarms
        private val swarmTable = "loki_api_swarm_cache"
        private val swarmPublicKey = "hex_encoded_public_key"
        private val swarm = "swarm"
        @JvmStatic val createSwarmTableCommand = "CREATE TABLE $swarmTable ($swarmPublicKey TEXT PRIMARY KEY, $swarm TEXT);"
        // Last message hash values
        private val lastMessageHashValueTable2 = "last_message_hash_value_table"
        private val lastMessageHashValue = "last_message_hash_value"
        @JvmStatic val createLastMessageHashValueTable2Command
            = "CREATE TABLE $lastMessageHashValueTable2 ($snode TEXT, $publicKey TEXT, $lastMessageHashValue TEXT, PRIMARY KEY ($snode, $publicKey));"
        // Received message hash values
        private val receivedMessageHashValuesTable3 = "received_message_hash_values_table_3"
        private val receivedMessageHashValues = "received_message_hash_values"
        @JvmStatic val createReceivedMessageHashValuesTable3Command
            = "CREATE TABLE $receivedMessageHashValuesTable3 ($publicKey STRING PRIMARY KEY, $receivedMessageHashValues TEXT);"
        // Open group auth tokens
        private val openGroupAuthTokenTable = "loki_api_group_chat_auth_token_database"
        private val server = "server"
        private val token = "token"
        @JvmStatic val createOpenGroupAuthTokenTableCommand = "CREATE TABLE $openGroupAuthTokenTable ($server TEXT PRIMARY KEY, $token TEXT);"
        // Last message server IDs
        private val lastMessageServerIDTable = "loki_api_last_message_server_id_cache"
        private val lastMessageServerIDTableIndex = "loki_api_last_message_server_id_cache_index"
        private val lastMessageServerID = "last_message_server_id"
        @JvmStatic val createLastMessageServerIDTableCommand = "CREATE TABLE $lastMessageServerIDTable ($lastMessageServerIDTableIndex STRING PRIMARY KEY, $lastMessageServerID INTEGER DEFAULT 0);"
        // Last deletion server IDs
        private val lastDeletionServerIDTable = "loki_api_last_deletion_server_id_cache"
        private val lastDeletionServerIDTableIndex = "loki_api_last_deletion_server_id_cache_index"
        private val lastDeletionServerID = "last_deletion_server_id"
        @JvmStatic val createLastDeletionServerIDTableCommand = "CREATE TABLE $lastDeletionServerIDTable ($lastDeletionServerIDTableIndex STRING PRIMARY KEY, $lastDeletionServerID INTEGER DEFAULT 0);"
        // User counts
        private val userCountTable = "loki_user_count_cache"
        private val publicChatID = "public_chat_id"
        private val userCount = "user_count"
        @JvmStatic val createUserCountTableCommand = "CREATE TABLE $userCountTable ($publicChatID STRING PRIMARY KEY, $userCount INTEGER DEFAULT 0);"
        // Session request sent timestamps
        private val sessionRequestSentTimestampTable = "session_request_sent_timestamp_cache"
        @JvmStatic val createSessionRequestSentTimestampTableCommand = "CREATE TABLE $sessionRequestSentTimestampTable ($publicKey STRING PRIMARY KEY, $timestamp INTEGER DEFAULT 0);"
        // Session request processed timestamp cache
        private val sessionRequestProcessedTimestampTable = "session_request_processed_timestamp_cache"
        @JvmStatic val createSessionRequestProcessedTimestampTableCommand = "CREATE TABLE $sessionRequestProcessedTimestampTable ($publicKey STRING PRIMARY KEY, $timestamp INTEGER DEFAULT 0);"
        // Open group public keys
        private val openGroupPublicKeyTable = "open_group_public_keys"
        @JvmStatic val createOpenGroupPublicKeyTableCommand = "CREATE TABLE $openGroupPublicKeyTable ($server STRING PRIMARY KEY, $publicKey INTEGER DEFAULT 0);"
        // Open group avatar cache
        private val openGroupAvatarCacheTable = "open_group_avatar_cache"
        private val openGroupAvatar = "open_group_avatar"
        @JvmStatic val createOpenGroupAvatarCacheCommand = "CREATE TABLE $openGroupAvatarCacheTable ($publicChatID STRING PRIMARY KEY, $openGroupAvatar TEXT NULLABLE DEFAULT NULL);"

        // region Deprecated
        private val deviceLinkCache = "loki_pairing_authorisation_cache"
        private val masterPublicKey = "primary_device"
        private val slavePublicKey = "secondary_device"
        private val requestSignature = "request_signature"
        private val authorizationSignature = "grant_signature"
        @JvmStatic val createDeviceLinkCacheCommand = "CREATE TABLE $deviceLinkCache ($masterPublicKey STRING, $slavePublicKey STRING, " +
            "$requestSignature STRING NULLABLE DEFAULT NULL, $authorizationSignature STRING NULLABLE DEFAULT NULL, PRIMARY KEY ($masterPublicKey, $slavePublicKey));"
        private val sessionRequestTimestampCache = "session_request_timestamp_cache"
        @JvmStatic val createSessionRequestTimestampCacheCommand = "CREATE TABLE $sessionRequestTimestampCache ($publicKey STRING PRIMARY KEY, $timestamp STRING);"
        // endregion
    }

    override fun getSnodePool(): Set<Snode> {
        val database = databaseHelper.readableDatabase
        return database.get(snodePoolTable, "${Companion.dummyKey} = ?", wrap("dummy_key")) { cursor ->
            val snodePoolAsString = cursor.getString(cursor.getColumnIndexOrThrow(snodePool))
            snodePoolAsString.split(", ").mapNotNull { snodeAsString ->
                val components = snodeAsString.split("-")
                val address = components[0]
                val port = components.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                val ed25519Key = components.getOrNull(2) ?: return@mapNotNull null
                val x25519Key = components.getOrNull(3) ?: return@mapNotNull null
                Snode(address, port, Snode.KeySet(ed25519Key, x25519Key))
            }
        }?.toSet() ?: setOf()
    }

    override fun setSnodePool(newValue: Set<Snode>) {
        val database = databaseHelper.writableDatabase
        val snodePoolAsString = newValue.joinToString(", ") { snode ->
            var string = "${snode.address}-${snode.port}"
            val keySet = snode.publicKeySet
            if (keySet != null) {
                string += "-${keySet.ed25519Key}-${keySet.x25519Key}"
            }
            string
        }
        val row = wrap(mapOf( Companion.dummyKey to "dummy_key", snodePool to snodePoolAsString ))
        database.insertOrUpdate(snodePoolTable, row, "${Companion.dummyKey} = ?", wrap("dummy_key"))
    }

    override fun getOnionRequestPaths(): List<List<Snode>> {
        val database = databaseHelper.readableDatabase
        fun get(indexPath: String): Snode? {
            return database.get(onionRequestPathTable, "${Companion.indexPath} = ?", wrap(indexPath)) { cursor ->
                val snodeAsString = cursor.getString(cursor.getColumnIndexOrThrow(snode))
                val components = snodeAsString.split("-")
                val address = components[0]
                val port = components.getOrNull(1)?.toIntOrNull()
                val ed25519Key = components.getOrNull(2)
                val x25519Key = components.getOrNull(3)
                if (port != null && ed25519Key != null && x25519Key != null) {
                    Snode(address, port, Snode.KeySet(ed25519Key, x25519Key))
                } else {
                    null
                }
            }
        }
        val path0Snode0 = get("0-0") ?: return listOf(); val path0Snode1 = get("0-1") ?: return listOf()
        val path0Snode2 = get("0-2") ?: return listOf(); val path1Snode0 = get("1-0") ?: return listOf()
        val path1Snode1 = get("1-1") ?: return listOf(); val path1Snode2 = get("1-2") ?: return listOf()
        return listOf( listOf( path0Snode0, path0Snode1, path0Snode2 ), listOf( path1Snode0, path1Snode1, path1Snode2 ) )
    }

    override fun clearOnionRequestPaths() {
        val database = databaseHelper.writableDatabase
        fun delete(indexPath: String) {
            database.delete(onionRequestPathTable, "${Companion.indexPath} = ?", wrap(indexPath))
        }
        delete("0-0"); delete("0-1")
        delete("0-2"); delete("1-0")
        delete("1-1"); delete("1-2")
    }

    override fun setOnionRequestPaths(newValue: List<List<Snode>>) {
        // TODO: Make this work with arbitrary paths
        if (newValue.count() != 2) { return }
        val path0 = newValue[0]
        val path1 = newValue[1]
        if (path0.count() != 3 || path1.count() != 3) { return }
        Log.d("Loki", "Persisting onion request paths to database.")
        val database = databaseHelper.writableDatabase
        fun set(indexPath: String, snode: Snode) {
            var snodeAsString = "${snode.address}-${snode.port}"
            val keySet = snode.publicKeySet
            if (keySet != null) {
                snodeAsString += "-${keySet.ed25519Key}-${keySet.x25519Key}"
            }
            val row = wrap(mapOf( Companion.indexPath to indexPath, Companion.snode to snodeAsString ))
            database.insertOrUpdate(onionRequestPathTable, row, "${Companion.indexPath} = ?", wrap(indexPath))
        }
        set("0-0", path0[0]); set("0-1", path0[1])
        set("0-2", path0[2]); set("1-0", path1[0])
        set("1-1", path1[1]); set("1-2", path1[2])
    }

    override fun getSwarm(publicKey: String): Set<Snode>? {
        val database = databaseHelper.readableDatabase
        return database.get(swarmTable, "${Companion.swarmPublicKey} = ?", wrap(publicKey)) { cursor ->
            val swarmAsString = cursor.getString(cursor.getColumnIndexOrThrow(swarm))
            swarmAsString.split(", ").mapNotNull { targetAsString ->
                val components = targetAsString.split("-")
                val address = components[0]
                val port = components.getOrNull(1)?.toIntOrNull() ?: return@mapNotNull null
                val ed25519Key = components.getOrNull(2) ?: return@mapNotNull null
                val x25519Key = components.getOrNull(3) ?: return@mapNotNull null
                Snode(address, port, Snode.KeySet(ed25519Key, x25519Key))
            }
        }?.toSet()
    }

    override fun setSwarm(publicKey: String, newValue: Set<Snode>) {
        val database = databaseHelper.writableDatabase
        val swarmAsString = newValue.joinToString(", ") { target ->
            var string = "${target.address}-${target.port}"
            val keySet = target.publicKeySet
            if (keySet != null) {
                string += "-${keySet.ed25519Key}-${keySet.x25519Key}"
            }
            string
        }
        val row = wrap(mapOf( Companion.swarmPublicKey to publicKey, swarm to swarmAsString ))
        database.insertOrUpdate(swarmTable, row, "${Companion.swarmPublicKey} = ?", wrap(publicKey))
    }

    override fun getLastMessageHashValue(snode: Snode, publicKey: String): String? {
        val database = databaseHelper.readableDatabase
        val query = "${Companion.snode} = ? AND ${Companion.publicKey} = ?"
        return database.get(lastMessageHashValueTable2, query, arrayOf( snode.toString(), publicKey )) { cursor ->
            cursor.getString(cursor.getColumnIndexOrThrow(lastMessageHashValue))
        }
    }

    override fun setLastMessageHashValue(snode: Snode, publicKey: String, newValue: String) {
        val database = databaseHelper.writableDatabase
        val row = wrap(mapOf( Companion.snode to snode.toString(), Companion.publicKey to publicKey, lastMessageHashValue to newValue ))
        val query = "${Companion.snode} = ? AND ${Companion.publicKey} = ?"
        database.insertOrUpdate(lastMessageHashValueTable2, row, query, arrayOf( snode.toString(), publicKey ))
    }

    override fun getReceivedMessageHashValues(publicKey: String): Set<String>? {
        val database = databaseHelper.readableDatabase
        val query = "${Companion.publicKey} = ?"
        return database.get(receivedMessageHashValuesTable3, query, arrayOf( publicKey )) { cursor ->
            val receivedMessageHashValuesAsString = cursor.getString(cursor.getColumnIndexOrThrow(Companion.receivedMessageHashValues))
            receivedMessageHashValuesAsString.split("-").toSet()
        }
    }

    override fun setReceivedMessageHashValues(publicKey: String, newValue: Set<String>) {
        val database = databaseHelper.writableDatabase
        val receivedMessageHashValuesAsString = newValue.joinToString("-")
        val row = wrap(mapOf( Companion.publicKey to publicKey, Companion.receivedMessageHashValues to receivedMessageHashValuesAsString ))
        val query = "${Companion.publicKey} = ?"
        database.insertOrUpdate(receivedMessageHashValuesTable3, row, query, arrayOf( publicKey ))
    }

    override fun getAuthToken(server: String): String? {
        val database = databaseHelper.readableDatabase
        return database.get(openGroupAuthTokenTable, "${Companion.server} = ?", wrap(server)) { cursor ->
            cursor.getString(cursor.getColumnIndexOrThrow(token))
        }
    }

    override fun setAuthToken(server: String, newValue: String?) {
        val database = databaseHelper.writableDatabase
        if (newValue != null) {
            val row = wrap(mapOf( Companion.server to server, token to newValue ))
            database.insertOrUpdate(openGroupAuthTokenTable, row, "${Companion.server} = ?", wrap(server))
        } else {
            database.delete(openGroupAuthTokenTable, "${Companion.server} = ?", wrap(server))
        }
    }

    override fun getLastMessageServerID(group: Long, server: String): Long? {
        val database = databaseHelper.readableDatabase
        val index = "$server.$group"
        return database.get(lastMessageServerIDTable, "$lastMessageServerIDTableIndex = ?", wrap(index)) { cursor ->
            cursor.getInt(lastMessageServerID)
        }?.toLong()
    }

    override fun setLastMessageServerID(group: Long, server: String, newValue: Long) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$group"
        val row = wrap(mapOf( lastMessageServerIDTableIndex to index, lastMessageServerID to newValue.toString() ))
        database.insertOrUpdate(lastMessageServerIDTable, row, "$lastMessageServerIDTableIndex = ?", wrap(index))
    }

    fun removeLastMessageServerID(group: Long, server: String) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$group"
        database.delete(lastMessageServerIDTable,"$lastMessageServerIDTableIndex = ?", wrap(index))
    }

    override fun getLastDeletionServerID(group: Long, server: String): Long? {
        val database = databaseHelper.readableDatabase
        val index = "$server.$group"
        return database.get(lastDeletionServerIDTable, "$lastDeletionServerIDTableIndex = ?", wrap(index)) { cursor ->
            cursor.getInt(lastDeletionServerID)
        }?.toLong()
    }

    override fun setLastDeletionServerID(group: Long, server: String, newValue: Long) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$group"
        val row = wrap(mapOf( lastDeletionServerIDTableIndex to index, lastDeletionServerID to newValue.toString() ))
        database.insertOrUpdate(lastDeletionServerIDTable, row, "$lastDeletionServerIDTableIndex = ?", wrap(index))
    }

    fun removeLastDeletionServerID(group: Long, server: String) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$group"
        database.delete(lastDeletionServerIDTable,"$lastDeletionServerIDTableIndex = ?", wrap(index))
    }

    fun getUserCount(group: Long, server: String): Int? {
        val database = databaseHelper.readableDatabase
        val index = "$server.$group"
        return database.get(userCountTable, "$publicChatID = ?", wrap(index)) { cursor ->
            cursor.getInt(userCount)
        }?.toInt()
    }

    override fun setUserCount(group: Long, server: String, newValue: Int) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$group"
        val row = wrap(mapOf( publicChatID to index, Companion.userCount to newValue.toString() ))
        database.insertOrUpdate(userCountTable, row, "$publicChatID = ?", wrap(index))
    }

    override fun getSessionRequestSentTimestamp(publicKey: String): Long? {
        val database = databaseHelper.readableDatabase
        return database.get(sessionRequestSentTimestampTable, "${LokiAPIDatabase.publicKey} = ?", wrap(publicKey)) { cursor ->
            cursor.getLong(LokiAPIDatabase.timestamp)
        }?.toLong()
    }

    override fun setSessionRequestSentTimestamp(publicKey: String, newValue: Long) {
        val database = databaseHelper.writableDatabase
        val row = wrap(mapOf( LokiAPIDatabase.publicKey to publicKey, LokiAPIDatabase.timestamp to newValue.toString() ))
        database.insertOrUpdate(sessionRequestSentTimestampTable, row, "${LokiAPIDatabase.publicKey} = ?", wrap(publicKey))
    }

    override fun getSessionRequestProcessedTimestamp(publicKey: String): Long? {
        val database = databaseHelper.readableDatabase
        return database.get(sessionRequestProcessedTimestampTable, "${LokiAPIDatabase.publicKey} = ?", wrap(publicKey)) { cursor ->
            cursor.getInt(LokiAPIDatabase.timestamp)
        }?.toLong()
    }

    override fun setSessionRequestProcessedTimestamp(publicKey: String, newValue: Long) {
        val database = databaseHelper.writableDatabase
        val row = wrap(mapOf(LokiAPIDatabase.publicKey to publicKey, LokiAPIDatabase.timestamp to newValue.toString()))
        database.insertOrUpdate(sessionRequestProcessedTimestampTable, row, "${LokiAPIDatabase.publicKey} = ?", wrap(publicKey))
    }

    override fun getOpenGroupPublicKey(server: String): String? {
        val database = databaseHelper.readableDatabase
        return database.get(openGroupPublicKeyTable, "${LokiAPIDatabase.server} = ?", wrap(server)) { cursor ->
            cursor.getString(LokiAPIDatabase.publicKey)
        }
    }

    override fun setOpenGroupPublicKey(server: String, newValue: String) {
        val database = databaseHelper.writableDatabase
        val row = wrap(mapOf( LokiAPIDatabase.server to server, LokiAPIDatabase.publicKey to newValue ))
        database.insertOrUpdate(openGroupPublicKeyTable, row, "${LokiAPIDatabase.server} = ?", wrap(server))
    }

    override fun getOpenGroupAvatarURL(group: Long, server: String): String? {
        val database = databaseHelper.readableDatabase
        val index = "$server.$group"
        return database.get(openGroupAvatarCacheTable, "$publicChatID = ?", wrap(index)) { cursor ->
            cursor.getString(openGroupAvatar)
        }?.toString()
    }

    override fun setOpenGroupAvatarURL(url: String, group: Long, server: String) {
        val database = databaseHelper.writableDatabase
        val index = "$server.$group"
        val row = wrap(mapOf(publicChatID to index, openGroupAvatar to url))
        database.insertOrUpdate(openGroupAvatarCacheTable, row, "$publicChatID = ?", wrap(index))
    }

    // region Deprecated
    override fun getDeviceLinks(publicKey: String): Set<DeviceLink> {
        return setOf()
        /*
        val database = databaseHelper.readableDatabase
        return database.getAll(deviceLinkCache, "$masterPublicKey = ? OR $slavePublicKey = ?", arrayOf( publicKey, publicKey )) { cursor ->
            val masterHexEncodedPublicKey = cursor.getString(masterPublicKey)
            val slaveHexEncodedPublicKey = cursor.getString(slavePublicKey)
            val requestSignature: ByteArray? = if (cursor.isNull(cursor.getColumnIndexOrThrow(requestSignature))) null else cursor.getBase64EncodedData(requestSignature)
            val authorizationSignature: ByteArray? = if (cursor.isNull(cursor.getColumnIndexOrThrow(authorizationSignature))) null else cursor.getBase64EncodedData(authorizationSignature)
            DeviceLink(masterHexEncodedPublicKey, slaveHexEncodedPublicKey, requestSignature, authorizationSignature)
        }.toSet()
         */
    }

    override fun clearDeviceLinks(publicKey: String) {
        /*
        val database = databaseHelper.writableDatabase
        database.delete(deviceLinkCache, "$masterPublicKey = ? OR $slavePublicKey = ?", arrayOf( publicKey, publicKey ))
         */
    }

    override fun addDeviceLink(deviceLink: DeviceLink) {
        /*
        val database = databaseHelper.writableDatabase
        val values = ContentValues()
        values.put(masterPublicKey, deviceLink.masterPublicKey)
        values.put(slavePublicKey, deviceLink.slavePublicKey)
        if (deviceLink.requestSignature != null) { values.put(requestSignature, Base64.encodeBytes(deviceLink.requestSignature)) }
        if (deviceLink.authorizationSignature != null) { values.put(authorizationSignature, Base64.encodeBytes(deviceLink.authorizationSignature)) }
        database.insertOrUpdate(deviceLinkCache, values, "$masterPublicKey = ? AND $slavePublicKey = ?", arrayOf( deviceLink.masterPublicKey, deviceLink.slavePublicKey ))
         */
    }

    override fun removeDeviceLink(deviceLink: DeviceLink) {
        /*
        val database = databaseHelper.writableDatabase
        database.delete(deviceLinkCache, "$masterPublicKey = ? OR $slavePublicKey = ?", arrayOf( deviceLink.masterPublicKey, deviceLink.slavePublicKey ))
         */
    }
    // endregion
}

// region Convenience
private inline fun <reified T> wrap(x: T): Array<T> {
    return Array(1) { x }
}

private fun wrap(x: Map<String, String>): ContentValues {
    val result = ContentValues(x.size)
    x.forEach { result.put(it.key, it.value) }
    return result
}
// endregion