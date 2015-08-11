package com.arcadia.wearapp.alarm_sevices;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.arcadia.wearapp.R;

import java.util.Calendar;
import java.util.TimeZone;

public class AlarmTask implements Runnable {
    // The date selected for the alarm
    private final Calendar date;
    // The android system alarm manager
    private final AlarmManager am;
    // Your context to retrieve the alarm manager from
    private final Context context;
    private int reminderID;

    public AlarmTask(Context context, Calendar date, int reminderID) {
        this.context = context;
        this.reminderID = reminderID;
        this.am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        int offset = date.getTimeZone().getRawOffset() - TimeZone.getDefault().getRawOffset();
        date.add(Calendar.MILLISECOND, offset);
        this.date = date;
    }

    @Override
    public void run() {
        // Request to start are service when the alarm date is upon us
        // We don't start an activity as we just want to pop up a notification into the system bar not a full activity
        Intent intent = new Intent(context, NotifyService.class);
        intent.putExtra(NotifyService.INTENT_NOTIFY, true);
        intent.putExtra(context.getString(R.string.intent_reminder_id_key), reminderID);
        PendingIntent pendingIntent = PendingIntent.getService(context, 0, intent, 0);

        // Sets an alarm - note this alarm will be lost if the phone is turned off and on again
        am.set(AlarmManager.RTC_WAKEUP, date.getTimeInMillis(), pendingIntent);
    }
}