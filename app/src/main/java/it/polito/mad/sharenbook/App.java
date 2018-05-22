package it.polito.mad.sharenbook;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import com.google.firebase.database.FirebaseDatabase;
import com.onesignal.OneSignal;

import it.polito.mad.sharenbook.utils.NotificationOpenedHandler;

/**
 * Created by Davide on 10/05/2018.
 */


public class App extends MultiDexApplication {

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        initOneSignal();
        mContext = this;

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseDatabase.setPersistenceEnabled(true);           //set Persistance only one time at app startup
    }

    private void initOneSignal(){
        OneSignal.startInit(this)
                .setNotificationOpenedHandler(new NotificationOpenedHandler(getApplicationContext()))//set open handler
                .inFocusDisplaying(OneSignal.OSInFocusDisplayOption.Notification)
                .unsubscribeWhenNotificationsAreDisabled(true)
                .init();
    }

    public static Context getContext(){
        return mContext;
    }


}
