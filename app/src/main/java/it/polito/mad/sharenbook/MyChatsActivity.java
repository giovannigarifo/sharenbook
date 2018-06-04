package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.onesignal.OneSignal;

import java.util.Map;
import java.util.Set;

import it.polito.mad.sharenbook.adapters.ConversationAdapter;
import it.polito.mad.sharenbook.model.Conversation;
import it.polito.mad.sharenbook.model.Message;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.UserInterface;

public class MyChatsActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {


    private BottomNavigationView navBar;
    private SharedPreferences userPreferences;
    private ListView chatsListView;
    private ConversationAdapter adapter;
    private DatabaseReference mychatsDB;

    private NavigationView navigationView;
    private DrawerLayout drawer;
    private CircularImageView drawer_userPicture;
    private TextView drawer_fullname;
    private TextView drawer_email;
    private CardView no_chats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_chats);
        setupNavigationTools();
        chatsListView = findViewById(R.id.my_chats_lv);
        no_chats = findViewById(R.id.card_no_chats);

        adapter = new ConversationAdapter(MyChatsActivity.this);
        chatsListView.setAdapter(adapter);

        userPreferences = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        String username = userPreferences.getString(getString(R.string.username_copy_key), "void");

        if (!username.equals("void")){
            mychatsDB = FirebaseDatabase.getInstance().getReference("chats").child(username);


            /**take actual number of conversations **/

            mychatsDB.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    long actualChats = dataSnapshot.getChildrenCount();

                    if(actualChats == 0)
                        no_chats.setVisibility(View.VISIBLE);

                    adapter.setWithoutIncomingMessagesCounter(actualChats);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        mychatsDB.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                dataSnapshot.getRef().orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(adapter.getCount() == 0){
                            no_chats.setVisibility(View.GONE);
                        }

                        setConversationAndApapter(dataSnapshot, true);


                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
               // Log.d("conversations changed:", "->" + dataSnapshot.getKey() + "and" + s);
                dataSnapshot.getRef().orderByKey().limitToLast(1).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                     //   Log.d("conversation chg lmsg", dataSnapshot.toString());
                        setConversationAndApapter(dataSnapshot, false);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    }


    private void setConversationAndApapter(DataSnapshot dataSnapshot, boolean newConversation){
        /** Construct the message **/
        Message message = null;
        Map<String, Map<String,Object>> snapshot =( Map<String, Map<String,Object>>) dataSnapshot.getValue();
        Map<String,Object> messageMap;
        Set<String> key = snapshot.keySet();
        for(String s:key){
            messageMap = snapshot.get(s);
            message = new Message(messageMap.get("message").toString(),false,messageMap.get("user").toString(),
                    false,(long)messageMap.get("date_time"),MyChatsActivity.this);
            message.setViewed((boolean)messageMap.get("viewed"));
            if(message.isViewed())
                Log.d("conversation","added true");
            else
                Log.d("conversation","added false");
        }

        Conversation conversation = new Conversation(dataSnapshot.getKey(),message, 0,
                FirebaseStorage.getInstance().getReference().child("images/" + dataSnapshot.getKey() +".jpg"));

        if(newConversation) {
            adapter.addConversation(conversation);
            Log.d("chatpicture","new recipient->"+dataSnapshot.getKey()+" path->"+"images/" + dataSnapshot.getKey() +".jpg");
        }
        else {
            adapter.modifyConversation(conversation);
            Log.d("chatpicture","mod recipient->"+dataSnapshot.getKey()+" path->"+"images/" + dataSnapshot.getKey() +".jpg");
        }
    }

    private void setupNavigationTools() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.sba_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.mychats_title);
        }

        // Setup navigation drawer
        drawer = findViewById(R.id.my_chats_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.my_chats_nav_view);
        navigationView.setNavigationItemSelectedListener(MyChatsActivity.this);

        // Update drawer with user info
        View nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        drawer_email = nav.findViewById(R.id.drawer_user_email);



        // Setup bottom navbar
        UserInterface.setupNavigationBar(this,R.id.navigation_chat);
        navBar = findViewById(R.id.navigation);
    }


    /**
     * Navigation Drawer Listeners
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        return NavigationDrawerManager.onNavigationItemSelected(this,null,item,getApplicationContext(),drawer,0);

        }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.my_chats_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        OneSignal.clearOneSignalNotifications();
        NavigationDrawerManager.setDrawerViews(getApplicationContext(), getWindowManager(), drawer_fullname,
                drawer_email, drawer_userPicture, NavigationDrawerManager.getNavigationDrawerProfile());
        navBar.setSelectedItemId(R.id.navigation_chat);
        navigationView.setCheckedItem(R.id.drawer_navigation_none);
    }
}


