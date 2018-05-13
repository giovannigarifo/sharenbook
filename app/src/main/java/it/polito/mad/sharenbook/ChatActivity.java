package it.polito.mad.sharenbook;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.onesignal.OneSignal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;


public class ChatActivity extends AppCompatActivity {
    LinearLayout layout;
    RelativeLayout layout_2;
    ImageView sendButton;
    EditText messageArea;
    ScrollView scrollView;
    DatabaseReference chatToOthersReference, chatFromOthersReference;
    String recipientUsername;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        layout = (LinearLayout) findViewById(R.id.layout1);
        layout_2 = (RelativeLayout)findViewById(R.id.layout2);
        sendButton = (ImageView)findViewById(R.id.sendButton);
        messageArea = (EditText)findViewById(R.id.messageArea);
        scrollView = (ScrollView)findViewById(R.id.scrollView);

        recipientUsername = getIntent().getStringExtra("recipientUsername");

        chatToOthersReference = FirebaseDatabase.getInstance().getReference("chats").child("/" + App.username + "_" + recipientUsername);
        chatFromOthersReference = FirebaseDatabase.getInstance().getReference("chats").child("/" + recipientUsername + "_" +App.username);

        sendButton.setOnClickListener(v -> {
            String messageText = messageArea.getText().toString();

            if(!messageText.equals("")){
                Map<String, String> map = new HashMap<String, String>();
                map.put("message", messageText);
                map.put("user", App.username);
                sendNotification(recipientUsername);
                chatToOthersReference.push().setValue(map);
                chatFromOthersReference.push().setValue(map);
                messageArea.setText("");
            }
        });

        chatToOthersReference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Map<Object, Object> map = (Map<Object, Object>) dataSnapshot.getValue();
                String message = map.get("message").toString();
                String userName = map.get("user").toString();

                if(userName.equals(App.username)){
                    addMessageBox(message, 1);
                }
                else{
                    addMessageBox(userName + ":\n" + message, 2);
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
        });

    }

    public void addMessageBox(String message, int type){
        TextView textView = new TextView(ChatActivity.this);
        textView.setText(message);

        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.weight = 1.0f;

        final float scale = getApplicationContext().getResources().getDisplayMetrics().density;
        int pixels = (int) (300 * scale + 0.5f);

        if(type == 1) {
            lp2.gravity = Gravity.RIGHT;
            textView.setBackgroundResource(R.drawable.message_bubble_out);
        }
        else{
            lp2.gravity = Gravity.LEFT;
            textView.setBackgroundResource(R.drawable.message_bubble_in);
        }

        textView.setTextColor(getResources().getColor(R.color.white));
        textView.setMaxWidth(pixels);
        textView.setTypeface(null, Typeface.BOLD);
        textView.setPadding(24, 24,24,24);
        textView.setLayoutParams(lp2);
        layout.addView(textView);
        scrollView.fullScroll(View.FOCUS_DOWN);
    }

    public void sendNotification(String destination){
        AsyncTask.execute(() -> {
            int SDK_INT = Build.VERSION.SDK_INT;
            if(SDK_INT > 8){
                Log.d("notification", "sending to " + destination);
                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                        .permitAll().build();
                StrictMode.setThreadPolicy(policy);

                try{
                    String jsonResponse;

                    URL url = new URL("https://onesignal.com/api/v1/notifications");
                    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    conn.setUseCaches(false);
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Authorization", "Basic ZTc3MjExODEtYmM4Yy00YjU5LWFjNWEtM2VlNGNmYTA0OWU1");
                    conn.setRequestMethod("POST");

                    String strJsonBody = "{"
                            + "\"app_id\": \"edfbe9fb-e0fc-4fdb-b449-c5d6369fada5\","

                            + "\"filters\": [{\"field\": \"tag\", \"key\": \"User_ID\", \"relation\": \"=\", \"value\": \"" + destination + "\"}],"

                            + "\"data\": {\"foo\": \"bar\"},"
                            + "\"contents\": {\"en\": \"English Message\"}"
                            + "}";

                    System.out.println("strJsonBody:" + strJsonBody);

                    byte[] sendBytes = strJsonBody.getBytes("UTF-8");
                    conn.setFixedLengthStreamingMode(sendBytes.length);

                    OutputStream outputStream = conn.getOutputStream();
                    outputStream.write(sendBytes);

                    int httpResponse = conn.getResponseCode();
                    System.out.println("httpResponse: " + httpResponse);

                    if (httpResponse >= HttpURLConnection.HTTP_OK
                            && httpResponse < HttpURLConnection.HTTP_BAD_REQUEST) {
                        Log.d("notification", "done");
                        Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8");
                        jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                        scanner.close();
                    } else {
                        Scanner scanner = new Scanner(conn.getErrorStream(), "UTF-8");
                        jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                        scanner.close();
                    }
                    System.out.println("jsonResponse:\n" + jsonResponse);


                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }

}
