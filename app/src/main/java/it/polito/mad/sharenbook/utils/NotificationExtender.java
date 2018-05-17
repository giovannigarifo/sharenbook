package it.polito.mad.sharenbook.utils;

import android.support.v4.app.NotificationBuilderWithBuilderAccessor;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.onesignal.NotificationExtenderService;
import com.onesignal.OSNotificationDisplayedResult;
import com.onesignal.OSNotificationReceivedResult;

import org.json.JSONException;

import java.math.BigInteger;

import it.polito.mad.sharenbook.ChatActivity;

public class NotificationExtender extends NotificationExtenderService {

    private String senderName = null;

    @Override
    protected boolean onNotificationProcessing(OSNotificationReceivedResult result) {



        // Read properties from result.
        String notificationType = null;
        try {
            notificationType = result.payload.additionalData.getString("notificationType");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(notificationType != null && notificationType.equals("message")) {     //notification was because user received a new message

            //open the chat
            String senderUid = null;
            try {
                senderName = result.payload.additionalData.getString("senderName");
                senderUid = result.payload.additionalData.getString("senderUid");

                /*OverrideSettings overrideSettings = new OverrideSettings();
                overrideSettings.extender = builder -> {
                    builder.setGroup(senderName);
                    return builder;
                };

                OSNotificationDisplayedResult displayedResult = displayNotification(overrideSettings);*/

            } catch (JSONException e) {
                e.printStackTrace();
            }

            if(ChatActivity.chatOpened){

                    // Returning true tells the OneSignal SDK you have processed the notification and not to display it's own.
                    return ChatActivity.recipientUsername != null && ChatActivity.recipientUsername.equals(senderName);

            } else
                return false;

        } else
            return false;


    }
}