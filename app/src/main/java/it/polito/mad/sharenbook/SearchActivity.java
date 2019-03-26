package it.polito.mad.sharenbook;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.location.Address;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.algolia.instantsearch.core.helpers.Searcher;
import com.algolia.instantsearch.ui.helpers.InstantSearch;
import com.algolia.instantsearch.core.model.SearchResults;
import com.algolia.search.saas.Query;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mikhaellopez.circularimageview.CircularImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.fragments.SearchFilterFragment;
import it.polito.mad.sharenbook.fragments.SearchListFragment;
import it.polito.mad.sharenbook.fragments.SearchMapFragment;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.PermissionsHandler;

//TODO: rimuovere MapsActivity una volta ultimato SearchMapFragment

public class SearchActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MaterialSearchBar.OnSearchActionListener {

    //activity model
    public CharSequence searchInputText; //search query received in bundle
    ArrayList<Book> searchResult; //list of book that matched the query

    /**
     * DRAWER AND SEARCHBAR
     **/
    private MaterialSearchBar sba_searchbar;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private View nav;
    private TextView drawer_fullname;
    private TextView drawer_email;
    private CircularImageView drawer_userPicture;
    private String searchState;

    //search filters selected by the user
    private static String searchFilters;

    //Map, location and range
    private GoogleMap mMap;
    private static Address filterPlace;
    private static int filterRange;
    private DatabaseReference location_ref = FirebaseDatabase.getInstance().getReference("books_locations");
    private GeoFire geoFire = new GeoFire(location_ref);

    //filter button
    private Button sba_btn_filter;

    //fab to display map
    private FloatingActionButton search_fab_changeFragment;

    // Algolia instant search
    private Searcher searcher;
    private InstantSearch helper;

    //fragments
    private SearchListFragment searchListFragment;
    private SearchMapFragment searchMapFragment;

    private int currentFragment;
    private static final int NO_FRAG = -1;
    private static final int LIST_FRAG = 0;
    private static final int MAP_FRAG = 1;

    //filters saved state, containes the already selected filters
    public ArrayList<String> filterState_selectedConditions;
    public ArrayList<String> filterState_selectedCategories;
    public String filterState_tags;
    public String filterState_author;
    public int filterState_range;
    public String filterState_location;


    /*
     * OnCreate
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        //istantiate as empty collection
        this.searchResult = new ArrayList<>();

        //create fragments or restore them
        if (savedInstanceState == null) {

            this.searchListFragment = new SearchListFragment();
            this.searchMapFragment = new SearchMapFragment();

        } else {

            if (getSupportFragmentManager().findFragmentByTag("searchList") != null)
                this.searchListFragment = (SearchListFragment) getSupportFragmentManager().findFragmentByTag("searchList");
            else this.searchListFragment = new SearchListFragment();

            if (getSupportFragmentManager().findFragmentByTag("searchMap") != null)
                this.searchMapFragment = (SearchMapFragment) getSupportFragmentManager().findFragmentByTag("searchMap");
            else this.searchMapFragment = new SearchMapFragment();
        }

        //setup map floating action button
        setChangeFragmentButton();

        // setup Drawer and Search Bar
        setDrawerAndSearchBar();

        // Check for permissions
        PermissionsHandler.check(this);

        //Algolia's InstantSearch setup
        searcher = Searcher.create("K7HV32WVKQ", "04a25396f978e2d22348e5520d70437e", "books");
        searcher.registerResultListener(this::onSearchResultReceived);
        searcher.registerErrorListener((query, error) -> {
            Toast.makeText(getApplicationContext(), R.string.sa_no_results, Toast.LENGTH_LONG).show();
            Log.d("error", "Unable to retrieve search isValid from Algolia");
        });
        helper = new InstantSearch(searcher);


        //start alwayis without selected filters
        this.filterState_selectedConditions = null;
        this.filterState_selectedCategories = null;
        this.filterState_tags = null;
        this.filterState_author = null;
        this.filterState_range = -1;
        this.filterState_location = null;


        //retrieve data form intent or from saved state
        if (savedInstanceState == null)
            startedFromIntent();
        else
            startedFromSavedState(savedInstanceState); // rotation ecc...
    }


    /**
     * Saves the state of the activity when dealing with system wide events (e.g. rotation)
     *
     * @param outState : the bundle object that contains all the serialized information to be saved
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("searchResult", this.searchResult);
        outState.putCharSequence("searchInputText", this.searchInputText);
        outState.putString("searchState", this.searchState);
        outState.putInt("currentFragment", this.currentFragment);
        outState.putString("filterButtonText", this.sba_btn_filter.getText().toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationDrawerManager.setDrawerViews(getApplicationContext(),
                getWindowManager(), drawer_fullname, drawer_email, drawer_userPicture,
                NavigationDrawerManager.getNavigationDrawerProfile());
        navigationView.setCheckedItem(R.id.drawer_navigation_search);
    }

    /**
     * The SearchActivity has been started with an intent from another activity
     * <p>
     * the searchInputText is retrieved from the bundle
     */
    private void startedFromIntent() {

        Bundle bundle = getIntent().getExtras(); //retrieve data from the intent

        if (bundle != null) {

            this.searchInputText = bundle.getCharSequence("searchInputText"); //retrieve text from intent

            //if there is a text to search, search it, if not show the keyboard
            if (searchInputText != null) {

                this.sba_searchbar.setText(searchInputText.toString()); //set the searched text in the searchbar

                //remove previous filters if present
                clearFiltersState();
                onSearchConfirmed(this.searchInputText.toString()); // Fire the search (async)

            } else showKeyboard();

            //no fragment already displayes
            this.currentFragment = NO_FRAG;

            //add the search list fragment to the view
            addSearchListFragment();

        }
    }

    /**
     * The search activity has been started from a saved state, retrieve all data from Bundle
     *
     * @param savedInstanceState : a Bundle which contains the state of the application
     */
    private void startedFromSavedState(Bundle savedInstanceState) {

        this.searchInputText = savedInstanceState.getCharSequence("searchInputText"); //retrieve text info from saveInstanceState
        if (this.searchInputText != null)
            this.sba_searchbar.setText(this.searchInputText.toString()); //set the searched text in the searchbar

        ArrayList<Book> previousSearchResults = savedInstanceState.getParcelableArrayList("searchResult"); //from onSaveInstanceState
        this.searchResult.clear();
        this.searchResult.addAll(previousSearchResults);

        this.currentFragment = savedInstanceState.getInt("currentFragment");

        //update fab icon
        if (this.currentFragment == LIST_FRAG)
            search_fab_changeFragment.setImageResource(R.drawable.ic_location_black_12dp);
        else if (this.currentFragment == MAP_FRAG)
            search_fab_changeFragment.setImageResource(R.drawable.ic_view_list_black_24dp);

        //set previously setted filter number
        this.sba_btn_filter.setText(savedInstanceState.getString("filterButtonText"));
    }


    /**
     * Search in Algolia for the books that matched the searchInputText and the Filters
     *
     * @param searchInputText : searhcInputtext is null if the callback is called explicitly by FilterFragment, in all other cases it's equal to the input text of the user
     */
    @Override
    public void onSearchConfirmed(CharSequence searchInputText) {

        if (searchInputText != null) {
            this.searchInputText = searchInputText;
            this.sba_searchbar.setText(searchInputText.toString());
        }

        //first search in algolia using the searchFilters
        Query query = new Query();
        query.setQuery(searchInputText == null ? "" : searchInputText.toString());

        //set filters for category, authors, conditions: the Place&Distance filters will be applied when Algolia will send his response
        if (searchFilters != null)
            if (searchFilters.length() > 0)
                query.setFilters(searchFilters);

        searcher.setQuery(query);
        helper.search();
    }


    /**
     * Search result received from Algolia
     *
     * @param results       : the object that contains the results of the search
     * @param isLoadingMore :
     */
    private void onSearchResultReceived(SearchResults results, Boolean isLoadingMore) {

        if (results.nbHits > 0) {

            /*
             * Retrieve result list and filter it by distance if necessary
             */
            this.searchResult.clear();
            this.searchResult.addAll(parseResults(results.hits)); //parse all algolia results and add them into collection

            if (filterRange > 0)
                if (filterPlace != null)
                    filterByDistance(); //remove from searchResult books that are too far from the selected filter location

            /*
             * display the fragment with the search results and hide keyboard
             */
            if (this.searchResult.isEmpty())
                Toast.makeText(getApplicationContext(), getString(R.string.sa_no_results), Toast.LENGTH_SHORT).show();
            else hidekeyboard();

            // notify fragments that a new result is available
            this.searchListFragment.updateDisplayedSearchResult();
            this.searchMapFragment.updateDisplayedSearchResult();

        } else {

            clearCurrentSearchResult();
            Toast.makeText(getApplicationContext(), R.string.sa_no_results, Toast.LENGTH_LONG).show();
        }

    }


    /**
     * Material Search Bar onSearchStateChanged
     */
    @Override
    public void onSearchStateChanged(boolean enabled) {
        searchState = enabled ? "enabled" : "disabled";
        Log.d("debug", "search " + searchState);
    }

    /**
     * hide keyboard programmatically from the search bar
     */
    public void hidekeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(this.sba_searchbar.getWindowToken(), 0);
        }
    }


    /**
     * show keyboard programmatically from the search bar
     */
    public void showKeyboard() {

        if (this.sba_searchbar.requestFocus()) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null)
                imm.showSoftInput(this.sba_searchbar, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    /**
     * Load the search LIST fragment into the activity view
     */
    private void addSearchListFragment() {

        FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        //set fab button to place
        search_fab_changeFragment.setImageResource(R.drawable.ic_place_black_24dp);

        //add the list fragment
        if (this.currentFragment == NO_FRAG)
            fragmentTransaction.add(R.id.search_fragment_container, this.searchListFragment, "searchList")
                    .commit();

            //or replace the map fragmnet with the list one
        else if (this.currentFragment == MAP_FRAG)
            fragmentTransaction.replace(R.id.search_fragment_container, this.searchListFragment, "searchList")
                    .commit();

        this.currentFragment = LIST_FRAG;
    }


    /**
     * Load the search MAP fragment into the activity view
     */
    private void addSearchMapFragment() {

        FragmentTransaction fragmentTransaction = getSupportFragmentManager()
                .beginTransaction()
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);

        //set fab button to list
        search_fab_changeFragment.setImageResource(R.drawable.ic_view_list_black_24dp);

        //add the SearchMapFragment fragment
        if (this.currentFragment == NO_FRAG)
            fragmentTransaction.add(R.id.search_fragment_container, this.searchMapFragment, "searchMap")
                    .commit();

            //or replace the list fragment with the map one
        else if (this.currentFragment == LIST_FRAG)
            fragmentTransaction.replace(R.id.search_fragment_container, this.searchMapFragment, "searchMap")
                    .commit();

        this.currentFragment = MAP_FRAG;
    }

    /**
     * Callback for the changeFragment button, toggle fragments between list and map
     */
    private void loadListOrMapFragment(View view) {

        if (this.currentFragment == NO_FRAG) //default is search list
            addSearchListFragment();
        if (this.currentFragment == LIST_FRAG)
            addSearchMapFragment();
        else if (this.currentFragment == MAP_FRAG)
            addSearchListFragment();
    }


    /**
     * Fire the map fragment
     */
    private void setChangeFragmentButton() {

        search_fab_changeFragment = findViewById(R.id.search_fab_changeFragment);
        search_fab_changeFragment.setVisibility(View.VISIBLE);

        if (this.currentFragment == MAP_FRAG)
            search_fab_changeFragment.setImageResource(R.drawable.ic_view_list_black_24dp);
        else if (this.currentFragment == LIST_FRAG)
            search_fab_changeFragment.setImageResource(R.drawable.ic_place_black_24dp);

        search_fab_changeFragment.setOnClickListener(this::loadListOrMapFragment);
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
        ArrayList<Integer> categories = new ArrayList<>();

        try {

            JSONArray jsonCategories = jsonObject.getJSONArray("categories");
            for (int i = 0; i < jsonCategories.length(); i++)
                categories.add(jsonCategories.optInt(i));

        } catch (JSONException e) {
            Log.d("debug", "Error during BookJsonParse");
            e.printStackTrace();
        }

        String language = jsonObject.optString("language");
        String thumbnail = jsonObject.optString("thumbnail");
        int numPhotos = jsonObject.optInt("numPhotos");

        int bookConditions = jsonObject.optInt("bookConditions");

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
        Double locationLat = jsonObject.optDouble("location_lat");
        Double locationLong = jsonObject.optDouble("location_long");

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

        boolean shared = jsonObject.optBoolean("shared", false);

        return new Book(bookId, owner_uid, owner_username, isbn, title, subtitle, authors, publisher, publishedDate, description, pageCount, categories,
                language, thumbnail, numPhotos, bookConditions, tags, creationTime, locationLat, locationLong, photosName, shared);
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
                Book b = BookJsonParser(hit.getJSONObject("bookData"));
                b.setShared(hit.optBoolean("shared", false));
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


    /**
     * Release resources when onDestroy event is catched
     */
    @Override
    protected void onDestroy() {

        searcher.destroy();
        super.onDestroy();
    }


    /**
     * onBackPressed method
     */
    @Override
    public void onBackPressed() {

        DrawerLayout drawer = findViewById(R.id.search_drawer_layout);

        if (drawer.isDrawerOpen(GravityCompat.START)) {

            drawer.closeDrawer(GravityCompat.START);

        } else {

            //The user wants to close the activity and go back to the previous activity in stack
            super.onBackPressed();

            //remove all fragments from the fragment back stack
            this.freeFragmentBackStack();

            // destroy the activity
            finish();
        }
    }


    /**
     * setDrawerAndSearchBar
     */
    private void setDrawerAndSearchBar() {

        /* DRAWER AND SEARCHBAR */
        drawer = findViewById(R.id.search_drawer_layout);
        navigationView = findViewById(R.id.search_nav_view);
        sba_searchbar = findViewById(R.id.sba_searchbar);

        navigationView.setNavigationItemSelectedListener(SearchActivity.this);

        sba_searchbar.setOnSearchActionListener(SearchActivity.this);
        sba_searchbar.enableSearch();

        nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        drawer_email = nav.findViewById(R.id.drawer_user_email);


        //set also the filter button
        sba_btn_filter = findViewById(R.id.sba_btn_filter);

        sba_btn_filter.setOnClickListener(v -> {

            //if fragment already created, destroy it
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            Fragment prev = getSupportFragmentManager().findFragmentByTag("filterDialog");
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            // Create and show the new dialog.

            DialogFragment filterFragment = SearchFilterFragment.newInstance("Filter Fragment");

            filterFragment.show(ft, "filterDialog");
        });

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
     * Navigation Drawer Listeners
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        //close activity
        this.freeFragmentBackStack();
        finish();

        //jump to the selected one from the drawer
        return NavigationDrawerManager.onNavigationItemSelected(this, null, item, getApplicationContext(), drawer, R.id.drawer_navigation_search);
    }


    /**
     * Remove all the activity fragments from the fragments back stack
     */
    private void freeFragmentBackStack() {

        List<Fragment> searchFraments = getSupportFragmentManager().getFragments();

        for (Fragment fragment : searchFraments) {
            if (fragment != null)
                getSupportFragmentManager().beginTransaction().remove(fragment).commit();
        }
    }


    /* ***********************************************/
    /*                    FILTERS                    */
    /* ***********************************************/

    /**
     * filter the results received from Algolia using the location and range inserted by the user
     */
    public void filterByDistance() {

        List<String> geofireResults = new ArrayList<>();

        //query the DB
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(filterPlace.getLatitude(), filterPlace.getLongitude()), filterRange);

        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {

            @Override
            public void onGeoQueryReady() {

                if (!geofireResults.isEmpty()) {

                    //remove all the books that are not near the place
                    ArrayList<Book> copySearchResult = (ArrayList<Book>) searchResult.clone();
                    for (Book b : copySearchResult)
                        if (!geofireResults.contains(b.getBookId()))
                            searchResult.remove(b);

                } else searchResult.clear(); //no books near the place

                //update adapters of fragment's recyclerview
                searchListFragment.updateDisplayedSearchResult();
                searchMapFragment.updateDisplayedSearchResult();

                //TODO: find a better solution to display the circle also if we are on SearchListFragment
                //if the map is currently displayed, center into the circle
                if (getSupportFragmentManager().findFragmentById(R.id.search_fragment_container) instanceof SearchMapFragment) {
                    if (searchResult.isEmpty())
                        searchMapFragment.showFilterDistanceCircle(filterPlace, filterRange, Color.RED);
                    else
                        searchMapFragment.showFilterDistanceCircle(filterPlace, filterRange, Color.GREEN);
                }
            }

            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                geofireResults.add(key);
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });
    }

    /* **********************************************************************************
     * The Methods below are called from SearchFilterFragment to set the search filters
     */

    /**
     * Clear the currently showed search result
     */
    public void clearCurrentSearchResult() {
        this.searchResult.clear();
        this.searchListFragment.updateDisplayedSearchResult();
        this.searchMapFragment.updateDisplayedSearchResult();
    }

    /**
     * Set the Category, Conditions, Authors filters string
     */
    public void setSearchFilters(String userFilter) {
        searchFilters = userFilter;
    }

    /**
     * Set the place that will be used to filter by distance
     *
     * @param place :
     */
    public void setFilterPlace(Address place) {
        filterPlace = place;
    }

    /**
     * set the range that will be used to filter by distance
     *
     * @param range :
     */
    public void setFilterRange(int range) {
        filterRange = range;
    }


    /**
     * Returns the searchResult, used by fragments to get the collection to display on their recyclerviews
     *
     * @return :
     */

    public ArrayList<Book> getSearchResult() {
        return this.searchResult;
    }


    /**
     * Set the filter state, this method is fired when the user confirms his filters
     */
    public void setFiltersState(ArrayList<String> selectedConditions, ArrayList<String> selectedCategories, String tags, String author, int range, String location) {

        this.filterState_selectedConditions = selectedConditions;
        this.filterState_selectedCategories = selectedCategories;
        this.filterState_tags = tags;
        this.filterState_author = author;
        this.filterState_range = range;
        this.filterState_location = location;
    }

    public boolean filtersStatesArePresent() {

        return this.filterState_location != null ||
                this.filterState_range >= 0 ||
                this.filterState_selectedConditions != null ||
                this.filterState_selectedCategories != null ||
                this.filterState_author != null ||
                this.filterState_tags != null;
    }

    /**
     * Clear filter state, fired by the Clear button in the dialog fragment
     */
    public void clearFiltersState() {

        this.setSearchFilters("");
        this.filterState_selectedConditions = null;
        this.filterState_selectedCategories = null;
        this.filterState_tags = null;
        this.filterState_author = null;
        this.filterState_range = -1;
        this.filterState_location = null;

        filterRange = -1;
        filterPlace = null;
    }


    /**
     * Shows a (#filters) to right of the sba_filter button
     *
     * @param filtersCounter : the number of setted filters
     */
    @SuppressLint("SetTextI18n")
    public void showFilterCounterInFilterButton(int filtersCounter) {
        if (filtersCounter > 0)
            this.sba_btn_filter.setText(getResources().getString(R.string.sba_btn_filters) + " (" + filtersCounter + ")");
    }

    /**
     * Remove the (#filters) to the right of sba_filter button
     */
    public void clearFilterCounterInFilterButton() {
        this.sba_btn_filter.setText(R.string.sba_btn_filters);
    }

}