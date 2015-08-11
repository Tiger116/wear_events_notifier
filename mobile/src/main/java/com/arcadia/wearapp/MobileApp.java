package com.arcadia.wearapp;

import android.app.Application;

import com.arcadia.wearapp.realm_objects.ParseGroup;
import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.List;

import io.realm.Realm;

public class MobileApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        // Enable Local Datastore.
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "6pykGQdt4liKrk2ImWd0faAAjQL9RyXNeHftbiMh", "g3VGjDURrtgwYTCzUNw0vhkwB2zgK3hNk5Pv8pXW");

        getFromParse();
    }

    public void getFromParse() {
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Group");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                Realm realm = Realm.getInstance(MobileApp.this);

                realm.beginTransaction();
                realm.clear(ParseGroup.class);
                for (ParseObject object : list) {
                    ParseGroup group = new ParseGroup(object.getObjectId());
                    Object title = object.get("title");
                    Object description = object.get("description");
                    if (title != null)
                        group.setTitle(title.toString());
                    if (description != null)
                        group.setDescription(description.toString());
                    realm.copyToRealmOrUpdate(group);
                }
                realm.commitTransaction();

                realm.close();
            }
        });
    }
}
