package it.polito.mad.sharenbook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import it.polito.mad.sharenbook.adapters.ShowBooksAdapter;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.PermissionsHandler;
import it.polito.mad.sharenbook.utils.UserInterface;

public class ShowCaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, MaterialSearchBar.OnSearchActionListener {

    private MaterialSearchBar searchBar;
    private BottomNavigationView navBar;

    private DatabaseReference booksDb;
    private DatabaseReference favoritesDb;
    private DatabaseReference borrowRequestsDb;

    private ValueEventListener requestedBooksListener;
    private ValueEventListener favoriteBooksListener;
    private ChildEventListener lastBooksListener;

    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    private GeoFire geoFire;

    private RecyclerView lastBooksRV;
    private RecyclerView favoriteBooksRV;
    private RecyclerView closeBooksRV;

    private String user_id, username;
    private LinkedHashSet<String> favoritesBookIdList;
    private HashSet<String> requestedBookIdList;

    private NavigationView navigationView;
    private DrawerLayout drawer;
    private CircularImageView drawer_userPicture;
    private TextView drawer_fullname;
    private TextView drawer_email;

    private ShowBooksAdapter lastBooksAdapter, favoriteBooksAdapter, closeBooksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_case);

        // Get username and user_id
        SharedPreferences userData = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(getString(R.string.username_copy_key), "");
        user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Setup navigation tools
        setupNavigationTools();

        // Get Firebase reference
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        booksDb = db.getReference(getString(R.string.books_key));
        booksDb.keepSynced(true);
        favoritesDb = db.getReference(getString(R.string.users_key)).child(user_id).child(getString(R.string.user_favorites_key));
        favoritesDb.keepSynced(true);
        borrowRequestsDb = db.getReference(getString(R.string.usernames_key)).child(username).child(getString(R.string.borrow_requests_key));

        //Setup location client
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get Geofire
        DatabaseReference location_ref = FirebaseDatabase.getInstance().getReference("books_locations");
        geoFire = new GeoFire(location_ref);

        // Prepare for borrowRequests sent list creation
        createBorrowRequestsListener();

        // Prepare recycler views
        setupRecyclerViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        navBar.setSelectedItemId(R.id.navigation_showcase);
        if (searchBar.isSearchEnabled())
            searchBar.disableSearch();

        // Add listeners
        favoritesDb.orderByValue().limitToLast(20).addValueEventListener(favoriteBooksListener);
        borrowRequestsDb.addValueEventListener(requestedBooksListener);

        // Load/Reload recycler views
        checkLocationThenLoadCloseBooks();
        loadLastBooksRecyclerView();

        NavigationDrawerManager.setDrawerViews(getApplicationContext(), getWindowManager(), drawer_fullname,
                drawer_email, drawer_userPicture, NavigationDrawerManager.getNavigationDrawerProfile());
        navigationView.setCheckedItem(R.id.drawer_navigation_none);

    }

    @Override
    protected void onPause() {
        super.onPause();

        // Remove listeners
        borrowRequestsDb.removeEventListener(requestedBooksListener);
        favoritesDb.removeEventListener(favoriteBooksListener);
        booksDb.removeEventListener(lastBooksListener);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.show_case_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            finishAffinity();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        // Handle navigation view item clicks here.
        return NavigationDrawerManager.onNavigationItemSelected(this,null,item,getApplicationContext(),drawer,0);
    }

    @Override
    public void onSearchStateChanged(boolean enabled) {
        // Hide or show navbar when searchbar change state
        if (enabled) {
            navBar.setVisibility(View.GONE);
        } else {
            navBar.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Starts the search activity with the appropriate bundle
     */
    private void startSearchActivity(CharSequence searchInputText) {
        Intent i = new Intent(getApplicationContext(), SearchActivity.class);
        if (searchInputText != null)
            i.putExtra("searchInputText", searchInputText);
        startActivity(i);
    }

    //send intent to SearchActivity
    @Override
    public void onSearchConfirmed(CharSequence searchInputText) {

        searchBar.disableSearch();
        startSearchActivity(searchInputText);
    }

    @Override
    public void onButtonClicked(int buttonCode) {
        switch (buttonCode) {
            case MaterialSearchBar.BUTTON_NAVIGATION:
                DrawerLayout drawer = findViewById(R.id.show_case_drawer_layout);
                drawer.openDrawer(Gravity.START);
                break;
            case MaterialSearchBar.BUTTON_SPEECH:
                break;
            case MaterialSearchBar.BUTTON_BACK:
                MaterialSearchBar searchBar = findViewById(R.id.searchBar);
                searchBar.disableSearch();
                break;
        }
    }

    private Activity getActivityContext() {
        return this;
    }

    private void setupNavigationTools() {

        // Setup material serach bar
        searchBar = findViewById(R.id.searchBar);
        searchBar.setOnSearchActionListener(this);

        // Setup navigation drawer
        navigationView = findViewById(R.id.show_case_nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.drawer_navigation_none);
        drawer = findViewById(R.id.show_case_drawer_layout);

        // Update drawer with user info
        View nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        drawer_email = nav.findViewById(R.id.drawer_user_email);

        // Setup bottom navbar
        UserInterface.setupNavigationBar(this, R.id.navigation_showcase);
        navBar = findViewById(R.id.navigation);
    }

    private void createBorrowRequestsListener() {

        requestedBookIdList = new HashSet<>();
        favoritesBookIdList = new LinkedHashSet<>();

        // Create borrow requested books listener
        requestedBooksListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                requestedBookIdList.clear();

                if (dataSnapshot.getChildrenCount() == 0)
                    return;

                // Read books for which a loan request has been sent
                for (DataSnapshot bookIdSnapshot : dataSnapshot.getChildren()) {
                    String bookId = bookIdSnapshot.getKey();
                    requestedBookIdList.add(bookId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };

        // Get favorites books list
        favoriteBooksListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                favoritesBookIdList.clear();

                // Read books for which a loan request has been sent
                for (DataSnapshot bookIdSnapshot : dataSnapshot.getChildren()) {
                    String bookId = bookIdSnapshot.getKey();
                    favoritesBookIdList.add(bookId);
                }

                loadFavoriteBooksRecylerView();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
    }

    private void setupRecyclerViews() {

        // LAST BOOKS recycler view
        lastBooksRV = findViewById(R.id.showcase_rv_last);
        lastBooksRV.setHasFixedSize(true);
        LinearLayoutManager lastBooksLM = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        lastBooksRV.setLayoutManager(lastBooksLM);

        // FAVORITE BOOKS recycler view
        favoriteBooksRV = findViewById(R.id.showcase_rv_favorites);
        favoriteBooksRV.setHasFixedSize(true);
        LinearLayoutManager favoriteBooksLM = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        favoriteBooksRV.setLayoutManager(favoriteBooksLM);

        // CLOSE BOOKS recycler view
        closeBooksRV = findViewById(R.id.showcase_rv_closebooks);
        closeBooksRV.setHasFixedSize(true);
        LinearLayoutManager closeBooksLM = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        closeBooksRV.setLayoutManager(closeBooksLM);
    }

    private void loadLastBooksRecyclerView() {

        // Load Last Book RV
        booksDb.orderByChild("creationTime").limitToLast(20)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        List<Book> bookList = new ArrayList<>();

                        if (dataSnapshot.getChildrenCount() == 0)
                            return;

                        // Read books
                        for (DataSnapshot bookSnapshot : dataSnapshot.getChildren()) {
                            Book book = bookSnapshot.getValue(Book.class);
                            book.setBookId(bookSnapshot.getKey());
                            bookList.add(0, book);
                        }

                        // Specify an adapter
                        lastBooksAdapter = new ShowBooksAdapter(getActivityContext(), bookList, mLocation, favoritesBookIdList, requestedBookIdList);
                        lastBooksRV.setAdapter(lastBooksAdapter);
                        findViewById(R.id.showcase_cw_lastbook).setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("ERROR", "There was an error while fetching last book inserted");
                    }
                });


        lastBooksListener = booksDb.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                Book updatedBook = dataSnapshot.getValue(Book.class);
                updatedBook.setBookId(dataSnapshot.getKey());

                if (lastBooksAdapter != null)
                    lastBooksAdapter.updateItem(updatedBook);

                if (favoriteBooksAdapter != null)
                    favoriteBooksAdapter.updateItem(updatedBook);

                if (closeBooksAdapter != null)
                    closeBooksAdapter.updateItem(updatedBook);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        // Set MORE button listener
        findViewById(R.id.last_more_button).setOnClickListener(v -> {
            Intent i = new Intent(this, ShowMoreActivity.class);
            i.putExtra("moreType", ShowMoreActivity.LAST_BOOKS);
            if (mLocation != null) {
                i.putExtra("latitude", mLocation.getLatitude());
                i.putExtra("longitude", mLocation.getLongitude());
            }
            startActivity(i);
        });
    }

    private void loadFavoriteBooksRecylerView() {

        // Load Favorite Books RV
        List<Book> bookList = new ArrayList<>();
        final long bookCount = favoritesBookIdList.size();

        if (bookCount == 0) {
            findViewById(R.id.showcase_cw_favorites).setVisibility(View.GONE);
            return;
        }

        // Read books reference
        for (String bookId: favoritesBookIdList) {

            booksDb.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Book book = dataSnapshot.getValue(Book.class);
                    book.setBookId(dataSnapshot.getKey());
                    bookList.add(0, book);

                    if (bookList.size() == bookCount) {
                        // Set RV adapter
                        favoriteBooksAdapter = new ShowBooksAdapter(getActivityContext(), bookList, mLocation, favoritesBookIdList, requestedBookIdList);
                        favoriteBooksRV.setAdapter(favoriteBooksAdapter);
                        findViewById(R.id.showcase_cw_favorites).setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }

        // Set MORE button listener
        findViewById(R.id.favorites_more_button).setOnClickListener(v -> {
            Intent i = new Intent(this, ShowMoreActivity.class);
            i.putExtra("moreType", ShowMoreActivity.FAVORITES_BOOKS);
            if (mLocation != null) {
                i.putExtra("latitude", mLocation.getLatitude());
                i.putExtra("longitude", mLocation.getLongitude());
            }
            startActivity(i);
        });
    }

    private void loadCloseBooksRecyclerView() {

        // Check if location is available
        if (mLocation == null) {
            findViewById(R.id.showcase_cw_closebooks).setVisibility(View.GONE);
            return;
        }

        // Get close books
        List<String> queryResults = new ArrayList<>();
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(mLocation.getLatitude(), mLocation.getLongitude()), 10.0);
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                queryResults.add(key);
            }

            @Override
            public void onKeyExited(String key) {
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
            }

            @Override
            public void onGeoQueryReady() {
                loadCloseBooksAdapter(queryResults);
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });

        // Set MORE button listener
        findViewById(R.id.closebooks_more_button).setOnClickListener(v -> {
            Intent i = new Intent(this, ShowMoreActivity.class);
            i.putExtra("moreType", ShowMoreActivity.CLOSE_BOOKS);
            if (mLocation != null) {
                i.putExtra("latitude", mLocation.getLatitude());
                i.putExtra("longitude", mLocation.getLongitude());
            }
            startActivity(i);
        });
    }

    private void loadCloseBooksAdapter(List<String> geofireResults) {

        List<Book> bookList = new ArrayList<>();
        AtomicLong bookCount = new AtomicLong(geofireResults.size());

        if (bookCount.get() == 0) {
            findViewById(R.id.showcase_cw_closebooks).setVisibility(View.GONE);
            return;
        }

        // Read books reference
        for (String bookId : geofireResults) {

            booksDb.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Book book = dataSnapshot.getValue(Book.class);
                    book.setBookId(dataSnapshot.getKey());

                    if (book.getOwner_uid().equals(user_id) || book.isShared()) {
                        bookCount.decrementAndGet();
                    } else {
                        bookList.add(book);
                    }

                    if (bookList.size() == bookCount.get()) {
                        // Set RV adapter
                        closeBooksAdapter = new ShowBooksAdapter(getActivityContext(), bookList, mLocation, favoritesBookIdList, requestedBookIdList);
                        closeBooksRV.setAdapter(closeBooksAdapter);

                        if (bookCount.get() > 0)
                            findViewById(R.id.showcase_cw_closebooks).setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }

    @SuppressLint("MissingPermission")
    private void checkLocationThenLoadCloseBooks() {

        // Get last location
        PermissionsHandler.check(this, () -> {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                mLocation = location;
                loadCloseBooksRecyclerView();
            });
        });
    }
}
