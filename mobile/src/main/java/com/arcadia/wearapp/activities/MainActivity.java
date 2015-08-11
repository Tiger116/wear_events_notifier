package com.arcadia.wearapp.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.arcadia.wearapp.DividerItemDecoration;
import com.arcadia.wearapp.realm_objects.Event;
import com.arcadia.wearapp.MobileListenerService;
import com.arcadia.wearapp.realm_objects.ParseGroup;
import com.arcadia.wearapp.R;
import com.arcadia.wearapp.adapters.RecyclerViewAdapter;
import com.getbase.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;

import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, View.OnLongClickListener, DrawerLayout.DrawerListener {

    private static final int ADD_EVENT_REQUEST_CODE = 2;
    private static final int EDIT_EVENT_REQUEST_CODE = 1;
    private RecyclerViewAdapter adapter;
    private RecyclerView recyclerView;
    private ActionBar actionBar;
    private DrawerLayout mDrawerLayout;
    private NavigationView navigationView;
    private View headView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        this.mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerLayout.setDrawerListener(this);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        headView = navigationView.inflateHeaderView(R.layout.nav_drawer_header);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                menuItem.setChecked(true);
                switch (menuItem.getItemId()) {
                    case R.id.nav_settings:
                        Intent settingsIntent = new Intent(MainActivity.this, SettingsActivity.class);
                        mDrawerLayout.closeDrawers();
                        startActivity(settingsIntent);
                        break;
                    case R.id.parse_groups_download:
                        Intent groupsIntent = new Intent(MainActivity.this, GroupsActivity.class);
                        mDrawerLayout.closeDrawers();
                        startActivity(groupsIntent);
                    case R.id.group_all:
                        adapter.setGroupID(null);
                        break;
                    case R.id.group_local:
                        adapter.setGroupID("");
                        break;
                    default:
                        Realm realm = Realm.getInstance(MainActivity.this);
                        ParseGroup group = realm.where(ParseGroup.class).equalTo("title", menuItem.getTitle().toString()).findFirst();
                        adapter.setGroupID(group.getGroupID());
                        realm.close();
                        break;
                }
                mDrawerLayout.closeDrawers();
                return true;
            }
        });

        if (getIntent().getAction().equals(MobileListenerService.Action_Open_Event)) {
            if (getIntent().getExtras().containsKey(getString(R.string.intent_event_id_key))) {
                Intent openIntent = new Intent(MainActivity.this, DescriptionActivity.class);
                openIntent.putExtra(getString(R.string.intent_event_id_key), getIntent().getExtras().getInt(getString(R.string.intent_event_id_key)));
                startActivityForResult(openIntent, EDIT_EVENT_REQUEST_CODE);
            }
        }

        LinearLayoutManager manager = new LinearLayoutManager(this);

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        adapter = new RecyclerViewAdapter(this);
        adapter.setOnClickListener(this);
        adapter.setOnLongClickListener(this);

        populateEvents();

        adapter.update();
        recyclerView.setAdapter(adapter);
        syncWithWear();

        recyclerView.setLayoutManager(manager);
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        FloatingActionButton button = (FloatingActionButton) findViewById(R.id.floating_add_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, DescriptionActivity.class);
                startActivityForResult(intent, ADD_EVENT_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        Menu menu = navigationView.getMenu();
        MenuInflater inflater = getMenuInflater();

        menu.clear();
        inflater.inflate(R.menu.drawer_view, menu);

        Realm realm = Realm.getInstance(this);
        RealmResults<ParseGroup> resultList = realm.allObjectsSorted(ParseGroup.class, "title", true);
        realm.close();
        if (!resultList.isEmpty()) {
            for (int i = 0; i < resultList.size(); i++) {
                ParseGroup group = resultList.get(i);
                if (realm.where(Event.class).contains("groupID", group.getGroupID()).findFirst() != null)
                    menu.add(R.id.menu_group_groups, 0, i + 2, group.getTitle());
            }
            menu.setGroupCheckable(R.id.menu_group_groups, true, true);
        }
        adapter.notifyDataSetChanged();
    }

    public void populateEvents() {
        Realm realm = Realm.getInstance(this);
        RealmResults<Event> results = realm.allObjects(Event.class);

        if (results.isEmpty()) {
            for (int i = 1; i < 8; i++) {
                Event event = new Event(String.format("Item no. %d", i));
                // increment index
                int nextID = (int) (realm.where(Event.class).maximumInt("eventID") + 1);

                // insert new value
                event.setEventID(nextID);

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.SECOND, 0);
                calendar.add(Calendar.DAY_OF_YEAR, 7 - i);
                event.setStartDate(calendar.getTime());
                event.setEndDate(calendar.getTime());

                realm.beginTransaction();
                realm.copyToRealmOrUpdate(event);
                realm.commitTransaction();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.getFilter().filter(newText);
                return true;
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mDrawerLayout != null)
                    if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                        mDrawerLayout.closeDrawer(GravityCompat.START);
                    } else
                        mDrawerLayout.openDrawer(GravityCompat.START);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ADD_EVENT_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    adapter.update();
                }
                break;
            case EDIT_EVENT_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    adapter.update();
                }
        }
    }

    private void syncWithWear() {
        Intent listIntent = new Intent(MainActivity.this, MobileListenerService.class);
        listIntent.setAction(MobileListenerService.Action_Sync);
        startService(listIntent);
    }

    @Override
    public void onClick(View v) {
        int position = recyclerView.getChildLayoutPosition(v);
        Intent intent = new Intent(MainActivity.this, DescriptionActivity.class);

        Event event = adapter.getItem(position);

        intent.putExtra(getString(R.string.intent_event_id_key), event.getEventID());
        startActivityForResult(intent, EDIT_EVENT_REQUEST_CODE);
    }

    @Override
    public boolean onLongClick(View v) {
//        int position = recyclerView.getChildLayoutPosition(v);
        Toast.makeText(MainActivity.this, "Open event description", Toast.LENGTH_SHORT).show();
        return true;
    }

    @Override
    public void onDrawerSlide(View drawerView, float slideOffset) {

    }

    @Override
    public void onDrawerOpened(View drawerView) {
        if (headView != null)
//            updateStatus();
            if (actionBar != null) {
                actionBar.setHomeAsUpIndicator(R.drawable.ic_arrow_back_white_24dp);
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
    }

    @Override
    public void onDrawerClosed(View drawerView) {
        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.ic_menu_white_24dp);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onDrawerStateChanged(int newState) {

    }
}
