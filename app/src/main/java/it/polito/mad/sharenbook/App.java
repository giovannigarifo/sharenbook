package it.polito.mad.sharenbook;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by claudiosava on 20/04/18.
 */

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }
}
