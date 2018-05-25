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
import android.support.design.widget.FloatingActionButton;
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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
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
import java.util.List;
import java.util.Locale;

import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.UserInterface;
import it.polito.mad.sharenbook.utils.Utils;
import it.polito.mad.sharenbook.utils.ZoomLinearLayoutManager;

public class ShowBookActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Book book;

    private String user_id;
    private String username;

    private DatabaseReference favoriteBooksRef;

    private ImageView favoriteBtn;
    private boolean favoriteClicked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_book);

        // Setup navigation tools
        setupNavigationTools();

        // Get book info
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            book = bundle.getParcelable("book");
        }

        // Get current user info
        user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SharedPreferences userData = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(getString(R.string.username_copy_key), "");

        // Setup firebase
        DatabaseReference favoriteBooksDb = FirebaseDatabase.getInstance().getReference(getString(R.string.users_key)).child(user_id).child(getString(R.string.user_favorites_key));
        favoriteBooksRef = favoriteBooksDb.child(book.getBookId());

        // Setup favorite button
        setupFavoriteButton();

        // Setup RecyclerView
        RecyclerView mRecyclerView = findViewById(R.id.showbook_recycler_view);
        mRecyclerView.setHasFixedSize(true);

        // Use a zoom linear layout manager
        RecyclerView.LayoutManager mLayoutManager = new ZoomLinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false, UserInterface.convertDpToPixel(150));
        mRecyclerView.setLayoutManager(mLayoutManager);

        // Specify an adapter
        RecyclerView.Adapter mAdapter = new MyAdapter(book, this);
        mRecyclerView.setAdapter(mAdapter);

        // Load book data into view
        loadViewWithBookData();

        // Setup fab button for message with user
        setupFabContactUser();
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
                        Toast.makeText(getApplicationContext(), getString(R.string.log_out), Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }

        DrawerLayout drawer = findViewById(R.id.show_book_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("photoLoaded", true);
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

    private void setupFabContactUser() {
        //setup the chat fab
        FloatingActionButton fabContactUser = findViewById(R.id.message_user);

        if (!book.getOwner_username().equals(username)) {

            fabContactUser.setVisibility(View.VISIBLE);

            fabContactUser.setOnClickListener(view -> {
                Intent chatActivity = new Intent(getApplicationContext(), ChatActivity.class);
                chatActivity.putExtra("recipientUsername", book.getOwner_username());
                /*SharedPreferences userPreferences = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
                userPreferences.edit().putString("recipientUsername", book.getOwner_username()).commit();*/
                //chatActivity.putExtra("recipientUID", book.getOwner_uid());
                startActivity(chatActivity);
                finish();
            });

        } else {
            fabContactUser.setVisibility(View.GONE);
        }

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

        isbn.setText(book.getIsbn());
        title.setText(book.getTitle());

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
        DrawerLayout drawer = findViewById(R.id.show_book_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.show_book_nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Update drawer with user info
        View nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        CircularImageView drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        TextView drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        TextView drawer_email = nav.findViewById(R.id.drawer_user_email);

        NavigationDrawerManager.setDrawerViews(getApplicationContext(), getWindowManager(), drawer_fullname,
                drawer_email, drawer_userPicture, NavigationDrawerManager.getNavigationDrawerProfile());

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

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            String fileName = mPhotosName.get(position);
            StorageReference photoRef = mBookImagesStorage.child(mBook.getBookId() + "/" + fileName);

            GlideApp.with(mActivity)
                    .load(photoRef)
                    .into(holder.mImageView);
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mPhotosName.size();
        }
    }
}
