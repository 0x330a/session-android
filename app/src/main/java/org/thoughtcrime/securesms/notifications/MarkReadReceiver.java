package org.thoughtcrime.securesms.notifications;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import org.session.libsession.database.StorageProtocol;
import org.session.libsession.messaging.MessagingModuleConfiguration;
import org.session.libsession.messaging.messages.control.ReadReceipt;
import org.session.libsession.messaging.sending_receiving.MessageSender;
import org.session.libsession.snode.SnodeAPI;
import org.session.libsession.utilities.Address;
import org.session.libsession.utilities.TextSecurePreferences;
import org.session.libsession.utilities.recipients.Recipient;
import org.session.libsignal.utilities.Log;
import org.thoughtcrime.securesms.database.MessagingDatabase.ExpirationInfo;
import org.thoughtcrime.securesms.database.MessagingDatabase.MarkedMessageInfo;
import org.thoughtcrime.securesms.database.MessagingDatabase.SyncMessageId;
import org.thoughtcrime.securesms.database.MmsDatabase;
import org.thoughtcrime.securesms.database.SmsDatabase;
import org.thoughtcrime.securesms.service.ExpiringMessageManager;
import org.thoughtcrime.securesms.util.SessionMetaProtocol;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MarkReadReceiver extends BroadcastReceiver {

  private static final String TAG                   = MarkReadReceiver.class.getSimpleName();
  public static final  String CLEAR_ACTION          = "network.loki.securesms.notifications.CLEAR";
  public static final  String THREAD_IDS_EXTRA      = "thread_ids";
  public static final  String NOTIFICATION_ID_EXTRA = "notification_id";

  @Inject public ExpiringMessageManager expirationManager;
  @Inject public MmsDatabase mmsDb;
  @Inject public SmsDatabase smsDb;

  @SuppressLint("StaticFieldLeak")
  @Override
  public void onReceive(final Context context, Intent intent) {
    if (!CLEAR_ACTION.equals(intent.getAction()))
      return;

    final long[] threadIds = intent.getLongArrayExtra(THREAD_IDS_EXTRA);

    if (threadIds != null) {
      NotificationManagerCompat.from(context).cancel(intent.getIntExtra(NOTIFICATION_ID_EXTRA, -1));

      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          long currentTime = SnodeAPI.getNowWithOffset();
          for (long threadId : threadIds) {
            Log.i(TAG, "Marking as read: " + threadId);
            StorageProtocol storage = MessagingModuleConfiguration.getShared().getStorage();
            storage.markConversationAsRead(threadId,currentTime, true);
          }
          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }

  public void process(@NonNull Context context, @NonNull List<MarkedMessageInfo> markedReadMessages) {
    if (markedReadMessages.isEmpty()) return;

    for (MarkedMessageInfo messageInfo : markedReadMessages) {
      scheduleDeletion(messageInfo.getExpirationInfo());
    }

    if (!TextSecurePreferences.isReadReceiptsEnabled(context)) return;

    Map<Address, List<SyncMessageId>> addressMap = Stream.of(markedReadMessages)
                                                         .map(MarkedMessageInfo::getSyncMessageId)
                                                         .collect(Collectors.groupingBy(SyncMessageId::getAddress));

    for (Address address : addressMap.keySet()) {
      List<Long> timestamps = Stream.of(addressMap.get(address)).map(SyncMessageId::getTimetamp).toList();
      if (!SessionMetaProtocol.shouldSendReadReceipt(Recipient.from(context, address, false))) { continue; }
      ReadReceipt readReceipt = new ReadReceipt(timestamps);
      readReceipt.setSentTimestamp(SnodeAPI.getNowWithOffset());
      MessageSender.send(readReceipt, address);
    }
  }

  public void scheduleDeletion(ExpirationInfo expirationInfo) {
    if (expirationInfo.getExpiresIn() > 0 && expirationInfo.getExpireStarted() <= 0) {

      if (expirationInfo.isMms()) mmsDb.markExpireStarted(expirationInfo.getId());
      else                        smsDb.markExpireStarted(expirationInfo.getId());

      expirationManager.scheduleDeletion(expirationInfo.getId(), expirationInfo.isMms(), expirationInfo.getExpiresIn());
    }
  }
}
