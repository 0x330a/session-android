package org.session.libsession.utilities

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

fun Context.broadcast(event: String) {
    val intent = Intent(event)
    LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
}