package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

import it.polito.mad.sharenbook.adapters.MessageAdapter;
import it.polito.mad.sharenbook.model.Message;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.UserInterface;
import it.polito.mad.sharenbook.utils.Utils;


public class ChatActivity extends AppCompatActivity {

    private ListView messageView;
    private ImageView sendButton;
    private EditText messageArea;
    private DatabaseReference chatToOthersReference, chatFromOthersReference;
    public static String recipientUsername;
    private ImageView iv_profile;
    private TextView tv_username;
    private ImageButton im_back_button;


    private boolean lastMessageNotFromCounterpart = false;
    private boolean activityWasOnPause =false, isOnPause = false;
    private String username, userID;
    private long picSignature = 0;

    public static boolean chatOpened = false;
    private boolean openedFromNotification;
    private boolean firstTimeNotViewed = true;

    private FirebaseUser firebaseUser;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference serverTimeRef;

    private ChildEventListener childEventListener;
    private ValueEventListener readServerTime;

    /** adapter setting **/
    private MessageAdapter messageAdapter;

    private long unixTime;

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        intent.putExtra("openedFromNotification", false);
        setIntent(intent);
        this.recreate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        messageView = findViewById(R.id.chat_list_view);
        sendButton = findViewById(R.id.sendButton);
        messageArea = findViewById(R.id.messageArea);
        iv_profile = findViewById(R.id.iv_profile);
        tv_username = findViewById(R.id.tv_username);
        im_back_button = findViewById(R.id.back_button);

        recipientUsername = getIntent().getStringExtra("recipientUsername");
        openedFromNotification = getIntent().getBooleanExtra("openedFromNotification", false);

        tv_username.setText(recipientUsername);

        SharedPreferences userData = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(getString(R.string.username_copy_key), "");

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        userID = firebaseUser.getUid();

        StorageReference profilePicRef = FirebaseStorage.getInstance().getReference().child("images/" + recipientUsername +".jpg");

        messageAdapter = new MessageAdapter(ChatActivity.this,profilePicRef);
        messageView.setAdapter(messageAdapter);

        //show recipient profile pic
        DatabaseReference recipientPicSignature = FirebaseDatabase.getInstance().getReference("usernames").child(recipientUsername).child("picSignature");
        recipientPicSignature.addListenerForSingleValueEvent(new ValueEventListener() {
             @Override
             public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    picSignature = (long) dataSnapshot.getValue();
                    messageAdapter.setPicSignature(picSignature);
                    UserInterface.showGlideImage(getApplicationContext(), profilePicRef, iv_profile, picSignature);
                } else {
                    GlideApp.with(getApplicationContext()).load(getResources().getDrawable(R.drawable.ic_profile)).into(iv_profile);
                }


             }

             @Override
             public void onCancelled(DatabaseError databaseError) {

             }
         });

        chatToOthersReference = FirebaseDatabase.getInstance().getReference("chats").child(username).child(recipientUsername);
        chatFromOthersReference = FirebaseDatabase.getInstance().getReference("chats").child(recipientUsername).child(username);

        serverTimeRef = FirebaseDatabase.getInstance().getReferenceFromUrl("https://sharenbook-debug.firebaseio.com/server_timestamp");

        readServerTime = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                unixTime = (Long) dataSnapshot.getValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        serverTimeRef.setValue(ServerValue.TIMESTAMP);
        serverTimeRef.addListenerForSingleValueEvent(readServerTime);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                Map<String, Object> map = (Map<String, Object>) dataSnapshot.getValue();

                String messageBody = map.get("message").toString();
                String userName = map.get("user").toString();
                Boolean viewed = (Boolean) map.get("viewed");

                long date = 0;
                if(map.get("date_time")!=null){
                    date = (long)map.get("date_time");
                }
                Message message;
                //MESSAGE ADD -> MESSAGE string message, int type, string username
                if(userName.equals(username)){

                    message = new Message(messageBody,true, userName, lastMessageNotFromCounterpart, date, ChatActivity.this);
                    messageAdapter.addMessage(message);
                    messageView.setSelection(messageView.getCount() - 1);
                    lastMessageNotFromCounterpart = false;

                }
                else{

                    if(!viewed){

                        if((date < unixTime && firstTimeNotViewed) || (date < unixTime && activityWasOnPause) || (openedFromNotification && firstTimeNotViewed)){
                            message = new Message(null, true, null, lastMessageNotFromCounterpart, 0, ChatActivity.this);
                            messageAdapter.addMessage(message);
                            firstTimeNotViewed = false;
                            activityWasOnPause = false;
                        }

                        map.put("viewed", true);
                        dataSnapshot.getRef().updateChildren(map);

                    }

                    message = new Message(messageBody,false,userName, lastMessageNotFromCounterpart, date, ChatActivity.this);

                    lastMessageNotFromCounterpart = true;

                    messageAdapter.addMessage(message);
                    messageView.setSelection(messageView.getCount() - 1);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

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
        };
        chatToOthersReference.addChildEventListener(childEventListener);

        sendButton.setOnClickListener(v -> {
            String messageText = messageArea.getText().toString();

            if(!messageText.equals("")){
                Map<String,Object> map = new HashMap<>();
                map.put("message", messageText);
                map.put("user", username);
                map.put("date_time", ServerValue.TIMESTAMP);
                map.put("viewed", true);

                String strJsonBody = "{"
                        + "\"app_id\": \"edfbe9fb-e0fc-4fdb-b449-c5d6369fada5\","

                        + "\"filters\": [{\"field\": \"tag\", \"key\": \"User_ID\", \"relation\": \"=\", \"value\": \"" + recipientUsername + "\"}],"

                        + "\"data\": {\"notificationType\": \"message\", \"senderName\": \"" + username + "\", \"senderUid\": \"" + userID + "\"},"
                        + "\"contents\": {\"en\": \"" + username + " sent you a message!\", " +
                        "\"it\": \"" + username + " ti ha inviato un messaggio!\"},"
                        + "\"headings\": {\"en\": \"New message!\", \"it\": \"Nuovo messaggio!\"}"
                        + "}";

                Utils.sendNotification(strJsonBody);

                chatToOthersReference.push().setValue(map);
                Map<String,Object> map2 = new HashMap<>();
                map2.put("message", messageText);
                map2.put("user", username);
                map2.put("date_time", ServerValue.TIMESTAMP);
                map2.put("viewed", false);
                chatFromOthersReference.push().setValue(map2);
                messageArea.setText("");
            }
        });

        im_back_button.setOnClickListener(v -> {
            if(openedFromNotification){
                Intent i = new Intent (getApplicationContext(), MyChatsActivity.class);
                startActivity(i);
            }
            chatToOthersReference.removeEventListener(childEventListener);
            finish();
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        chatOpened = true;

    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        chatOpened = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if(openedFromNotification){
            Intent i = new Intent (getApplicationContext(), MyChatsActivity.class);
            startActivity(i);
        }
        chatToOthersReference.removeEventListener(childEventListener);
        finish();
    }

    @Override
    protected void onPause() {
        chatToOthersReference.removeEventListener(childEventListener);
        isOnPause = true;
        activityWasOnPause = true;

        super.onPause();
    }

    @Override
    protected void onResume() {
        if(isOnPause) {
            serverTimeRef.setValue(ServerValue.TIMESTAMP);
            serverTimeRef.addListenerForSingleValueEvent(readServerTime);
            messageAdapter.clearMessages();
            isOnPause = false;
            chatToOthersReference.addChildEventListener(childEventListener);
        }
        super.onResume();
    }
}
