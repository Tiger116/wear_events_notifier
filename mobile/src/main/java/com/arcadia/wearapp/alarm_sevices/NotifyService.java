package com.arcadia.wearapp.alarm_sevices;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.arcadia.wearapp.MobileListenerService;
import com.arcadia.wearapp.R;
import com.arcadia.wearapp.activities.MainActivity;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.realm_objects.Reminder;

import java.text.SimpleDateFormat;
import java.util.Locale;

import io.realm.Realm;

public class NotifyService extends IntentService {

    // Name of an intent extra we can use to identify if this service was started to create a notification
    public static final String INTENT_NOTIFY = "com.atcadia.wearapp.INTENT_NOTIFY";
    // This is the object that receives interactions from clients
    private final IBinder mBinder = new ServiceBinder();
    // The system notification manager
    private NotificationManagerCompat notificationManager;

    public NotifyService() {
        super(null);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
//        Log.i("LocalService", "Received start id " + startId + ": " + intent);
        int reminderId = 0;
        if (intent.getExtras().containsKey(getString(R.string.intent_reminder_id_key))) {
            reminderId = intent.getIntExtra(getString(R.string.intent_reminder_id_key), 0);
        }

        // If this service was started by out AlarmTask intent then we want to show our notification
        if (intent.getBooleanExtra(INTENT_NOTIFY, false))
            showNotification(reminderId);
    }

    private void showNotification(int reminderId) {
        notificationManager = NotificationManagerCompat.from(this);

        Realm realm = Realm.getInstance(this);
        Reminder reminder = realm.where(Reminder.class).equalTo("reminderID", reminderId).findFirst();
        if (reminder != null) {
            Event event = realm.where(Event.class).equalTo("eventID", reminder.getEventID()).findFirst();

            if (event == null)
                event = new Event("Unidentified event");
            Intent openIntent = new Intent(this, MainActivity.class);
            openIntent.setAction(MobileListenerService.Action_Open_Event);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK + Intent.FLAG_ACTIVITY_CLEAR_TASK);
            openIntent.putExtra(getString(R.string.intent_event_id_key), event.getEventID());

            PendingIntent viewPendingIntent = PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            String contentText = "";
            if (event.getStartDate() != null)
                contentText = new SimpleDateFormat("EEE d MMM h:mm a", Locale.ROOT).format(event.getStartDate());
            if (event.getDescription() != null && event.getDescription().isEmpty())
                contentText += String.format("\n%s", event.getDescription());
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(event.getTitle())
                    .setContentText(contentText)
                    .setContentText(contentText)
                    .setAutoCancel(true)
                    .setContentIntent(viewPendingIntent);
//        realm.beginTransaction();
//        realm.where(Reminder.class).equalTo("reminderID", remindId).findFirst().removeFromRealm();
//        realm.commitTransaction();
            realm.close();

            // Send the notification to the system.
            notificationManager.notify(reminderId, notificationBuilder.build());
        }
        // Stop the service when we are finished
        stopSelf();
    }

    /**
     * Class for clients to access
     */
    public class ServiceBinder extends Binder {
        NotifyService getService() {
            return NotifyService.this;
        }
    }
}