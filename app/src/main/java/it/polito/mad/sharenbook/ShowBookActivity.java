package it.polito.mad.sharenbook;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
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
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.ImageUtils;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.UserInterface;
import it.polito.mad.sharenbook.utils.ZoomLinearLayoutManager;

public class ShowBookActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private StorageReference bookImagesStorage;
    private List<StorageReference> bookPhotos;
    private Book book;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private FloatingActionButton fabContactUser;

    private String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_book);

        // Setup navigation tools
        setupNavigationTools();

        // Setup Firebase storage
        bookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));

        // Get book info
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            book = bundle.getParcelable("book");
        }

        SharedPreferences userData = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(getString(R.string.username_copy_key), "");

        mRecyclerView = findViewById(R.id.showbook_recycler_view);

        // Improve recyclerview performance
        mRecyclerView.setHasFixedSize(true);

        // Use a zoom linear layout manager
        mLayoutManager = new ZoomLinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false, UserInterface.convertDpToPixel(150));
        mRecyclerView.setLayoutManager(mLayoutManager);

        setupFabContactUser();

        // Specify an adapter
        mAdapter = new MyAdapter(book, this, bookImagesStorage);
        mRecyclerView.setAdapter(mAdapter);

        // Load book data into view
        loadViewWithBookData();
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
            Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
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

    private void setupFabContactUser(){
        //setup the chat fab
        fabContactUser = findViewById(R.id.message_user);

        if(!book.getOwner_username().equals(username)) {

            fabContactUser.setVisibility(View.VISIBLE);

            fabContactUser.setOnClickListener(view -> {
                Intent chatActivity = new Intent(getApplicationContext(), ChatActivity.class);
                chatActivity.putExtra("recipientUsername", book.getOwner_username());
                chatActivity.putExtra("recipientUID", book.getOwner_uid());
                chatActivity.putExtra("username", username);
                startActivity(chatActivity);
                finish();
            });

        } else {
            fabContactUser.setVisibility(View.GONE);
        }

    }

    private void loadViewWithBookData() {

        TextView isbnHeader = findViewById(R.id.showbook_tvh_isbn);
        TextView titleHeader = findViewById(R.id.showbook_tvh_title);
        TextView subtitleHeader = findViewById(R.id.showbook_tvh_subtitle);
        TextView authorsHeader = findViewById(R.id.showbook_tvh_authors);
        TextView publisherHeader = findViewById(R.id.showbook_tvh_publisher);
        TextView publishedDateHeader = findViewById(R.id.showbook_tvh_publishedDate);
        TextView descriptionHeader = findViewById(R.id.showbook_tvh_description);
        TextView pageCountHeader = findViewById(R.id.showbook_tvh_pageCount);
        TextView categoriesHeader = findViewById(R.id.showbook_tvh_categories);
        TextView languageHeader = findViewById(R.id.showbook_tvh_language);
        TextView locationHeader = findViewById(R.id.showbook_tvh_location);
        TextView bookConditionsHeader = findViewById(R.id.showbook_tvh_bookConditions);
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

        authors.setText(listToCommaString(book.getAuthors()));

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
            categories.setText(listToCommaString(book.getCategories()));
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
            places.addAll(geocoder.getFromLocation(Double.parseDouble(book.getLocation_lat()), Double.parseDouble(book.getLocation_long()), 1));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (places.isEmpty()) {
            location.setText(R.string.unknown_place);
        } else {
            location.setText(places.get(0).getLocality() + ", " + places.get(0).getCountryName());
        }

        bookConditions.setText(book.getBookConditions());

        if (book.getTags().size() == 0) {
            tagsHeader.setVisibility(View.GONE);
            tags.setVisibility(View.GONE);
        } else {
            tags.setText(listToCommaString(book.getTags()));
        }
    }

    /**
     * Convert a string list to comma separated multiple words string
     */
    private String listToCommaString(List<String> stringList) {
        StringBuilder sb = new StringBuilder();

        String prefix = "";
        for (String string : stringList) {
            sb.append(prefix);
            prefix = ", ";
            sb.append(string);
        }

        return sb.toString();
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
        setupNavbar();
    }

    private void setupNavbar() {
        BottomNavigationView navBar = findViewById(R.id.navigation);

        // Set navigation_shareBook as selected item
        navBar.setSelectedItemId(R.id.navigation_myBook);

        // Set the listeners for the navigation bar items
        navBar.setOnNavigationItemSelectedListener(item -> {

            switch (item.getItemId()) {
                case R.id.navigation_profile:
                    Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                    finish();
                    break;

                case R.id.navigation_search:
                    Intent searchBooks = new Intent(getApplicationContext(), SearchActivity.class);
                    startActivity(searchBooks);
                    finish();
                    break;

                case R.id.navigation_myBook:
                    Intent my_books = new Intent(getApplicationContext(), MyBookActivity.class);
                    startActivity(my_books);
                    finish();
                    break;
            }

            return true;
        });
    }
}

/**
 * Recycler View Adapter Class
 */
class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {
    private Activity mActivity;
    private Book mBook;
    private List<String> mPhotosName;
    private StorageReference mBookImagesStorage;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView mImageView;

        public ViewHolder(ImageView v) {
            super(v);
            mImageView = v;
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public MyAdapter(Book book, Activity activity, StorageReference bookImagesStorage) {
        mActivity = activity;
        mBook = book;
        mPhotosName = book.getPhotosName();
        mBookImagesStorage = bookImagesStorage;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        ImageView v = (ImageView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book_imageview, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
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