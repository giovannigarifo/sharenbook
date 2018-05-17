package it.polito.mad.sharenbook;

import android.app.Activity;
import android.content.Intent;
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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.UserInterface;

public class ShowCaseActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, MaterialSearchBar.OnSearchActionListener {

    private String searchState;
    private BottomNavigationView navBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_case);

        // Setup navigation tools
        setupNavigationTools();

        // Get bookDb reference
        DatabaseReference booksDb = FirebaseDatabase.getInstance().getReference(getString(R.string.books_key));
        booksDb.keepSynced(true);

        // Get favoriteBooks reference
        String user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference favoritesDb = FirebaseDatabase.getInstance().getReference(getString(R.string.users_key)).child(user_id).child(getString(R.string.user_favorites_key));
        favoritesDb.keepSynced(true);

        // LAST BOOKS recycler view
        RecyclerView lastBooksRV = findViewById(R.id.showcase_rv_last);
        lastBooksRV.setHasFixedSize(true);
        LinearLayoutManager lastBooksLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        lastBooksRV.setLayoutManager(lastBooksLayoutManager);

        // FAVORITE BOOKS recycler view
        RecyclerView favoriteBooksRV = findViewById(R.id.showcase_rv_favorites);
        favoriteBooksRV.setHasFixedSize(true);
        LinearLayoutManager favoriteBookLM = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        favoriteBooksRV.setLayoutManager(favoriteBookLM);

        // Load Last Book RV
        booksDb.orderByChild("creationTime").limitToLast(15)
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

        // Load Favorite Books RV
        favoritesDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                List<Book> bookList = new ArrayList<>();
                final long bookCount = dataSnapshot.getChildrenCount();

                if (bookCount == 0)
                    return;

                // Read books reference
                for (DataSnapshot bookIdSnapshot : dataSnapshot.getChildren()) {

                    String bookId = bookIdSnapshot.getKey();
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
                        public void onCancelled(DatabaseError databaseError) {}
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.show_case_drawer_layout);
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

        DrawerLayout drawer = findViewById(R.id.show_case_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSearchStateChanged(boolean enabled) {
        searchState = enabled ? "enabled" : "disabled";
        Log.d("debug", "Search " + searchState);

        if (searchState.equals("enabled")) {
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

        startSearchActivity(searchInputText);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("searchState", searchState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        searchState = savedInstanceState.getString("searchState");
    }

    @Override
    public void onButtonClicked(int buttonCode) {
        switch (buttonCode) {
            case MaterialSearchBar.BUTTON_NAVIGATION:
                Log.d("TEST", "Double deck");
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
        MaterialSearchBar searchBar = findViewById(R.id.searchBar);
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

    /**
     * Recycler View Adapter Class
     */
    class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

        private Activity mActivity;
        private StorageReference mBookImagesStorage;
        private List<Book> mBookList;

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public LinearLayout mLayout;
            public ImageView bookPhoto;
            public TextView bookTitle;

            public ViewHolder(LinearLayout layout) {
                super(layout);
                mLayout = layout;
                bookPhoto = layout.findViewById(R.id.showcase_rv_book_photo);
                bookTitle = layout.findViewById(R.id.showcase_rv_book_title);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public MyAdapter(List<Book> bookList) {
            mActivity = ShowCaseActivity.this;
            mBookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));
            mBookList = bookList;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public MyAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            LinearLayout layout = (LinearLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book_showcase_rv, parent, false);

            ViewHolder vh = new ViewHolder(layout);
            return vh;
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(MyAdapter.ViewHolder holder, int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            Book book = mBookList.get(position);
            String fileName = book.getPhotosName().get(0);
            StorageReference photoRef = mBookImagesStorage.child(book.getBookId()).child(fileName);
            Log.d("DEBUG", "filename: " + photoRef.toString());

            // Load book photo
            GlideApp.with(mActivity)
                    .load(photoRef)
                    .placeholder(R.drawable.book_cover_portrait)
                    .into(holder.bookPhoto);

            // Set title
            holder.bookTitle.setText(book.getTitle());

            // Set listener
            holder.mLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent i = new Intent(mActivity, ShowBookActivity.class);
                    i.putExtra("book", book);
                    mActivity.startActivity(i);
                }
            });
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mBookList.size();
        }
    }
}
