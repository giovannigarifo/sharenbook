package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mikhaellopez.circularimageview.CircularImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.model.UserProfile;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;


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

        //retrieve book info
        if (savedInstanceState == null) {

            Bundle bundle = getIntent().getExtras();

            if (bundle == null)
                Log.d("debug", "[SearchActivity] no search input text received from calling Activity");
            else {
                searchInputText = bundle.getCharSequence("searchInputText"); //retrieve text from intent
               // user = bundle.getParcelable(getString(R.string.user_profile_data_key)); //retrieve user info
            }

        } else {
            searchInputText = savedInstanceState.getCharSequence("searchInputText"); //retrieve text info from saveInstanceState
           // user = savedInstanceState.getParcelable(getString(R.string.user_profile_data_key)); //retrieve user info
        }


        // setup Drawer and Search Bar
        setDrawerAndSearchBar();

        //setup map floating action button
        setMapButton();

        //setup bottom navigation bar
        setBottomNavigationBar();

        // RecylerView setup
        searchResult = new ArrayList<>();

        RecyclerView search_rv_result = (RecyclerView) findViewById(R.id.search_rv_result);
        LinearLayoutManager llm = new LinearLayoutManager(SearchActivity.this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        search_rv_result.setLayoutManager(llm);

        sbAdapter = new SearchBookAdapter(this.searchResult, getApplicationContext());
        search_rv_result.setAdapter(sbAdapter);

        //Algolia's InstantSearch setup
        searcher = Searcher.create("4DWHVL57AK", "03391b3ea81e4a5c37651a677670bcb8", "books");
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
            Log.d("error", "Unable to retrieve search result from Algolia");
        });

        // Fire the search (async) if user launched from searchbar in another activity
        if (searchInputText != null) {
            helper.search(searchInputText.toString());
        }

    }

    /**
     *
     */
    private void setBottomNavigationBar() {

        search_bottom_nav_bar = findViewById(R.id.search_bottom_nav_bar);

        //set navigation_profile as selected item
        search_bottom_nav_bar.setSelectedItemId(R.id.navigation_search);

        //set the listener for the navigation bar items
        search_bottom_nav_bar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_profile:
                    Intent i_show_profile = new Intent(getApplicationContext(), ShowProfileActivity.class);
                    startActivity(i_show_profile);
                    break;

                case R.id.navigation_search:
                    sba_searchbar.enableSearch();
                    break;

                case R.id.navigation_myBook:
                    Intent my_books = new Intent(getApplicationContext(), MyBookActivity.class);
                    startActivity(my_books);
                    break;
            }
            return true;
        });
    }

    /**
     * Fire the map fragment
     */
    private void setMapButton() {

        search_fab_map = findViewById(R.id.search_fab_map);

        search_fab_map.setOnClickListener((v) -> {
            Intent mapSearch = new Intent(getApplicationContext(), MapsActivity.class);
            startActivity(mapSearch);
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
        String isbn = jsonObject.optString("isbn");
        String title = jsonObject.optString("title");
        String subtitle = jsonObject.optString("subtitle");

        //authors
        ArrayList<String> authors = new ArrayList<>();

        try {

            Object a = jsonObject.get("authors");

            if (a instanceof String) {

                String author = (String) a;
                author = author.replace("[", "");
                author = author.replace("]", "");
                authors.add(author);

            } else {

                JSONArray jsonCategories = jsonObject.getJSONArray("authors");
                for (int i = 0; i < jsonCategories.length(); i++)
                    authors.add(jsonCategories.optString(i));
            }

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

            Object c = jsonObject.get("categories");

            if (c instanceof String) {

                String category = (String) c;
                category = category.replace("[", "");
                category = category.replace("]", "");
                categories.add(category);

            } else {

                JSONArray jsonCategories = jsonObject.getJSONArray("categories");
                for (int i = 0; i < jsonCategories.length(); i++)
                    categories.add(jsonCategories.optString(i));
            }

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

            Object t = jsonObject.get("tags");

            if (t instanceof String) {

                String tag = (String) t;
                tag = tag.replace("[", "");
                tag = tag.replace("]", "");
                tags.add(tag);

            } else {

                JSONArray jsonTags = jsonObject.getJSONArray("tags");
                for (int i = 0; i < jsonTags.length(); i++)
                    tags.add(jsonTags.optString(i));
            }

        } catch (JSONException e) {
            Log.d("debug", "Error during BookJsonParse");
            e.printStackTrace();
        }

        long creationTime = jsonObject.optLong("creationTime");
        String location = jsonObject.optString("location");

        return new Book(bookId, owner_uid, isbn, title, subtitle, authors, publisher, publishedDate, description,
                pageCount, categories, language, thumbnail, numPhotos, bookConditions, tags, creationTime, location);
    }


    /**
     * Parser for the result of the search that returns an ArrayList of books that matched the query
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
                Log.d("debug", "unable to retrieve search result from json hits");
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

        outState.putCharSequence("searchInputText", searchInputText);
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
            super.onBackPressed();
        }
        navigationView.setCheckedItem(R.id.drawer_navigation_profile);
    }


    /**
     * setDrawerAndSearchBar
     */
    private void setDrawerAndSearchBar() {

        /** DRAWER AND SEARCHBAR **/

        drawer = findViewById(R.id.search_drawer_layout);
        navigationView = findViewById(R.id.search_nav_view);
        sba_searchbar = findViewById(R.id.sba_searchbar);

        navigationView.setCheckedItem(R.id.drawer_navigation_myBook);
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
                sbAdapter.notifyDataSetChanged();
                break;
        }

    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }
}


/**
 * SearchBookAdapter class
 */

class SearchBookAdapter extends RecyclerView.Adapter<SearchBookAdapter.SearchBookViewHolder> {

    private List<Book> searchResult;
    private Context context;

    //constructor
    SearchBookAdapter(ArrayList<Book> searchResult, Context context) {
        this.searchResult = searchResult;
        this.context = context;
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
        LinearLayout ll = (LinearLayout) li.inflate(R.layout.item_search_result, parent, false);

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

        int thumbnailOrFirstPhotoPosition = searchResult.get(position).getNumPhotos() - 1;
        String bookId = searchResult.get(position).getBookId();

        StorageReference thumbnailOrFirstPhotoRef = FirebaseStorage.getInstance().getReference().child("book_images/" + bookId + "/" + thumbnailOrFirstPhotoPosition + ".jpg");

        thumbnailOrFirstPhotoRef.getDownloadUrl().addOnSuccessListener((Uri uri) -> {

            String imageURL = uri.toString();

            GlideApp.with(context).load(imageURL)
                    .placeholder(R.drawable.book_photo_placeholder)
                    .error(R.drawable.book_photo_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade(500))
                    .into(photo);

        }).addOnFailureListener((exception) -> {
                    // handle errors here
                    Log.e("error", "Error while retrieving book photo/thumbnail from firebase storage.");
                }
        );

        //title
        TextView title = holder.item_search_result.findViewById(R.id.item_searchresult_title);
        title.setText(this.searchResult.get(position).getTitle());

        //author
        TextView authors = holder.item_search_result.findViewById(R.id.item_searchresult_author);
        authors.setText(this.searchResult.get(position).getAuthorsAsString());

        //creationTime
        TextView creationTime = holder.item_search_result.findViewById(R.id.item_searchresult_creationTime);
        creationTime.setText(this.searchResult.get(position).getCreationTimeAsString() + " - Placeholder (TO)" );

        //fab & card listeners
        FloatingActionButton fab = holder.item_search_result.findViewById(R.id.item_searchresult_fab);
        CardView card = holder.item_search_result.findViewById(R.id.item_searchresult_cv);

        fab.setOnClickListener((v -> {

            Toast.makeText(context, "fab listener placeholder", Toast.LENGTH_SHORT).show();
        }));

        card.setOnClickListener((v -> {
            Toast.makeText(context, "fab listener placeholder", Toast.LENGTH_SHORT).show();

        }));

        //show card
        holder.item_search_result.setVisibility(View.VISIBLE);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.searchResult.size();
    }

}

