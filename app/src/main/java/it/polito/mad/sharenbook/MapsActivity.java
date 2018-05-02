package it.polito.mad.sharenbook;

import android.content.pm.PackageManager;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import it.polito.mad.sharenbook.model.Book;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private final int location_request_code = 1122;
    private ArrayList<Book> searchResults;
    private boolean showBooks = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //TODO get Bundle -> announcements of books
        Bundle extras = getIntent().getExtras();
        if(!extras.isEmpty()) {
            searchResults = extras.getParcelableArrayList("SearchResults");
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        /*LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));*/
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION
        )
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);

            if(searchResults != null)
                setAnnouncementMarkers();

        } else {
            Toast.makeText(getApplicationContext(), "Niente", Toast.LENGTH_SHORT).show();
            //TODO add ask for permissions here or something similar
        }

    }

    public void setAnnouncementMarkers(){
        for(Book b : searchResults){
            LatLng loc = new LatLng(Double.parseDouble(b.getLocationLat()), Double.parseDouble(b.getLocationLong()));
            mMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .title(b.getTitle()));
        }
    }

}
