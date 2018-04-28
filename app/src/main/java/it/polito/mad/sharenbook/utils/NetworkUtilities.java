package it.polito.mad.sharenbook.utils;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.utils.ConnectionChangedListener;

public class NetworkUtilities {

    private static boolean isConnected = false;
    private static List<ConnectionChangedListener> listeners = new ArrayList<>();

    public static boolean isConnected() { return isConnected; }

    private static void setConnectionState(boolean connState) {
        isConnected = connState;

        if(!isConnected) {
            for (ConnectionChangedListener l : listeners) {
                l.OnConnectionStateChanged();
            }
        }
    }

    public static void checkNetworkConnection() {

        DatabaseReference connectedRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        connectedRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {

                    boolean result = snapshot.getValue(Boolean.class);

                    if(result != isConnected)
                        setConnectionState(result);

                    if (isConnected) {
                        System.out.println("Internet: Connected");
                    } else {
                        System.out.println("Internet:  Not connected");
                    }

            }

            @Override
            public void onCancelled(DatabaseError error) {
                System.err.println("Listener was cancelled");
            }
        });
    }

    public static void addConnectionStateListener(ConnectionChangedListener l) {
        listeners.add(l);
    }

    public static void removeConnectionStateListener(ConnectionChangedListener l) {
        listeners.remove(l);
    }

}
