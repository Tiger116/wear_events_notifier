package com.arcadia.wearapp;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.ArrayList;

public class WearBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        String type = bundle.getString("type");
        switch (type) {
            case WearListenerService.Mobile_List_Path:
                if (((WearApplication) context).getCurrentActivity() != null) {
                    Log.d(this.toString(), ((WearApplication) context).getCurrentActivity().getClass().getName());
                    String json = bundle.getString("json");
                    Gson gson = new GsonBuilder().setDateFormat(DateFormat.DEFAULT).create();
                    Type listType = new TypeToken<ArrayList<Event>>() {
                    }.getType();
                    ((WearApplication) context).events = gson.fromJson(json, listType);

                    Activity activity = ((WearApplication) context).getCurrentActivity();
                    if (activity.getClass().equals(WearMainActivity.class)) {
                        ((WearMainActivity) activity).redrawListView();
                    } else if (activity.getClass().equals(WearDescriptionActivity.class)) {
                        int eventID = ((WearDescriptionActivity) activity).getEventID();
                        ((WearDescriptionActivity) activity).setEvent(eventID);
                    }
                    break;
                }
        }
    }
}
