package it.polito.mad.sharenbook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.algolia.instantsearch.helpers.InstantSearch;
import com.algolia.instantsearch.helpers.Searcher;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.firebase.ui.auth.AuthUI;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.robertlevonyan.views.chip.Chip;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.model.UserProfile;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.PermissionsHandler;
import it.polito.mad.sharenbook.utils.UserInterface;


public class SearchActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MaterialSearchBar.OnSearchActionListener {


    CharSequence searchInputText; //search query received in bundle
    ArrayList<Book> searchResult; //list of book that matched the query

    //user
    private UserProfile user;


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

    //fab to display map
    FloatingActionButton search_fab_map;

    //bottom navigation bar
    BottomNavigationView search_bottom_nav_bar;

    // Recyler View
    SearchBookAdapter sbAdapter;

    // Algolia instant search
    Searcher searcher;
    InstantSearch helper;

    /*
     * OnCreate
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        //setup bottom navigation bar
        UserInterface.setupNavigationBar(this, 0);

        //setup map floating action button
        setMapButton();

        // setup Drawer and Search Bar
        setDrawerAndSearchBar();

        // RecylerView setup
        searchResult = new ArrayList<>();

        // Check for permissions
        PermissionsHandler.check(this);

        RecyclerView search_rv_result = (RecyclerView) findViewById(R.id.search_rv_result);

        StaggeredGridLayoutManager sglm;
        LinearLayoutManager llm;

        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){

            sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
            search_rv_result.setLayoutManager(sglm);

        } else if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){

            llm = new LinearLayoutManager(SearchActivity.this);
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            search_rv_result.setLayoutManager(llm);
        }

        sbAdapter = new SearchBookAdapter(this.searchResult, this);
        search_rv_result.setAdapter(sbAdapter);

        //Algolia's InstantSearch setup
        // searcher = Searcher.create("4DWHVL57AK", "03391b3ea81e4a5c37651a677670bcb8", "books");
        searcher = Searcher.create("K7HV32WVKQ", "04a25396f978e2d22348e5520d70437e", "books");
        helper = new InstantSearch(searcher);

        searcher.registerResultListener((results, isLoadingMore) -> {

            if (results.nbHits > 0) {

                searchResult.clear();
                searchResult.addAll(parseResults(results.hits));
                sbAdapter.notifyDataSetChanged();
                sba_searchbar.disableSearch();

            } else {
                Toast.makeText(getApplicationContext(), R.string.sa_no_results, Toast.LENGTH_LONG).show();
            }
        });

        searcher.registerErrorListener((query, error) -> {

            Toast.makeText(getApplicationContext(), R.string.sa_no_results, Toast.LENGTH_LONG).show();
            Log.d("error", "Unable to retrieve search isValid from Algolia");
        });


        //retrieve data form intent or from saved state
        if (savedInstanceState == null) {
            startedFromIntent();
        } else {
            startedFromSavedState(savedInstanceState);
        }
    }

    private void searchStatusCheck(Bundle data){
        if(data.getString("searchState")!=null) {
            searchState = data.getString("searchState");
            if (searchState.equals("enabled")) {
                search_bottom_nav_bar.setVisibility(View.GONE);
                search_fab_map.setVisibility(View.GONE);
            } else {
                search_bottom_nav_bar.setVisibility(View.VISIBLE);
                search_fab_map.setVisibility(View.VISIBLE);
            }
        }
    }

    private void startedFromIntent(){

        Bundle bundle = getIntent().getExtras();

        if (bundle == null)
            Log.d("debug", "[SearchActivity] no search input text received from calling Activity");
        else {
            searchInputText = bundle.getCharSequence("searchInputText"); //retrieve text from intent

            ArrayList<Book> previousSearchResults = bundle.getParcelableArrayList("SearchResults");

            if(previousSearchResults != null){
                searchResult.clear();
                searchResult.addAll(previousSearchResults);
                sbAdapter.notifyDataSetChanged();
            }

            searchStatusCheck(bundle);
        }

        // Fire the search (async) if user launched from searchbar in another activity
        if (searchInputText != null) {
            helper.search(searchInputText.toString());
        }
    }

    private void startedFromSavedState(Bundle savedInstanceState){

        searchInputText = savedInstanceState.getCharSequence("searchInputText"); //retrieve text info from saveInstanceState
        // user = savedInstanceState.getParcelable(getString(R.string.user_profile_data_key)); //retrieve user info

        ArrayList<Book> previousSearchResults = savedInstanceState.getParcelableArrayList("searchResult"); //from onSaveInstanceState

        searchResult.clear();
        searchResult.addAll(previousSearchResults);
        sbAdapter.notifyDataSetChanged();

        searchStatusCheck(savedInstanceState);
    }



    /**
     * Fire the map fragment
     */
    private void setMapButton() {

        search_fab_map = findViewById(R.id.search_fab_map);

        search_fab_map.setVisibility(View.VISIBLE);

        search_fab_map.setOnClickListener((v) -> {
            Intent mapSearch = new Intent(getApplicationContext(), MapsActivity.class);
            if(!searchResult.isEmpty()){
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("SearchResults", searchResult);
                mapSearch.putExtras(bundle);
            }
            startActivity(mapSearch);
            finish();
        });
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


    /**
     * Saves the state of the activity when dealing with system wide events (e.g. rotation)
     *
     * @param outState : the bundle object that contains all the serialized information to be saved
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList("searchResult", searchResult);
        outState.putCharSequence("searchInputText", searchInputText);
        outState.putString("searchState",searchState);
       // outState.putParcelable(getString(R.string.user_profile_data_key), user);
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
     * Terminate activity if actionbar left arrow pressed
     */
    @Override
    public boolean onSupportNavigateUp() {

        finish();
        return true;
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
            Intent show_case = new Intent(getApplicationContext(), ShowCaseActivity.class);
            startActivity(show_case);
            finish();
        }
    }


    /**
     * setDrawerAndSearchBar
     */
    private void setDrawerAndSearchBar() {

        // Get bottom navbar
        search_bottom_nav_bar = findViewById(R.id.navigation);

        /** DRAWER AND SEARCHBAR **/
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

        NavigationDrawerManager.setDrawerViews(getApplicationContext(),
                getWindowManager(),drawer_fullname,drawer_email,drawer_userPicture,
                NavigationDrawerManager.getNavigationDrawerProfile());
    }

    /**
     * Material Search Bar onSearchStateChanged
     */
    @Override
    public void onSearchStateChanged(boolean enabled) {
        searchState = enabled ? "enabled" : "disabled";
        Log.d("debug", "search " + searchState);

        if (sba_searchbar.isSearchEnabled()){search_bottom_nav_bar.setVisibility(View.GONE);
            search_fab_map.setVisibility(View.GONE);}

        else {
            search_bottom_nav_bar.setVisibility(View.VISIBLE);
            search_fab_map.setVisibility(View.VISIBLE);
        }

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
                sbAdapter.notifyDataSetChanged();
                break;
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

        DrawerLayout drawer = findViewById(R.id.search_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}


/**
 * SearchBookAdapter class
 */

class SearchBookAdapter extends RecyclerView.Adapter<SearchBookAdapter.SearchBookViewHolder> {

    private List<Book> searchResult;
    private Context context;
    private Activity activity;

    //constructor
    SearchBookAdapter(ArrayList<Book> searchResult, Activity activity) {
        this.searchResult = searchResult;
        this.context = activity.getApplicationContext();
        this.activity = activity;
    }

    //Inner Class that provides a reference to the views for each data item of the collection
    class SearchBookViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout item_search_result;

        SearchBookViewHolder(LinearLayout ll) {
            super(ll);
            this.item_search_result = ll;
        }
    }

    /**
     * Create new ViewHolder objects (invoked by the layout manager) and set the view to use to
     * display it's content
     *
     * @param parent   :
     * @param viewType :
     * @return BookPhotoViewHolder :
     */
    @NonNull
    @Override
    public SearchBookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater li = LayoutInflater.from(parent.getContext());
        LinearLayout ll = null;

        if(parent.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){

            ll = (LinearLayout) li.inflate(R.layout.item_search_result_land, parent, false);

        } else if(parent.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){

            ll = (LinearLayout) li.inflate(R.layout.item_search_result, parent, false);
        }

        return new SearchBookViewHolder(ll);
    }

    /**
     * Replace the contents of a ViewHolder (invoked by the layout manager)
     *
     * @param holder   :
     * @param position :
     */
    @Override
    public void onBindViewHolder(@NonNull SearchBookViewHolder holder, int position) {

        //hide card
        holder.item_search_result.setVisibility(View.INVISIBLE);

        //photo
        ImageView photo = holder.item_search_result.findViewById(R.id.item_searchresult_photo);
        photo.setImageResource(R.drawable.book_photo_placeholder);

        Book book = searchResult.get(position);
        String fileName = (book.getPhotosName().size() > 1) ? book.getPhotosName().get(1) : book.getPhotosName().get(0);
        StorageReference thumbnailOrFirstPhotoRef = FirebaseStorage.getInstance().getReference().child("book_images/" + book.getBookId() + "/" + fileName);

        GlideApp.with(context).load(thumbnailOrFirstPhotoRef)
                .placeholder(R.drawable.book_photo_placeholder)
                .error(R.drawable.book_photo_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .into(photo);

        //title
        TextView title = holder.item_search_result.findViewById(R.id.item_searchresult_title);
        title.setText(this.searchResult.get(position).getTitle());

        //author
        TextView authors = holder.item_search_result.findViewById(R.id.item_searchresult_author);
        authors.setText(this.searchResult.get(position).getAuthorsAsString());

        //creationTime
        TextView creationTime = holder.item_search_result.findViewById(R.id.item_searchresult_creationTime);
        creationTime.setText(this.searchResult.get(position).getCreationTimeAsString(context));

        //location
        TextView location = holder.item_search_result.findViewById(R.id.item_searchresult_location);
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> place = new ArrayList<>();
        try {
            place.addAll(geocoder.getFromLocation(Double.parseDouble(searchResult.get(position).getLocation_lat()), Double.parseDouble(searchResult.get(position).getLocation_long()), 1));
        }catch (IOException e) {
            e.printStackTrace();
        }

        if(!place.isEmpty())
            location.setText(place.get(0).getLocality() + ", " + place.get(0).getCountryName());
        else
            location.setText(R.string.unknown_place);


        //card listeners
        CardView card = holder.item_search_result.findViewById(R.id.item_searchresult_cv);

        card.setOnClickListener((v -> {
            Intent i = new Intent(context, ShowBookActivity.class);
            i.putExtra("book",searchResult.get(position));
            activity.startActivity(i); // start activity without finishing in order to return back with back pressed

        }));

        //Chip set
        Chip chip = (Chip) holder.item_search_result.findViewById(R.id.chip);
        chip.setChipText(searchResult.get(position).getOwner_username());
        //ImageView im_icon = chip.
        //UserInterface.showGlideImage(this.context, FirebaseStorage.getInstance().getReference().child("/images").child("/"+searchResult.get(position).getOwner_uid()+".jpg"), im_icon, 0);


        //show card
        holder.item_search_result.setVisibility(View.VISIBLE);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.searchResult.size();
    }

}

