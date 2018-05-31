package it.polito.mad.sharenbook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.app.DialogFragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
import com.firebase.ui.auth.AuthUI;
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
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.onesignal.OneSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import it.polito.mad.sharenbook.fragments.GenericAlertDialog;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.PermissionsHandler;
import it.polito.mad.sharenbook.utils.UserInterface;
import it.polito.mad.sharenbook.utils.Utils;

public class ShowCaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, MaterialSearchBar.OnSearchActionListener {

    private MaterialSearchBar searchBar;
    private BottomNavigationView navBar;
    private boolean shouldExecuteOnResume;

    private DatabaseReference booksDb;
    private DatabaseReference favoritesDb;

    private FusedLocationProviderClient mFusedLocationClient;
    private Location mLocation;
    private GeoFire geoFire;

    private RecyclerView lastBooksRV;
    private RecyclerView favoriteBooksRV;
    private RecyclerView closeBooksRV;

    private String user_id, username;
    private String selectedBookOwner, selectedBookId;
    private HashSet<String> favoritesBookIdList;
    private HashSet<String> requestedBookIdList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_case);
        shouldExecuteOnResume = false;

        // Get username
        SharedPreferences userData = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(getString(R.string.username_copy_key), "");

        // Setup navigation tools
        setupNavigationTools();

        // Get bookDb reference
        booksDb = FirebaseDatabase.getInstance().getReference(getString(R.string.books_key));
        booksDb.keepSynced(true);

        // Get favoriteBooks reference
        user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        favoritesDb = FirebaseDatabase.getInstance().getReference(getString(R.string.users_key)).child(user_id).child(getString(R.string.user_favorites_key));
        favoritesDb.keepSynced(true);

        //Setup location client
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Get Geofire
        DatabaseReference location_ref = FirebaseDatabase.getInstance().getReference("books_locations");
        geoFire = new GeoFire(location_ref);

        // Load borrowRequests sent list
        loadBorrowRequests();

        // Load recycler views
        setupRecyclerViews();
        checkLocationThenLoadCloseBooks();
        loadLastBooksRecyclerView();
        loadFavoriteBooksRecylerView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        navBar.setSelectedItemId(R.id.navigation_showcase);
        if (searchBar.isSearchEnabled())
            searchBar.disableSearch();

        if (shouldExecuteOnResume) {
            //Reload recycler views
            checkLocationThenLoadCloseBooks();
            loadLastBooksRecyclerView();
            loadFavoriteBooksRecylerView();

        } else {
            shouldExecuteOnResume = true;
        }
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
        int id = item.getItemId();

        if (id == R.id.drawer_navigation_profile) {
            Intent i = new Intent(getApplicationContext(), TabbedShowProfileActivity.class);
            i.putExtra(getString(R.string.user_profile_data_key), NavigationDrawerManager.getUserParcelable(getApplicationContext()));
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
                        OneSignal.setSubscription(false);
                        Toast.makeText(getApplicationContext(), getString(R.string.log_out), Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }

        DrawerLayout drawer = findViewById(R.id.show_case_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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

    private void setupNavigationTools() {

        // Setup material serach bar
        searchBar = findViewById(R.id.searchBar);
        searchBar.setOnSearchActionListener(this);

        // Setup navigation drawer
        NavigationView navigationView = findViewById(R.id.show_case_nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setCheckedItem(R.id.drawer_navigation_none);

        // Update drawer with user info
        View nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        CircularImageView drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        TextView drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        TextView drawer_email = nav.findViewById(R.id.drawer_user_email);

        NavigationDrawerManager.setDrawerViews(getApplicationContext(), getWindowManager(), drawer_fullname,
                drawer_email, drawer_userPicture, NavigationDrawerManager.getNavigationDrawerProfile());

        // Setup bottom navbar
        UserInterface.setupNavigationBar(this, R.id.navigation_showcase);
        navBar = findViewById(R.id.navigation);
    }

    private void loadBorrowRequests() {

        DatabaseReference borrowRequestsDb = FirebaseDatabase.getInstance()
                .getReference(getString(R.string.usernames_key))
                .child(username)
                .child(getString(R.string.borrow_requests_key));

        // Get borrow requested books
        borrowRequestsDb.addValueEventListener(new ValueEventListener() {
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
        });
    }

    private void setupRecyclerViews() {

        // LAST BOOKS recycler view
        lastBooksRV = findViewById(R.id.showcase_rv_last);
        lastBooksRV.setHasFixedSize(true);
        LinearLayoutManager lastBooksLM = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        lastBooksRV.setLayoutManager(lastBooksLM);
        //LinearSnapHelper lastLinearSnapHelper = new LinearSnapHelper();
        //lastLinearSnapHelper.attachToRecyclerView(lastBooksRV);

        // FAVORITE BOOKS recycler view
        favoriteBooksRV = findViewById(R.id.showcase_rv_favorites);
        favoriteBooksRV.setHasFixedSize(true);
        LinearLayoutManager favoriteBooksLM = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        favoriteBooksRV.setLayoutManager(favoriteBooksLM);
        //LinearSnapHelper favoritesLinearSnapHelper = new LinearSnapHelper();
        //favoritesLinearSnapHelper.attachToRecyclerView(favoriteBooksRV);

        // CLOSE BOOKS recycler view
        closeBooksRV = findViewById(R.id.showcase_rv_closebooks);
        closeBooksRV.setHasFixedSize(true);
        LinearLayoutManager closeBooksLM = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        closeBooksRV.setLayoutManager(closeBooksLM);
        //LinearSnapHelper closeLinearSnapHelper = new LinearSnapHelper();
        //closeLinearSnapHelper.attachToRecyclerView(closeBooksRV);
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
                        MyAdapter lastBooksAdapter = new MyAdapter(bookList);
                        lastBooksRV.setAdapter(lastBooksAdapter);
                        findViewById(R.id.showcase_cw_lastbook).setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("ERROR", "There was an error while fetching last book inserted");
                    }
                });

        // Set MORE button listener
        findViewById(R.id.last_more_button).setOnClickListener(v -> Toast.makeText(getApplicationContext(), R.string.to_be_implemented, Toast.LENGTH_SHORT).show());
    }

    private void loadFavoriteBooksRecylerView() {

        // Load Favorite Books RV
        favoritesDb.orderByValue().limitToLast(20)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        List<Book> bookList = new ArrayList<>();
                        favoritesBookIdList = new HashSet<>();
                        final long bookCount = dataSnapshot.getChildrenCount();

                        if (bookCount == 0) {
                            findViewById(R.id.showcase_cw_favorites).setVisibility(View.GONE);
                            return;
                        }

                        // Read books reference
                        for (DataSnapshot bookIdSnapshot : dataSnapshot.getChildren()) {
                            String bookId = bookIdSnapshot.getKey();
                            favoritesBookIdList.add(bookId);

                            booksDb.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {
                                    Book book = dataSnapshot.getValue(Book.class);
                                    book.setBookId(dataSnapshot.getKey());
                                    bookList.add(0, book);

                                    if (bookList.size() == bookCount) {
                                        // Set RV adapter
                                        MyAdapter favoriteBooksAdapter = new MyAdapter(bookList);
                                        favoriteBooksRV.setAdapter(favoriteBooksAdapter);
                                        findViewById(R.id.showcase_cw_favorites).setVisibility(View.VISIBLE);
                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

        // Set MORE button listener
        findViewById(R.id.favorites_more_button).setOnClickListener(v -> Toast.makeText(getApplicationContext(), R.string.to_be_implemented, Toast.LENGTH_SHORT).show());
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
                loadCloseBooksAdapter(queryResults, closeBooksRV);
            }

            @Override
            public void onGeoQueryError(DatabaseError error) {
            }
        });

        // Set MORE button listener
        findViewById(R.id.closebooks_more_button).setOnClickListener(v -> Toast.makeText(getApplicationContext(), R.string.to_be_implemented, Toast.LENGTH_SHORT).show());
    }

    private void loadCloseBooksAdapter(List<String> geofireResults, RecyclerView closeBooksRV) {

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
                        MyAdapter closeBooksAdapter = new MyAdapter(bookList);
                        closeBooksRV.setAdapter(closeBooksAdapter);
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
            mActivity = ShowCaseActivity.this;
            mBookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));
            mBookList = bookList;
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // create a new view
            ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book_showcase_rv, parent, false);

            return new ViewHolder(layout);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@NonNull MyAdapter.ViewHolder holder, int position) {

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
                                loadFavoriteBooksRecylerView();
                                Toast.makeText(getApplicationContext(), R.string.showcase_add_favorite, Toast.LENGTH_SHORT).show();
                            } else
                                Log.d("FIREBASE ERROR", "Favorite -> " + databaseError.getMessage());
                        });
                        return true;

                    case R.id.del_from_favorites:
                        favoriteBooksRef.removeValue((databaseError, databaseReference) -> {
                            if (databaseError == null) {
                                loadFavoriteBooksRecylerView();
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

                    case R.id.borrow_book:
                        selectedBookOwner = book.getOwner_username();
                        selectedBookId = book.getBookId();
                        showDialog();

                    default:
                        return false;
                }
            });

            // Disable contact owner menu entry if is an user's book
            if (book.getOwner_uid().equals(user_id)) {
                popup.getMenu().getItem(0).setEnabled(false);
                popup.getMenu().getItem(2).setEnabled(false);
                popup.getMenu().getItem(3).setEnabled(false);

            } else {
                if (favoritesBookIdList.contains(book.getBookId())) {
                    popup.getMenu().getItem(0).setVisible(false);
                    popup.getMenu().getItem(1).setVisible(true);
                }
                if (book.isShared()) {
                    popup.getMenu().getItem(3).setTitle(R.string.book_unavailable).setEnabled(false);
                } else if (requestedBookIdList.contains(book.getBookId())) {
                    popup.getMenu().getItem(3).setVisible(false);
                    popup.getMenu().getItem(4).setVisible(true);
                }
            }

            popup.show();
        }
    }

    private void firebaseInsertRequest() {

        DatabaseReference usernamesDb = FirebaseDatabase.getInstance().getReference(getString(R.string.usernames_key));

        // Create transaction Map
        Map<String, Object> transaction = new HashMap<>();
        transaction.put(username + "/" + getString(R.string.borrow_requests_key) + "/" + selectedBookId, ServerValue.TIMESTAMP);
        transaction.put(selectedBookOwner + "/" + getString(R.string.pending_requests_key) + "/" + selectedBookId + "/" + username, ServerValue.TIMESTAMP);

        // Push entire transaction
        usernamesDb.updateChildren(transaction, (databaseError, databaseReference) -> {
            if (databaseError == null) {

                String requestBody = "{"
                        + "\"app_id\": \"edfbe9fb-e0fc-4fdb-b449-c5d6369fada5\","

                        + "\"filters\": [{\"field\": \"tag\", \"key\": \"User_ID\", \"relation\": \"=\", \"value\": \"" + selectedBookOwner + "\"}],"

                        + "\"data\": {\"notificationType\": \"bookRequest\", \"senderName\": \"" + username + "\", \"senderUid\": \"" + user_id + "\"},"
                        + "\"contents\": {\"en\": \"" + username + " wants to borrow your book!\", " +
                        "\"it\": \"" + username + " vuole in prestito un tuo libro!\"},"
                        + "\"headings\": {\"en\": \"Someone wants one of your books!\", \"it\": \"Qualcuno è interessato a un tuo libro!\"}"
                        + "}";

                // Send notification
                Utils.sendNotification(requestBody);
                Toast.makeText(getApplicationContext(), R.string.borrow_request_done, Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(getApplicationContext(), R.string.borrow_request_fail, Toast.LENGTH_LONG).show();
            }
        });
    }

    void showDialog() {
        DialogFragment newFragment = GenericAlertDialog.newInstance(
                R.string.borrow_book, getString(R.string.borrow_book_msg));
        newFragment.show(getSupportFragmentManager(), "borrow_dialog");
    }

    public void doPositiveClick() {
        if (!selectedBookOwner.equals("")) {
            firebaseInsertRequest();
        }
    }

    public void doNegativeClick() {
        selectedBookOwner = "";
    }

}
