package org.thoughtcrime.securesms.jobs;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

import org.session.libsession.messaging.threads.Address;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.MessagingDatabase.SyncMessageId;
import org.thoughtcrime.securesms.database.RecipientDatabase;
import org.thoughtcrime.securesms.jobmanager.Job;
import org.thoughtcrime.securesms.logging.Log;
import org.session.libsession.messaging.threads.recipients.Recipient;
import org.session.libsignal.service.api.messages.SignalServiceEnvelope;

public abstract class PushReceivedJob extends BaseJob {

  private static final String TAG = PushReceivedJob.class.getSimpleName();

  public static final Object RECEIVE_LOCK = new Object();

  protected PushReceivedJob(Job.Parameters parameters) {
    super(parameters);
  }

  public void processEnvelope(@NonNull SignalServiceEnvelope envelope, boolean isPushNotification) {
    synchronized (RECEIVE_LOCK) {
      try {
        if (envelope.hasSource()) {
          Address source = Address.fromExternal(context, envelope.getSource());
          Recipient recipient = Recipient.from(context, source, false);

          if (!isActiveNumber(recipient)) {
            DatabaseFactory.getRecipientDatabase(context).setRegistered(recipient, RecipientDatabase.RegisteredState.REGISTERED);
          }
        }

        if (envelope.isReceipt()) {
          handleReceipt(envelope);
        } else if (envelope.isPreKeySignalMessage() || envelope.isSignalMessage()
            || envelope.isUnidentifiedSender() || envelope.isFallbackMessage() || envelope.isClosedGroupCiphertext()) {
          handleMessage(envelope, isPushNotification);
        } else {
          Log.w(TAG, "Received envelope of unknown type: " + envelope.getType());
        }
      } catch (Exception e) {
        Log.d("Loki", "Failed to process envelope due to error: " + e);
      }
    }
  }

  private void handleMessage(SignalServiceEnvelope envelope, boolean isPushNotification) {
    new PushDecryptJob(context).processMessage(envelope, isPushNotification);
  }

  @SuppressLint("DefaultLocale")
  private void handleReceipt(SignalServiceEnvelope envelope) {
    Log.i(TAG, String.format("Received receipt: (XXXXX, %d)", envelope.getTimestamp()));
    DatabaseFactory.getMmsSmsDatabase(context).incrementDeliveryReceiptCount(new SyncMessageId(Address.fromExternal(context, envelope.getSource()),
                                                                                               envelope.getTimestamp()), System.currentTimeMillis());
  }

  private boolean isActiveNumber(@NonNull Recipient recipient) {
    return recipient.resolve().getRegistered() == RecipientDatabase.RegisteredState.REGISTERED;
  }
}
