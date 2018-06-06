package it.polito.mad.sharenbook.utils;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.onesignal.OSNotificationOpenResult;
import com.onesignal.OneSignal;

import org.json.JSONException;

import it.polito.mad.sharenbook.ChatActivity;
import it.polito.mad.sharenbook.MyBookActivity;

public class NotificationOpenedHandler implements OneSignal.NotificationOpenedHandler {

    private Context context;

    public NotificationOpenedHandler(Context context) {
        this.context = context;
    }

    // This fires when a notification is opened by tapping on it.
    @Override
    public void notificationOpened(OSNotificationOpenResult result) {

        String notificationType = null;
        try {
            notificationType = result.notification.payload.additionalData.getString("notificationType");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(notificationType != null && notificationType.equals("message")){     //notification was because user received a new message

            //open the chat
            String senderName = null, senderUid = null;
            try {
                senderName = result.notification.payload.additionalData.getString("senderName");
                senderUid = result.notification.payload.additionalData.getString("senderUid");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(senderName != null && senderUid != null){
                Intent chatActivity = new Intent(context, ChatActivity.class);
                chatActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                chatActivity.putExtra("recipientUsername", senderName);
                chatActivity.putExtra("openedFromNotification", true);
                context.startActivity(chatActivity);
            }

        }
        else if (notificationType != null && notificationType.equals("bookRequest")){

            Intent myBookActivity = new Intent(context, MyBookActivity.class);
            myBookActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            myBookActivity.putExtra("openedFromNotification", true);
            myBookActivity.putExtra("showPageNum", 1);
            context.startActivity(myBookActivity);

        }
        else if (notificationType != null && (notificationType.equals("AcceptedRequest")||notificationType.equals("returnedBook"))){

            Intent myBookActivity = new Intent(context, MyBookActivity.class);
            myBookActivity.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            myBookActivity.putExtra("openedFromNotification", true);
            myBookActivity.putExtra("showPageNum", 2);
            context.startActivity(myBookActivity);

        }

    }

}