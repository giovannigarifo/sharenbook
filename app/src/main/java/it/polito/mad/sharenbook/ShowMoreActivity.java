package it.polito.mad.sharenbook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
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
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.PermissionsHandler;
import it.polito.mad.sharenbook.utils.Utils;

public class ShowMoreActivity extends AppCompatActivity {

    public static final int LAST_BOOKS = 0;
    public static final int FAVORITES_BOOKS = 1;
    public static final int CLOSE_BOOKS = 2;

    private int moreType;

    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;

    private DatabaseReference booksDb;
    private DatabaseReference favoritesDb;
    private DatabaseReference borrowRequestsDb;

    private ValueEventListener requestedBooksListener;
    private ValueEventListener favoriteBooksListener;

    private String user_id, username;
    private HashSet<String> favoritesBookIdList;
    private HashSet<String> requestedBookIdList;

    private RecyclerView moreBooksRV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_more);

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

        // Popolate recyclerview
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
        favoritesDb.addValueEventListener(favoriteBooksListener);
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

        // Create borrow requested books listener
        requestedBooksListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                requestedBookIdList = new HashSet<>();

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

                if (requestedBookIdList == null)
                    requestedBookIdList = new HashSet<>();
            }
        };

        // Get favorites books list
        favoriteBooksListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                favoritesBookIdList = new HashSet<>();

                if (dataSnapshot.getChildrenCount() == 0)
                    return;

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

                if (favoritesBookIdList == null)
                    favoritesBookIdList = new HashSet<>();
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
                MyAdapter booksAdapter = new MyAdapter(bookList);
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
                        MyAdapter booksAdapter = new MyAdapter(bookList);
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
                        MyAdapter booksAdapter = new MyAdapter(bookList);
                        moreBooksRV.setAdapter(booksAdapter);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }

    /**
     * Recycler View Adapter Class
     */
    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        private Activity mActivity;
        private StorageReference mBookImagesStorage;
        private List<Book> mBookList;

        class ViewHolder extends RecyclerView.ViewHolder {

            ConstraintLayout mLayout;
            ImageView bookPhoto;
            TextView bookTitle;
            TextView bookDistance;
            ImageView bookOptions;
            ImageView bookmarkIcon;
            ImageView bookUnavailable;

            ViewHolder(ConstraintLayout layout) {
                super(layout);
                mLayout = layout;
                bookPhoto = layout.findViewById(R.id.showcase_rv_book_photo);
                bookTitle = layout.findViewById(R.id.showcase_rv_book_title);
                bookDistance = layout.findViewById(R.id.showcase_rv_book_location);
                bookOptions = layout.findViewById(R.id.showcase_rv_book_options);
                bookmarkIcon = layout.findViewById(R.id.showcase_rv_book_shared);
                bookUnavailable = layout.findViewById(R.id.showcase_rv_unavailable_bg);
            }
        }

        MyAdapter(List<Book> bookList) {
            mActivity = ShowMoreActivity.this;
            mBookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));
            mBookList = bookList;
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // create a new view
            ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book_showmore_rv, parent, false);

            return new ViewHolder(layout);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

            Book book = mBookList.get(position);
            String fileName = book.getPhotosName().get(0);
            StorageReference photoRef = mBookImagesStorage.child(book.getBookId()).child(fileName);

            // Load book photo
            GlideApp.with(mActivity)
                    .load(photoRef)
                    .placeholder(R.drawable.book_cover_portrait)
                    .into(holder.bookPhoto);

            // Put bookmark icon if already shared
            if (book.isShared())
                holder.bookUnavailable.setVisibility(View.VISIBLE);
            else
                holder.bookUnavailable.setVisibility(View.GONE);


            // Set title
            holder.bookTitle.setText(book.getTitle());

            // Set distance
            if (mLocation != null) {
                String distance = Utils.distanceBetweenLocations(
                        mLocation.getLatitude(),
                        mLocation.getLongitude(),
                        book.getLocation_lat(),
                        book.getLocation_long());
                holder.bookDistance.setText(distance);
                holder.bookDistance.setVisibility(View.VISIBLE);

            } else {
                holder.bookDistance.setVisibility(View.GONE);
            }

            // Set listener
            holder.mLayout.setOnClickListener(v -> {
                Intent i = new Intent(mActivity, ShowBookActivity.class);
                i.putExtra("book", book);
                mActivity.startActivity(i);
            });

            // Setup options menu
            holder.bookOptions.setOnClickListener(v -> {
                showOptionsPopupMenu(v, book);
            });
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mBookList.size();
        }

        private void showOptionsPopupMenu(View v, Book book) {

            final PopupMenu popup = new PopupMenu(mActivity, v);
            popup.inflate(R.menu.showcase_rv_options_menu);
            popup.setOnMenuItemClickListener(item -> {

                DatabaseReference favoriteBooksRef = FirebaseDatabase.getInstance()
                        .getReference(getString(R.string.users_key))
                        .child(user_id)
                        .child(getString(R.string.user_favorites_key))
                        .child(book.getBookId());

                switch (item.getItemId()) {

                    case R.id.add_to_favorites:
                        favoriteBooksRef.setValue(ServerValue.TIMESTAMP, (databaseError, databaseReference) -> {
                            if (databaseError == null) {
                                Toast.makeText(getApplicationContext(), R.string.showcase_add_favorite, Toast.LENGTH_SHORT).show();
                            } else
                                Log.d("FIREBASE ERROR", "Favorite -> " + databaseError.getMessage());
                        });
                        return true;

                    case R.id.del_from_favorites:
                        favoriteBooksRef.removeValue((databaseError, databaseReference) -> {
                            if (databaseError == null) {

                                if (moreType == FAVORITES_BOOKS && mBookList.size() == 1) {
                                    finish();
                                }
                                Toast.makeText(getApplicationContext(), R.string.showcase_del_favorite, Toast.LENGTH_SHORT).show();
                            } else
                                Log.d("FIREBASE ERROR", "Favorite -> " + databaseError.getMessage());
                        });
                        return true;

                    case R.id.contact_owner:
                        Intent chatActivity = new Intent(mActivity, ChatActivity.class);
                        chatActivity.putExtra("recipientUsername", book.getOwner_username());
                        mActivity.startActivity(chatActivity);
                        return true;

                    case R.id.show_profile:
                        Intent showOwnerProfile = new Intent(mActivity, ShowOthersProfile.class);
                        showOwnerProfile.putExtra("username", book.getOwner_username());
                        mActivity.startActivity(showOwnerProfile);
                        return true;

                    case R.id.borrow_book:
                        //selectedBookOwner = book.getOwner_username();
                        //selectedBookId = book.getBookId();
                        //showDialog();
                        return true;

                    default:
                        return false;
                }
            });

            // Disable contact owner menu entry if is an user's book
            if (book.getOwner_uid().equals(user_id)) {
                popup.getMenu().getItem(0).setEnabled(false);
                popup.getMenu().getItem(2).setEnabled(false);
                popup.getMenu().getItem(3).setEnabled(false);
                popup.getMenu().getItem(4).setEnabled(false);

            } else {
                if (favoritesBookIdList.contains(book.getBookId())) {
                    popup.getMenu().getItem(0).setVisible(false);
                    popup.getMenu().getItem(1).setVisible(true);
                }
                if (book.isShared()) {
                    popup.getMenu().getItem(4).setTitle(R.string.book_unavailable).setEnabled(false);
                } else if (requestedBookIdList.contains(book.getBookId())) {
                    popup.getMenu().getItem(4).setVisible(false);
                    popup.getMenu().getItem(5).setVisible(true);
                }
            }

            popup.show();
        }
    }
}
