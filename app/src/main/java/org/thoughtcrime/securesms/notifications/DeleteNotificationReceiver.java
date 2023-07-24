package org.thoughtcrime.securesms.notifications;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;

import org.session.libsession.messaging.sending_receiving.notifications.MessageNotifier;
import org.thoughtcrime.securesms.database.MmsDatabase;
import org.thoughtcrime.securesms.database.SmsDatabase;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class DeleteNotificationReceiver extends BroadcastReceiver {

  public static String DELETE_NOTIFICATION_ACTION = "network.loki.securesms.DELETE_NOTIFICATION";

  public static String EXTRA_IDS = "message_ids";
  public static String EXTRA_MMS = "is_mms";

  @Inject MessageNotifier messageNotifier;
  @Inject SmsDatabase smsDb;
  @Inject MmsDatabase mmsDb;

  @Override
  public void onReceive(final Context context, Intent intent) {
    if (DELETE_NOTIFICATION_ACTION.equals(intent.getAction())) {
      messageNotifier.clearReminder(context);

      final long[]    ids = intent.getLongArrayExtra(EXTRA_IDS);
      final boolean[] mms = intent.getBooleanArrayExtra(EXTRA_MMS);

      if (ids == null  || mms == null || ids.length != mms.length) return;

      new AsyncTask<Void, Void, Void>() {
        @Override
        protected Void doInBackground(Void... params) {
          for (int i=0;i<ids.length;i++) {
            if (!mms[i]) smsDb.markAsNotified(ids[i]);
            else         mmsDb.markAsNotified(ids[i]);
          }

          return null;
        }
      }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }
  }
}
