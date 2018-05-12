package it.polito.mad.sharenbook;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.algolia.instantsearch.helpers.InstantSearch;
import com.algolia.instantsearch.helpers.Searcher;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.robertlevonyan.views.chip.Chip;
import com.robertlevonyan.views.chip.OnChipClickListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.polito.mad.sharenbook.fragments.DistanceFilterFragment;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.CustomInfoWindowAdapter;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;

public class MapsActivity extends FragmentActivity
        implements OnMapReadyCallback, MaterialSearchBar.OnSearchActionListener, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener
,NavigationView.OnNavigationItemSelectedListener{

    private GoogleMap mMap;
    private ArrayList<Book> searchResult = null;

    private MaterialSearchBar sba_searchbar;

    private DrawerLayout drawer;
    //fab to display map
    FloatingActionButton search_fab_list;

    Chip filterDistanceChip;

    DatabaseReference location_ref = FirebaseDatabase.getInstance().getReference("books_locations");
    GeoFire geoFire = new GeoFire(location_ref);

    // Algolia instant search
    Searcher searcher;
    InstantSearch helper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        Bundle extras = getIntent().getExtras();
        if(!extras.isEmpty()) {
            searchResult = extras.getParcelableArrayList("SearchResults");
            if (searchResult == null) {
                searchResult = new ArrayList<>();
            }
        } else {
            searchResult = new ArrayList<>();
        }

        //Algolia's InstantSearch setup
        // searcher = Searcher.create("4DWHVL57AK", "03391b3ea81e4a5c37651a677670bcb8", "books");
        searcher = Searcher.create("K7HV32WVKQ", "04a25396f978e2d22348e5520d70437e", "books");
        helper = new InstantSearch(searcher);

        searcher.registerResultListener((results, isLoadingMore) -> {

            if (results.nbHits > 0) {

                searchResult.clear();
                searchResult.addAll(parseResults(results.hits));
                showSearchResults();
                sba_searchbar.disableSearch();

            } else {
                Toast.makeText(getApplicationContext(), R.string.sa_no_results, Toast.LENGTH_LONG).show();
            }
        });

        searcher.registerErrorListener((query, error) -> {

            Toast.makeText(getApplicationContext(), R.string.sa_no_results, Toast.LENGTH_LONG).show();
            Log.d("error", "Unable to retrieve search isValid from Algolia");
        });

        setListButton();

        setDrawerAndSearchBar();

        setChipFilters();

    }


    /**
     * setDrawerAndSearchBar
     */
    private void setDrawerAndSearchBar() {

        /** DRAWER AND SEARCHBAR **/

        drawer = findViewById(R.id.map_search_drawer_layout);
        NavigationView navigationView = findViewById(R.id.map_search_nav_view);
        sba_searchbar = findViewById(R.id.sba_searchbar);

        navigationView.setNavigationItemSelectedListener(MapsActivity.this);

        sba_searchbar.setOnSearchActionListener(MapsActivity.this);

        //enable or disable searchbar
        if( searchResult == null)
            sba_searchbar.enableSearch();
        else sba_searchbar.disableSearch();

        View nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        CircularImageView drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        TextView drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        TextView drawer_email = nav.findViewById(R.id.drawer_user_email);

        NavigationDrawerManager.setDrawerViews(getApplicationContext(),
                getWindowManager(),drawer_fullname,drawer_email,drawer_userPicture,
                NavigationDrawerManager.getNavigationDrawerProfile());
    }
    @Override
    public void onBackPressed() {

        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            startListSearch();
        }

    }
    /**
     * Navigation Drawer Listeners
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(id ==R.id.drawer_navigation_profile){
            Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);

        } else if (id == R.id.drawer_navigation_shareBook) {
            Intent i = new Intent(getApplicationContext(), ShareBookActivity.class);
            startActivity(i);
        } else if (id == R.id.drawer_navigation_myBook) {
            Intent my_books = new Intent(getApplicationContext(), MyBookActivity.class);
            startActivity(my_books);
        } else if (id == R.id.drawer_navigation_logout) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(task -> {
                        Intent i = new Intent(getApplicationContext(), SplashScreenActivity.class);
                        startActivity(i);
                        Toast.makeText(getApplicationContext(), getString(R.string.log_out), Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * Fire the list view mode
     */
    private void setListButton() {

        search_fab_list = findViewById(R.id.search_fab_list);

        search_fab_list.setOnClickListener((v) -> {
            startListSearch();
        });
    }


    private void setChipFilters() {

        filterDistanceChip = findViewById(R.id.distanceChip);
        filterDistanceChip.setChipText(getString(R.string.filter_distance));
        filterDistanceChip.changeBackgroundColor(getResources().getColor(R.color.white));
        OnChipClickListener chipClickListener = v -> {

            showDialog();

        };
        filterDistanceChip.setOnChipClickListener(chipClickListener);

    }


    private void showDialog() {

        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("distanceDialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        DialogFragment newFragment = DistanceFilterFragment.newInstance("Test Fragment");
        newFragment.show(ft, "distanceDialog");
    }


    /**
     * Start the SearchActivity with the classic list view
     */
    private void startListSearch(){

        Intent listSearch = new Intent(getApplicationContext(), SearchActivity.class);

        if(searchResult != null){

            Bundle bundle = new Bundle();
            bundle.putParcelableArrayList("SearchResults", searchResult);
            listSearch.putExtras(bundle);
        }

        startActivity(listSearch);
        finish();
    }


    /**
     * JSON Parser: from JSON to Book
     *
     * @param jsonObject : json representation of the book stored in algolia's "books" index
     * @return : the Book object
     */
    public Book BookJsonParser(JSONObject jsonObject) {

        String bookId = jsonObject.optString("bookId");
        String owner_uid = jsonObject.optString("owner_uid");
        String owner_username = jsonObject.optString("owner_username");
        String isbn = jsonObject.optString("isbn");
        String title = jsonObject.optString("title");
        String subtitle = jsonObject.optString("subtitle");

        //authors
        ArrayList<String> authors = new ArrayList<>();

        try {

            JSONArray jsonCategories = jsonObject.getJSONArray("authors");
            for (int i = 0; i < jsonCategories.length(); i++)
                authors.add(jsonCategories.optString(i));

        } catch (JSONException e) {
            Log.d("debug", "Error during BookJsonParse");
            e.printStackTrace();
        }

        String publisher = jsonObject.optString("publisher");
        String publishedDate = jsonObject.optString("publishedDate");
        String description = jsonObject.optString("description");
        int pageCount = jsonObject.optInt("pageCount");

        //categories
        ArrayList<String> categories = new ArrayList<>();

        try {

            JSONArray jsonCategories = jsonObject.getJSONArray("categories");
            for (int i = 0; i < jsonCategories.length(); i++)
                categories.add(jsonCategories.optString(i));

        } catch (JSONException e) {
            Log.d("debug", "Error during BookJsonParse");
            e.printStackTrace();
        }

        String language = jsonObject.optString("language");
        String thumbnail = jsonObject.optString("thumbnail");
        int numPhotos = jsonObject.optInt("numPhotos");

        String bookConditions = jsonObject.optString("bookConditions");

        //tags
        ArrayList<String> tags = new ArrayList<>();

        try {

            JSONArray jsonTags = jsonObject.getJSONArray("tags");
            for (int i = 0; i < jsonTags.length(); i++)
                tags.add(jsonTags.optString(i));

        } catch (JSONException e) {
            Log.d("debug", "Error during BookJsonParse");
            e.printStackTrace();
        }

        long creationTime = jsonObject.optLong("creationTime");
        String locationLat = jsonObject.optString("location_lat");
        String locationLong = jsonObject.optString("location_long");

        //photos
        ArrayList<String> photosName = new ArrayList<>();

        try {

            JSONArray jsonPhotos = jsonObject.getJSONArray("photosName");
            for (int i = 0; i < jsonPhotos.length(); i++)
                photosName.add(jsonPhotos.optString(i));

        } catch (JSONException e) {
            Log.d("debug", "Error during BookJsonParse");
            e.printStackTrace();
        }

        return new Book(bookId, owner_uid, owner_username, isbn, title, subtitle, authors, publisher, publishedDate, description, pageCount, categories,
                language, thumbnail, numPhotos, bookConditions, tags, creationTime, locationLat, locationLong, photosName);
    }


    /**
     * Parser for the isValid of the search that returns an ArrayList of books that matched the query
     *
     * @param hits : algolia's search hits
     * @return : the colleciton of books
     */
    public ArrayList<Book> parseResults(JSONArray hits) {

        ArrayList<Book> books = new ArrayList<>();

        for (int i = 0; i < hits.length(); i++) {

            try {

                JSONObject hit = hits.getJSONObject(i);

                Iterator<String> keyList = hit.keys();

                //first key (bookData): book object
                String bookData = keyList.next();
                Book b = BookJsonParser(hit.getJSONObject(bookData));
                //b.setBookId(bookData); //save also the FireBase unique ID, is used to retrieve the photos from firebase storage
                books.add(b);

                //second key: objectId == bookId
                //third key: _highlightResult

            } catch (JSONException e) {
                Log.d("debug", "unable to retrieve search isValid from json hits");
                e.printStackTrace();
            }
        }

        return books;
    }


    private void showSearchResults(){
        mMap.clear(); //remove previous markers
        setAnnouncementMarkers();
    }


    /**
     * Material Search Bar onSearchStateChanged
     */
    @Override
    public void onSearchStateChanged(boolean enabled) {
        String s = enabled ? "enabled" : "disabled";
        Log.d("debug", "search " + s);
    }


    /**
     * Material Search Bar onSearchConfirmed
     */
    @Override
    public void onSearchConfirmed(CharSequence searchInputText) {

        if (searchInputText != null) {
            helper.search(searchInputText.toString());
        }
    }

    /**
     * Material Search Bar onButtonClicked
     */
    @Override
    public void onButtonClicked(int buttonCode) {

        switch (buttonCode) {

            case MaterialSearchBar.BUTTON_NAVIGATION:
                drawer.openDrawer(Gravity.START); //open the drawer
                break;

            case MaterialSearchBar.BUTTON_SPEECH:
                break;

            case MaterialSearchBar.BUTTON_BACK:
                sba_searchbar.disableSearch();
                break;
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

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMapToolbarEnabled(false);

            if(searchResult != null)
                setAnnouncementMarkers();

        } else {
            Toast.makeText(getApplicationContext(), "No permissions", Toast.LENGTH_SHORT).show();
        }

    }

    public void setAnnouncementMarkers(){
        for(Book b : searchResult){
            LatLng loc = new LatLng(Double.parseDouble(b.getLocation_lat()), Double.parseDouble(b.getLocation_long()));
            Marker m = mMap.addMarker(new MarkerOptions()
                    .position(loc)
                    .title(b.getTitle()));

            m.setTag(b); //associate book object to this marker

            //Set Custom InfoWindow Adapter
            CustomInfoWindowAdapter adapter = new CustomInfoWindowAdapter(this);
            mMap.setInfoWindowAdapter(adapter);
        }

        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerClickListener(this);


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

        Intent showBook = new Intent(getApplicationContext(), ShowBookActivity.class);
        if(!searchResult.isEmpty()){
            Bundle bundle = new Bundle();
            bundle.putParcelable("book", book);
            showBook.putExtras(bundle);
        }
        startActivity(showBook);
    }


    public void filterByDistance(List<Address> place, int range){

        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(place.get(0).getLatitude(), place.get(0).getLongitude()), range);

        List<String> results = new ArrayList<>();

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

                if(!results.isEmpty()) {
                    ArrayList<Book> copySearchResult = (ArrayList<Book>) searchResult.clone();
                    for (Book b : copySearchResult) {
                        if (!results.contains(b.getBookId())) {
                            searchResult.remove(b);
                        }
                    }

                    showSearchResults();

                } else {
                    mMap.clear();
                    Toast.makeText(getApplicationContext(), getString(R.string.sa_no_results), Toast.LENGTH_SHORT).show();
                }



            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }

            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                results.add(key);
            }

        });

    }

}
