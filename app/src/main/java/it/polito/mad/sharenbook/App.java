package it.polito.mad.sharenbook;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;
import com.onesignal.OneSignal;

import it.polito.mad.sharenbook.utils.NotificationOpenedHandler;

/**
 * Created by Davide on 10/05/2018.
 */


public class App extends Application {
    
    private FirebaseDatabase firebaseDatabase;

    @Override
    public void onCreate() {
        super.onCreate();
        initOneSignal();

        firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);           //set Persistance only one time at app startup

    }

    private void initOneSignal(){
        OneSignal.startInit(this)
                .setNotificationOpenedHandler(new NotificationOpenedHandler(getApplicationContext()))      //set open handler
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
    }

}
