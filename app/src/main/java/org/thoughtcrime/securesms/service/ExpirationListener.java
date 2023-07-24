package org.thoughtcrime.securesms.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ExpirationListener extends BroadcastReceiver {

  @Inject
  public ExpiringMessageManager expiringMessageManager;

  @Override
  public void onReceive(Context context, Intent intent) {
    expiringMessageManager.checkSchedule();
  }

  public static void setAlarm(Context context, long waitTimeMillis) {
    Intent        intent        = new Intent(context, ExpirationListener.class);
    PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    AlarmManager  alarmManager  = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);

    alarmManager.cancel(pendingIntent);
    alarmManager.set(AlarmManager.RTC, System.currentTimeMillis() + waitTimeMillis, pendingIntent);
  }
}
