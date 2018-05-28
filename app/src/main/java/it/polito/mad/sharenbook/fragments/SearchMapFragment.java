package it.polito.mad.sharenbook.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.SearchActivity;
import it.polito.mad.sharenbook.ShowBookActivity;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.adapters.CustomInfoWindowAdapter;


public class SearchMapFragment extends Fragment  implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener{

    //reference to the parent activity
    SearchActivity searchActivity;

    //algolia result obtained from parent
    private ArrayList<Book> searchResult; //list of book that matched the query

    //map
    GoogleMap googleMap;

    //range circle
    Circle distanceFilterCircle;


    public SearchMapFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     */
    public static SearchMapFragment newInstance(String param1, String param2) {

        SearchMapFragment fragment = new SearchMapFragment();
        fragment.setRetainInstance(true); //save state when rotating
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.searchActivity = (SearchActivity) getActivity(); //get parent activity reference
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_search_map, container, false);

        //get map asynchronously
        ((SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.search_map))
                .getMapAsync(this);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        this.searchResult = searchActivity.getSearchResult(); //read the collection to be displayed
    }


    @Override
    public void onMapReady(GoogleMap gMap) {

        //get obtained map
        this.googleMap = gMap;

        //map setup
        if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(false);
            if(this.searchResult != null)
                setAnnouncementMarkers();

        } else {
            Toast.makeText(getContext(), "No permissions", Toast.LENGTH_SHORT).show();
        }
    }


    public void setAnnouncementMarkers(){

        //flush all previous markers
        this.googleMap.clear();

        //set new markers
        for(Book b : searchResult){

            LatLng loc = new LatLng(b.getLocation_lat(), b.getLocation_long());

            Marker m = this.googleMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .title(b.getTitle()));

            m.setTag(b); //associate book object to this marker

            //Set Custom InfoWindow Adapter
            CustomInfoWindowAdapter adapter = new CustomInfoWindowAdapter(getContext()); //TODO: change adapter implementation
            this.googleMap.setInfoWindowAdapter(adapter);
        }

        this.googleMap.setOnInfoWindowClickListener(this);
        this.googleMap.setOnMarkerClickListener(this);
    }


    /** Called when the user clicks a marker. */
    @Override
    public boolean onMarkerClick(final Marker marker) {

        // Return false to indicate that we have not consumed the event and that we wish
        // for the default behavior to occur (which is for the camera to move such that the
        // marker is centered and for the marker's info window to open, if it has one).
        return false;
    }

    @Override
    public void onInfoWindowClick(Marker marker) {

        // Retrieve the data from the marker.
        Book book = (Book) marker.getTag();

        Intent showBook = new Intent(getContext(), ShowBookActivity.class);

        if(!searchResult.isEmpty()){
            Bundle bundle = new Bundle();
            bundle.putParcelable("book", book);
            showBook.putExtras(bundle);
        }
        startActivity(showBook);
    }


    public void showFilterDistanceCircle(Address place, int range, int color){

        LatLng point = new LatLng(place.getLatitude(), place.getLongitude());

        CircleOptions circleOptions = new CircleOptions()
                .center(point)   //set center
                .radius(range*1000)   //set radius in meters
                .fillColor(Color.TRANSPARENT)  //default
                .strokeColor(color)
                .strokeWidth(8);

        distanceFilterCircle = googleMap.addCircle(circleOptions);

        //center map into circle
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(place.getLatitude(), place.getLongitude()), range*2));
    }


    /**
     * Method called from SearchActivity to notify the fragment that the searchResult collection has changed
     */
    public void updateDisplayedSearchResult() {

        if(this.searchResult != null && this.googleMap!= null)
            setAnnouncementMarkers();
    }

}
