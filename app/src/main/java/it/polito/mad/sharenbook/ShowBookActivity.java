package it.polito.mad.sharenbook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.GenericFragmentDialog;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.UserInterface;
import it.polito.mad.sharenbook.utils.Utils;
import it.polito.mad.sharenbook.utils.ZoomLinearLayoutManager;

public class ShowBookActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnMapReadyCallback {

    private Book book;

    private GoogleMap mMap;

    private String user_id;
    private String username;

    private DatabaseReference favoriteBooksRef;
    private DatabaseReference borrowRequestRef;

    private ImageView favoriteBtn;
    private boolean favoriteClicked;


    private ValueEventListener requestedBookListener;

    private NavigationView navigationView;
    private DrawerLayout drawer;
    private CircularImageView drawer_userPicture;
    private TextView drawer_fullname;
    private TextView drawer_email;

    private boolean locationSet = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_book);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapContainer);
        mapFragment.getMapAsync(this);

        // Setup navigation tools
        setupNavigationTools();

        // Get book info
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {

            book = bundle.getParcelable("book");

            if (book == null) {
                String bookId = bundle.getString("bookId");
                getBookData(bookId);
            } else {
                initActivity();
            }
        }


    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.show_book_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        return NavigationDrawerManager.onNavigationItemSelected(this,null,item,getApplicationContext(),drawer,0);

    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationDrawerManager.setDrawerViews(getApplicationContext(), getWindowManager(), drawer_fullname,
                drawer_email, drawer_userPicture, NavigationDrawerManager.getNavigationDrawerProfile());
        navigationView.setCheckedItem(R.id.drawer_navigation_none);

        if (requestedBookListener != null)
            borrowRequestRef.addValueEventListener(requestedBookListener);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (requestedBookListener != null)
            borrowRequestRef.removeEventListener(requestedBookListener);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setMapToolbarEnabled(false);

        if(book != null && !locationSet) {
            locationSet = true;
            LatLng loc = new LatLng(book.getLocation_lat(), book.getLocation_long());
            Marker m = mMap.addMarker(new MarkerOptions()
                    .position(loc));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(m.getPosition(), 14));
        }

    }

    private Activity getActivityContext() {
        return this;
    }

    private void initActivity() {

        // Get current user info
        user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SharedPreferences userData = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(getString(R.string.username_copy_key), "");

        // Setup firebase
        DatabaseReference favoriteBooksDb = FirebaseDatabase.getInstance().getReference(getString(R.string.users_key)).child(user_id).child(getString(R.string.user_favorites_key));
        favoriteBooksRef = favoriteBooksDb.child(book.getBookId());
        borrowRequestRef = FirebaseDatabase.getInstance().getReference("usernames").child(username).child("borrowRequests").child(book.getBookId());

        // Setup favorite button
        setupFavoriteButton();

        // Setup RecyclerView
        RecyclerView mRecyclerView = findViewById(R.id.showbook_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        // Use a zoom linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new ZoomLinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false, UserInterface.convertDpToPixel(120));
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Specify an adapter
        RecyclerView.Adapter mAdapter = new MyAdapter(book, this);
        mRecyclerView.setAdapter(mAdapter);

        // Load book data into view
        loadViewWithBookData();

        if(mMap != null && !locationSet) {
            locationSet = true;
            LatLng loc = new LatLng(book.getLocation_lat(), book.getLocation_long());
            Marker m = mMap.addMarker(new MarkerOptions()
                    .position(loc));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(m.getPosition(), 14));
        }

        // Setup book buttons
        setupBookActionsButton();

    }

    private void setupBookActionsButton() {

        Button requestButton = findViewById(R.id.request_btn);
        Button contactButton = findViewById(R.id.contact_owner);

        if (book.getOwner_username().equals(username)) {
            findViewById(R.id.buttons_layout).setVisibility(View.GONE);
            return;
        } else if (book.isShared()) {
            requestButton.setEnabled(false);
            requestButton.setText(R.string.book_unavailable);
            requestButton.setAlpha(.7F);
        } else {
            setRequestButton(requestButton);
        }

        // Setup contact button listener
        contactButton.setOnClickListener(v -> {
            Intent chatActivity = new Intent(this, ChatActivity.class);
            chatActivity.putExtra("recipientUsername", book.getOwner_username());
            startActivity(chatActivity);
        });

    }

    private void setRequestButton(Button requestButton) {

        requestedBookListener = borrowRequestRef.addValueEventListener(new ValueEventListener() {

            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    requestButton.setText(R.string.undo_borrow_book_small);
                    requestButton.setOnClickListener(v -> {
                        String title = getString(R.string.undo_borrow_book);
                        String message = getString(R.string.undo_borrow_book_msg);
                        GenericFragmentDialog.show(getActivityContext(), title, message, () -> cancelBookRequest());
                    });
                } else {
                    requestButton.setText(R.string.borrow_book);
                    requestButton.setOnClickListener(v -> {
                        String title = getString(R.string.borrow_book);
                        String message = getString(R.string.borrow_book_msg);
                        GenericFragmentDialog.show(getActivityContext(), title, message, () -> requestBook());
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                requestButton.setEnabled(false);
            }
        });
    }

    private void requestBook() {

        DatabaseReference usernamesDb = FirebaseDatabase.getInstance().getReference(getString(R.string.usernames_key));

        // Create transaction Map
        Map<String, Object> transaction = new HashMap<>();
        transaction.put(username + "/" + getString(R.string.borrow_requests_key) + "/" + book.getBookId(), ServerValue.TIMESTAMP);
        transaction.put(book.getOwner_username() + "/" + getString(R.string.pending_requests_key) + "/" + book.getBookId() + "/" + username, ServerValue.TIMESTAMP);

        // Push entire transaction
        usernamesDb.updateChildren(transaction, (databaseError, databaseReference) -> {
            if (databaseError == null) {

                String requestBody = "{"
                        + "\"app_id\": \"edfbe9fb-e0fc-4fdb-b449-c5d6369fada5\","

                        + "\"filters\": [{\"field\": \"tag\", \"key\": \"User_ID\", \"relation\": \"=\", \"value\": \"" + book.getOwner_username() + "\"}],"

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

    private void cancelBookRequest() {

        DatabaseReference usernamesDb = FirebaseDatabase.getInstance().getReference(getString(R.string.usernames_key));

        // Create transaction Map
        Map<String, Object> transaction = new HashMap<>();
        transaction.put(username + "/" + getString(R.string.borrow_requests_key) + "/" + book.getBookId(), null);
        transaction.put(book.getOwner_username() + "/" + getString(R.string.pending_requests_key) + "/" + book.getBookId() + "/" + username, null);

        // Push entire transaction
        usernamesDb.updateChildren(transaction, (databaseError, databaseReference) -> {
            if (databaseError == null) {
                Toast.makeText(getApplicationContext(), R.string.borrow_request_undone, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getApplicationContext(), R.string.borrow_request_undone_fail, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void getBookData(String bookId) {

        DatabaseReference bookRef = FirebaseDatabase.getInstance().getReference(getString(R.string.books_key)).child(bookId);
        bookRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {

                    book = dataSnapshot.getValue(Book.class);
                    book.setBookId(dataSnapshot.getKey());
                    initActivity();

                } else {
                    Log.d("ERROR", "No book found with this id -> " + bookId);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });
    }

    private void setupFavoriteButton() {

        favoriteBtn = findViewById(R.id.show_book_btn_favorite);

        // Check if current user is the book owner
        if (user_id.equals(book.getOwner_uid())) {
            favoriteBtn.setVisibility(View.GONE);
            return;
        }

        // Check if this book is inside user's favorites list
        favoriteBooksRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    favoriteBtn.setImageResource(R.drawable.ic_favorite_black_24dp);
                    favoriteClicked = true;
                } else {
                    favoriteClicked = false;
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                favoriteClicked = false;
            }
        });

        // Add click handlers
        favoriteBtn.setOnClickListener(v -> {

            if (favoriteClicked) {
                favoriteBooksRef.removeValue((databaseError, databaseReference) -> {
                    if (databaseError == null) {
                        favoriteBtn.setImageResource(R.drawable.ic_favorite_border_black_24dp);
                        Toast.makeText(getApplicationContext(), R.string.showcase_del_favorite, Toast.LENGTH_SHORT).show();
                        favoriteClicked = false;
                    } else
                        Log.d("FIREBASE ERROR", "Favorite -> " + databaseError.getMessage());
                });

            } else {
                favoriteBooksRef.setValue(ServerValue.TIMESTAMP, (databaseError, databaseReference) -> {
                    if (databaseError == null) {
                        favoriteBtn.setImageResource(R.drawable.ic_favorite_black_24dp);
                        Toast.makeText(getApplicationContext(), R.string.showcase_add_favorite, Toast.LENGTH_SHORT).show();
                        favoriteClicked = true;
                    } else
                        Log.d("FIREBASE ERROR", "Favorite -> " + databaseError.getMessage());
                });
            }
        });
    }


    @SuppressLint("SetTextI18n")
    private void loadViewWithBookData() {

        TextView subtitleHeader = findViewById(R.id.showbook_tvh_subtitle);
        TextView publisherHeader = findViewById(R.id.showbook_tvh_publisher);
        TextView publishedDateHeader = findViewById(R.id.showbook_tvh_publishedDate);
        TextView descriptionHeader = findViewById(R.id.showbook_tvh_description);
        TextView pageCountHeader = findViewById(R.id.showbook_tvh_pageCount);
        TextView categoriesHeader = findViewById(R.id.showbook_tvh_categories);
        TextView languageHeader = findViewById(R.id.showbook_tvh_language);
        TextView tagsHeader = findViewById(R.id.showbook_tvh_tags);

        TextView isbn = findViewById(R.id.showbook_tvc_isbn);
        TextView title = findViewById(R.id.showbook_tvc_title);
        TextView subtitle = findViewById(R.id.showbook_tvc_subtitle);
        TextView authors = findViewById(R.id.showbook_tvc_authors);
        TextView publisher = findViewById(R.id.showbook_tvc_publisher);
        TextView publishedDate = findViewById(R.id.showbook_tvc_publishedDate);
        TextView description = findViewById(R.id.showbook_tvc_description);
        TextView pageCount = findViewById(R.id.showbook_tvc_pageCount);
        TextView categories = findViewById(R.id.showbook_tvc_categories);
        TextView language = findViewById(R.id.showbook_tvc_language);
        TextView location = findViewById(R.id.showbook_tvc_location);
        TextView bookConditions = findViewById(R.id.showbook_tvc_bookConditions);
        TextView tags = findViewById(R.id.showbook_tvc_tags);
        TextView owner = findViewById(R.id.published_by_tv);

        isbn.setText(book.getIsbn());
        title.setText(book.getTitle());
        owner.setText(App.getContext().getResources().getString(R.string.published_by,book.getOwner_username()));

        if (book.getSubtitle().equals("")) {
            subtitleHeader.setVisibility(View.GONE);
            subtitle.setVisibility(View.GONE);
        } else {
            subtitle.setText(book.getSubtitle());
        }

        authors.setText(Utils.listToCommaString(book.getAuthors()));

        if (book.getPublisher().equals("")) {
            publisherHeader.setVisibility(View.GONE);
            publisher.setVisibility(View.GONE);
        } else {
            publisher.setText(book.getPublisher());
        }

        if (book.getPublishedDate().equals("")) {
            publishedDateHeader.setVisibility(View.GONE);
            publishedDate.setVisibility(View.GONE);
        } else {
            publishedDate.setText(book.getPublishedDate());
        }

        if (book.getDescription().equals("")) {
            descriptionHeader.setVisibility(View.GONE);
            description.setVisibility(View.GONE);
        } else {
            description.setText(book.getDescription());
        }

        if (book.getPageCount() <= 0) {
            pageCountHeader.setVisibility(View.GONE);
            pageCount.setVisibility(View.GONE);
        } else {
            pageCount.setText(Integer.valueOf(book.getPageCount()).toString());
        }

        if (book.getCategories().size() == 0) {
            categoriesHeader.setVisibility(View.GONE);
            categories.setVisibility(View.GONE);
        } else {
            categories.setText(book.getCategoriesAsString(getResources().getStringArray(R.array.book_categories)));
        }

        if (book.getLanguage().equals("")) {
            languageHeader.setVisibility(View.GONE);
            language.setVisibility(View.GONE);
        } else {
            language.setText(book.getLanguage());
        }

        List<Address> places = new ArrayList<>();
        try {
            Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
            places.addAll(geocoder.getFromLocation(book.getLocation_lat(), book.getLocation_long(), 1));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (places.isEmpty()) {
            location.setText(R.string.unknown_place);
        } else {
            String bookLocation = places.get(0).getLocality() + ", " + places.get(0).getCountryName();
            location.setText(bookLocation);
        }

        bookConditions.setText(book.getBookConditionsAsString(getResources().getStringArray(R.array.book_conditions)));

        if (book.getTags().size() == 0) {
            tagsHeader.setVisibility(View.GONE);
            tags.setVisibility(View.GONE);
        } else {
            tags.setText(Utils.listToCommaString(book.getTags()));
        }
    }

    private void setupNavigationTools() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.show_book_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.showbook_toolbar_title);
        }

        // Setup navigation drawer
        drawer = findViewById(R.id.show_book_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.show_book_nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Update drawer with user info
        View nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        drawer_email = nav.findViewById(R.id.drawer_user_email);



        // Setup bottom navbar
        UserInterface.setupNavigationBar(this, R.id.navigation_myBook);

    }

    /**
     * Recycler View Adapter Class
     */
    private class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
        private Activity mActivity;
        private Book mBook;
        private List<String> mPhotosName;
        private StorageReference mBookImagesStorage;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            ImageView mImageView;

            ViewHolder(ImageView v) {
                super(v);
                mImageView = v;
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        MyAdapter(Book book, Activity activity) {
            mActivity = activity;
            mBook = book;
            mPhotosName = book.getPhotosName();
            mBookImagesStorage = FirebaseStorage.getInstance().getReference(activity.getString(R.string.book_images_key));
        }

        // Create new views (invoked by the layout manager)
        @NonNull
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            // create a new view
            ImageView v = (ImageView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book_imageview, parent, false);

            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

            String fileName = mPhotosName.get(position);
            StorageReference photoRef = mBookImagesStorage.child(mBook.getBookId() + "/" + fileName);

            GlideApp.with(mActivity)
                    .load(photoRef)
                    .into(holder.mImageView);

            holder.mImageView.setOnClickListener(view -> {
                Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
                i.putExtra("PictureSignature", 0);
                i.putExtra("pathPortion", mBook.getBookId() + "/" + fileName);
                i.putExtra("mode", 2);
                startActivity(i);
            });
        }

        @Override
        public int getItemCount() {
            return mPhotosName.size();
        }
    }
}
