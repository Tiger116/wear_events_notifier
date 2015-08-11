package com.arcadia.wearapp.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.MobileListenerService;
import com.arcadia.wearapp.R;
import com.arcadia.wearapp.realm_objects.Reminder;
import com.arcadia.wearapp.adapters.ReminderView;
import com.arcadia.wearapp.alarm_sevices.ScheduleClient;
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.RadialPickerLayout;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import io.realm.Realm;
import io.realm.RealmResults;

public class DescriptionActivity extends AppCompatActivity {
    public static final int datepicker_type_start = 1;
    public static final int datepicker_type_end = 2;
    LinearLayout remindersLayout;
    public Calendar startDate = Calendar.getInstance();
    private Calendar endDate = Calendar.getInstance();
    private boolean editMode;
    private List<ReminderView> reminderViews;
    private int eventID = -1;
    private boolean isChanged = false;
    private TextView startDateTV;
    private ImageButton clearStartDateButton;
    private ImageButton clearEndDateButton;
    private ImageButton clearNameButton;
    private TextView endDateTV;
    private TextView startTimeTV;
    private ImageButton addReminderButton;
    private EditText nameEditText;
    private TextView endTimeTV;
    TimePickerDialog.OnTimeSetListener endTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialPickerLayout radialPickerLayout, int hourOfDay, int minute) {
            endDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
            endDate.set(Calendar.MINUTE, minute);
            endDate.set(Calendar.SECOND, 0);
            endTimeTV.setText(new SimpleDateFormat("hh:mm a", Locale.ROOT).format(endDate.getTime()));
            if (endDateTV.getText().toString().isEmpty())
                endDateTV.setText(new SimpleDateFormat("EEE d MMM", Locale.ENGLISH).format(endDate.getTime()));

            if (editMode)
                clearEndDateButton.setVisibility(View.VISIBLE);
        }
    };
    private boolean userIsInteracting = false;
    DatePickerDialog.OnDateSetListener startDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePickerDialog datePickerDialog, int year, int monthOfYear, int dayOfMonth) {
            startDate = Calendar.getInstance();
            startDate.set(year, monthOfYear, dayOfMonth);
            startDate.set(Calendar.SECOND, 0);
            startDateTV.setText(new SimpleDateFormat("EEE d MMM", Locale.ENGLISH).format(startDate.getTime()));
            if (startTimeTV.getText().toString().isEmpty())
                startTimeTV.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(startDate.getTime()));
            if (editMode)
                clearStartDateButton.setVisibility(View.VISIBLE);
            setChanged(true);
        }
    };
    TimePickerDialog.OnTimeSetListener startTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(RadialPickerLayout radialPickerLayout, int hourOfDay, int minute) {
            startDate.set(Calendar.HOUR_OF_DAY, hourOfDay);
            startDate.set(Calendar.MINUTE, minute);
            startDate.set(Calendar.SECOND, 0);
            startTimeTV.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(startDate.getTime()));
            if (startDateTV.getText().toString().isEmpty())
                startDateTV.setText(new SimpleDateFormat("EEE d MMM", Locale.ENGLISH).format(startDate.getTime()));
            if (endDateTV.getText().toString().isEmpty() || endTimeTV.getText().toString().isEmpty()) {
                endDate.setTime(startDate.getTime());
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(DescriptionActivity.this);
                if (preferences.getBoolean("timePeriod", false)) {
                    endDate.add(Calendar.HOUR, 1);
                    endDateTV.setText(new SimpleDateFormat("EEE d MMM", Locale.ENGLISH).format(endDate.getTime()));
                    endTimeTV.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(endDate.getTime()));
                    clearEndDateButton.setVisibility(View.VISIBLE);
                }
            }
            if (editMode)
                clearStartDateButton.setVisibility(View.VISIBLE);
            setChanged(true);
        }
    };
    DatePickerDialog.OnDateSetListener endDateSetListener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePickerDialog datePickerDialog, int year, int monthOfYear, int dayOfMonth) {
            endDate.setTime(startDate.getTime());
            endDate.set(year, monthOfYear, dayOfMonth);
            endDate.set(Calendar.SECOND, 0);
            endDateTV.setText(new SimpleDateFormat("EEE d MMM", Locale.ENGLISH).format(endDate.getTime()));
            if (endTimeTV.getText().length() == 0)
                endTimeTV.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(endDate.getTime()));
            if (editMode)
                clearEndDateButton.setVisibility(View.VISIBLE);
            setChanged(true);
        }
    };
    private ScheduleClient scheduleClient;
    private EditText descriptionEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);
        getIntent().getFlags();

        reminderViews = new ArrayList<>();

        nameEditText = (EditText) findViewById(R.id.name_text);
        clearNameButton = (ImageButton) findViewById(R.id.clear_name_text_button);
        clearStartDateButton = (ImageButton) findViewById(R.id.clear_start_date_button);
        clearEndDateButton = (ImageButton) findViewById(R.id.clear_end_date_button);
        addReminderButton = (ImageButton) findViewById(R.id.add_remind_button);

        startDateTV = (TextView) findViewById(R.id.start_date);
        startTimeTV = (TextView) findViewById(R.id.start_time);

        endDateTV = (TextView) findViewById(R.id.end_date);
        endTimeTV = (TextView) findViewById(R.id.end_time);
        descriptionEditText = (EditText) findViewById(R.id.description_text);

        remindersLayout = (LinearLayout) findViewById(R.id.reminders_list);

        Realm realm = Realm.getInstance(this);
        Event event;
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            eventID = bundle.getInt(getString(R.string.intent_event_id_key));
            event = realm.where(Event.class).equalTo("eventID", eventID).findFirst();
            if (event != null) {
                nameEditText.setText(event.getTitle());
                startDate.setTime(event.getStartDate());
                endDate.setTime(event.getEndDate());
                if (startDate != null) {
                    startDateTV.setText(new SimpleDateFormat("EEE d MMM", Locale.ENGLISH).format(startDate.getTime()));
                    startTimeTV.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(startDate.getTime()));
                    if (endDate != null && endDate.after(startDate)) {
                        endDateTV.setText(new SimpleDateFormat("EEE d MMM", Locale.ENGLISH).format(endDate.getTime()));
                        endTimeTV.setText(new SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(endDate.getTime()));
                    }
                }
                if (event.getDescription() != null)
                    descriptionEditText.setText(event.getDescription());
                RealmResults<Reminder> reminders = realm.where(Reminder.class).equalTo("eventID", event.getEventID()).findAll();
                if (!reminders.isEmpty()) {
                    for (Reminder reminder : reminders) {
                        addRemindView(reminder);
                    }
                }
            }
        } else {
            allowEditMode();
        }
        realm.close();

        scheduleClient = new ScheduleClient(this);
        scheduleClient.doBindService();
    }

    private void addRemindView(Reminder reminder) {
        ReminderView reminderView = new ReminderView(remindersLayout, reminder, this);
        reminderViews.add(reminderView);
    }

    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        userIsInteracting = true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_description, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.menu_save_button:
                if (invalidateData())
                    saveAndExit();
                return true;
            case R.id.menu_edit_button:
                allowEditMode();
                return true;
            case R.id.menu_remove_button:
                showRemoveDialog();
                return true;
            case R.id.home:
                if (editMode && isChanged)
                    showSaveDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void removeAndExit() {

        Realm realm = Realm.getInstance(this);
        Event event = realm.where(Event.class).equalTo("eventID", eventID).findFirst();
        if (event != null) {
            realm.beginTransaction();
            event.removeFromRealm();
            realm.commitTransaction();
            Intent listIntent = new Intent(DescriptionActivity.this, MobileListenerService.class);
            listIntent.setAction(MobileListenerService.Action_Sync);
            startService(listIntent);

            setResult(RESULT_OK);
        } else
            setResult(RESULT_CANCELED);
        realm.close();
        Intent intent = NavUtils.getParentActivityIntent(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        NavUtils.navigateUpTo(this, intent);
    }

    private void allowEditMode() {
        editMode = true;

        nameEditText.setFocusableInTouchMode(true);
        nameEditText.setFocusable(true);
        nameEditText.setCursorVisible(true);
        nameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setChanged(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0)
                    clearNameButton.setVisibility(View.VISIBLE);
                else
                    clearNameButton.setVisibility(View.GONE);
            }
        });
        clearNameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nameEditText.setText("");
            }
        });
        if (!nameEditText.getText().toString().isEmpty())
            clearNameButton.setVisibility(View.VISIBLE);

        startDateTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(startDate, datepicker_type_start);
            }
        });
        startTimeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(startDate, datepicker_type_start);
            }
        });
        clearStartDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDateTV.setText("");
                startTimeTV.setText("");
                startDate = Calendar.getInstance();
                clearStartDateButton.setVisibility(View.GONE);
                setChanged(true);
            }
        });
        if (!startDateTV.getText().toString().isEmpty())
            clearStartDateButton.setVisibility(View.VISIBLE);

        endDateTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(endDate, datepicker_type_end);
            }
        });
        endTimeTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker(endDate, datepicker_type_end);
            }
        });
        clearEndDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endDateTV.setText("");
                endTimeTV.setText("");
                if (startDate == null)
                    startDate = Calendar.getInstance();
                endDate.setTime(startDate.getTime());
                endDate.set(Calendar.SECOND, 0);
                clearEndDateButton.setVisibility(View.GONE);
                setChanged(true);
            }
        });
        if (!endDateTV.getText().toString().isEmpty())
            clearEndDateButton.setVisibility(View.VISIBLE);

        descriptionEditText.setFocusableInTouchMode(true);
        descriptionEditText.setFocusable(true);
        descriptionEditText.setCursorVisible(true);
        descriptionEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setChanged(true);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        addReminderButton.setVisibility(View.VISIBLE);
        addReminderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setChanged(true);
                addRemindView(new Reminder(eventID));
            }
        });

        for (ReminderView view : reminderViews) {
            view.setEditMode(true);
        }

        invalidateOptionsMenu();
    }

    @Override
    protected void onStop() {
        // When our activity is stopped ensure we also stop the connection to the service
        // this stops us leaking our activity into the system *bad*
        if (scheduleClient != null)
            scheduleClient.doUnbindService();
        super.onStop();
    }

    private void saveAndExit() {
        if (isChanged) {
            Realm realm = Realm.getInstance(getApplicationContext());
            Event event = realm.where(Event.class).equalTo("eventID", eventID).findFirst();
            if (event == null)
                event = new Event();

            String timezone;
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            boolean isLocally = preferences.getBoolean("locallyTimezone", true);

            if (isLocally)
                timezone = TimeZone.getDefault().getDisplayName();
            else
                timezone = preferences.getString("timezoneList", TimeZone.getDefault().getDisplayName());

            realm.beginTransaction();
            event.setTitle(nameEditText.getText().toString());
            if (!descriptionEditText.getText().toString().isEmpty())
                event.setDescription(descriptionEditText.getText().toString());
            else
                event.setDescription("");
            if (startDate != null) {
                startDate.setTimeZone(TimeZone.getTimeZone(timezone));
                event.setStartDate(startDate.getTime());
                if (endDate == null) {
                    event.setEndDate(startDate.getTime());
                } else {
                    endDate.setTimeZone(TimeZone.getTimeZone(timezone));
                    event.setEndDate(endDate.getTime());
                }
            }
            if (event.getEventID() == 0) {
                // increment index
                eventID = (int) (realm.where(Event.class).maximumInt("eventID") + 1);
                // insert new value
                event.setEventID(eventID);
                realm.copyToRealm(event);
            }
//            RealmResults<Reminder> reminders = realm.where(Reminder.class).equalTo("eventID", event.getEventID()).findAll();
//            reminders.clear();
            for (ReminderView view : reminderViews) {

                Reminder reminder = view.getReminder();
                if (realm.where(Reminder.class).equalTo("reminderID", reminder.getReminderID()).findFirst() == null) {
                    // increment index
                    int nextID = (int) (realm.where(Reminder.class).maximumInt("reminderID") + 1);
                    // insert new value
                    reminder.setReminderID(nextID);
                }
                int offset = (int) (view.getDate().getTimeInMillis() - startDate.getTimeInMillis()) / 1000;
                reminder.setAlertOffset(offset);
                reminder.setEventID(event.getEventID());
                realm.copyToRealmOrUpdate(reminder);
                Calendar c = Calendar.getInstance();
                c.setTime(view.getDate().getTime());
                c.set(Calendar.SECOND, 0);
                c.set(Calendar.MILLISECOND, 0);
                if (timezone != null)
                    c.setTimeZone(TimeZone.getTimeZone(timezone));
                scheduleClient.setAlarmForNotification(c, reminder.getReminderID());
//                new AlarmTask(this, c, reminder.getReminderID()).run();
            }
            realm.commitTransaction();
            realm.close();

            Intent listIntent = new Intent(DescriptionActivity.this, MobileListenerService.class);
            listIntent.setAction(MobileListenerService.Action_Sync);
            startService(listIntent);

            Intent data = new Intent();
            data.putExtra(getString(R.string.intent_event_id_key), eventID);
            setResult(RESULT_OK, data);
        } else
            setResult(RESULT_CANCELED);
        Intent intent = NavUtils.getParentActivityIntent(this);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        NavUtils.navigateUpTo(this, intent);
    }

    private boolean invalidateData() {
        if (nameEditText.getText().toString().isEmpty()) {
            Toast.makeText(this, "Name value cannot be empty!", Toast.LENGTH_SHORT).show();
            return false;
        } else if (startDateTV.getText().toString().isEmpty()) {
            Toast.makeText(this, "Start Date cannot be empty!", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!endDateTV.getText().toString().isEmpty()) {
            if (endDate.before(startDate)) {
                Toast.makeText(this, "End date cannot be before start date!", Toast.LENGTH_SHORT).show();
                endDateTV.setText("");
                endTimeTV.setText("");
                clearEndDateButton.setVisibility(View.GONE);
                return false;
            }
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        if (editMode && isChanged)
            showSaveDialog();
        else
            super.onBackPressed();
    }

    protected void showDatePicker(Calendar calendar, int type) {
        if (calendar == null)
            calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        DatePickerDialog dpd = null;
        switch (type) {
            case datepicker_type_start:
                dpd = DatePickerDialog.newInstance(startDateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                break;
            case datepicker_type_end:
                dpd = DatePickerDialog.newInstance(endDateSetListener, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
                break;
        }
        if (dpd != null) {
            dpd.setCancelable(true);
            dpd.show(getFragmentManager(), getString(R.string.datepicker_tag));
        }
    }

    private void showSaveDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
                .setTitle("Save and exit")
                .setMessage("Do you want save changes?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (invalidateData())
                            saveAndExit();
                        NavUtils.navigateUpFromSameTask(DescriptionActivity.this);
                    }
                }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setResult(RESULT_CANCELED);
                Intent intent = NavUtils.getParentActivityIntent(DescriptionActivity.this);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(DescriptionActivity.this, intent);
            }
        }).setNeutralButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void showRemoveDialog() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true)
                .setTitle("Remove event")
                .setMessage("Are you sure want to remove this event?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (invalidateData())
                            removeAndExit();
                        NavUtils.navigateUpFromSameTask(DescriptionActivity.this);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    protected void showTimePicker(Calendar calendar, int type) {
        if (calendar == null)
            calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, 0);
        TimePickerDialog tpd = null;
        switch (type) {
            case datepicker_type_start:
                tpd = TimePickerDialog.newInstance(startTimeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
                break;
            case datepicker_type_end:
                tpd = TimePickerDialog.newInstance(endTimeSetListener, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), false);
                break;
        }
        if (tpd != null) {
            tpd.setCancelable(true);
            tpd.show(getFragmentManager(), getString(R.string.timepicker_tag));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!editMode) {
            menu.findItem(R.id.menu_edit_button).setVisible(true);
            menu.findItem(R.id.menu_save_button).setVisible(false);
            menu.findItem(R.id.menu_remove_button).setVisible(false);
        } else {
            if (eventID != -1)
                menu.findItem(R.id.menu_remove_button).setVisible(true);
            if (isChanged) {
                menu.findItem(R.id.menu_edit_button).setVisible(false);
                menu.findItem(R.id.menu_save_button).setVisible(true);
            }
        }
        return true;
    }

    public void setChanged(boolean changed) {
        if (userIsInteracting) {
            this.isChanged = changed;
            invalidateOptionsMenu();
        }
    }

    public void removeReminder(ReminderView view) {
        remindersLayout.removeView(view.getView());
        reminderViews.remove(view);
        Realm realm = Realm.getInstance(this);
        if (view.getReminder().isValid()) {
            realm.beginTransaction();
            view.getReminder().removeFromRealm();
            realm.commitTransaction();
        }
        realm.close();
        setChanged(true);
    }

    public boolean getEditMode() {
        return editMode;
    }
}