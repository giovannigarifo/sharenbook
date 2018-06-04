package it.polito.mad.sharenbook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import it.polito.mad.sharenbook.adapters.ShowBooksAdapter;
import it.polito.mad.sharenbook.fragments.GenericAlertDialog;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.PermissionsHandler;
import it.polito.mad.sharenbook.utils.Utils;

public class ShowMoreActivity extends AppCompatActivity {

    public static final int LAST_BOOKS = 0;
    public static final int FAVORITES_BOOKS = 1;
    public static final int CLOSE_BOOKS = 2;

    private Activity thisActivity;
    private int moreType;

    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;

    private DatabaseReference booksDb;
    private DatabaseReference favoritesDb;
    private DatabaseReference borrowRequestsDb;

    private ValueEventListener requestedBooksListener;
    private ValueEventListener favoriteBooksListener;

    private String user_id, username;
    private LinkedHashSet<String> favoritesBookIdList;
    private HashSet<String> requestedBookIdList;

    private RecyclerView moreBooksRV;

    public String selectedBookOwner, selectedBookId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_more);
        thisActivity = this;

        // Get bundle info
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            moreType = bundle.getInt("moreType", 0);
        } else {
            return;
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.showmore_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(getToolbarTitle());
        toolbar.getNavigationIcon().setColorFilter(getResources().getColor(R.color.black), PorterDuff.Mode.SRC_ATOP);

        // Get username and user_id
        SharedPreferences userData = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(getString(R.string.username_copy_key), "");
        user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Get Firebase reference
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        booksDb = db.getReference(getString(R.string.books_key));
        booksDb.keepSynced(true);
        favoritesDb = db.getReference(getString(R.string.users_key)).child(user_id).child(getString(R.string.user_favorites_key));
        favoritesDb.keepSynced(true);
        borrowRequestsDb = db.getReference(getString(R.string.usernames_key)).child(username).child(getString(R.string.borrow_requests_key));

        // Create requested and favorite books listeners
        createListeners();

        // Setup recyclerview
        moreBooksRV = findViewById(R.id.showcase_rv_more);
        moreBooksRV.setHasFixedSize(true);
        GridLayoutManager moreBooksLM = new GridLayoutManager(this, getGridColumnCount());
        moreBooksRV.setLayoutManager(moreBooksLM);

        //Setup location client and get current location
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        checkLocation();

        // Load books
        loadBooks();
    }

    @Override
    protected void onPause() {
        super.onPause();
        borrowRequestsDb.removeEventListener(requestedBooksListener);
        favoritesDb.removeEventListener(favoriteBooksListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        borrowRequestsDb.addValueEventListener(requestedBooksListener);
        favoritesDb.orderByValue().addValueEventListener(favoriteBooksListener);
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }

    private String getToolbarTitle() {

        switch (moreType) {
            case LAST_BOOKS:
                return getString(R.string.showcase_lastbook);
            case FAVORITES_BOOKS:
                return getString(R.string.showcase_favorites);
            case CLOSE_BOOKS:
                return getString(R.string.showcase_closebooks);
            default:
                return "";
        }
    }

    private void createListeners() {

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

                if (moreType == FAVORITES_BOOKS)
                    loadFavoriteBooks();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
    }

    @SuppressLint("MissingPermission")
    private void checkLocation() {

        // Get last location
        PermissionsHandler.check(this, () -> {
            mFusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                mLocation = location;

                if (moreType == CLOSE_BOOKS) {
                    loadCloseBooks();
                }
            });
        });
    }

    private int getGridColumnCount() {

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int wInches = metrics.widthPixels / metrics.densityDpi;

        if (wInches <= 3) return 3;
        else if (wInches <= 4) return 4;
        else if (wInches <= 5) return 5;
        else if (wInches <= 6) return 6;
        else if (wInches <= 7) return 7;
        else return 8;
    }

    private void loadBooks() {

        if (moreType == LAST_BOOKS) {
            loadLastBooks();
        }
    }

    private void loadLastBooks() {

        booksDb.orderByChild("creationTime").addListenerForSingleValueEvent(new ValueEventListener() {

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
                ShowBooksAdapter booksAdapter = new ShowBooksAdapter(thisActivity, bookList, mLocation, favoritesBookIdList, requestedBookIdList);
                moreBooksRV.setAdapter(booksAdapter);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("ERROR", "There was an error while fetching last book inserted");
            }
        });
    }

    private void loadFavoriteBooks() {

        List<Book> bookList = new ArrayList<>();
        final long bookCount = favoritesBookIdList.size();

        if (bookCount == 0) {
            finish();
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
                        ShowBooksAdapter booksAdapter = new ShowBooksAdapter(thisActivity, bookList, mLocation, favoritesBookIdList, requestedBookIdList);
                        moreBooksRV.setAdapter(booksAdapter);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }

    private void loadCloseBooks() {

        // Get Geofire
        DatabaseReference location_ref = FirebaseDatabase.getInstance().getReference("books_locations");
        GeoFire geoFire = new GeoFire(location_ref);

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
    }

    private void loadCloseBooksAdapter(List<String> geofireResults) {

        List<Book> bookList = new ArrayList<>();
        AtomicLong bookCount = new AtomicLong(geofireResults.size());

        if (bookCount.get() == 0) {
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
                        ShowBooksAdapter booksAdapter = new ShowBooksAdapter(thisActivity, bookList, mLocation, favoritesBookIdList, requestedBookIdList);
                        moreBooksRV.setAdapter(booksAdapter);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }
}
