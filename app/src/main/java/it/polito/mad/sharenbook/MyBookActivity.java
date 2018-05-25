package it.polito.mad.sharenbook;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.MyBooksUtils;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.UserInterface;

public class MyBookActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private BottomNavigationView navBar;

    /** FireBase objects */
    private FirebaseUser firebaseUser;
    private DatabaseReference booksDb;
    private DatabaseReference userBooksDb;
    private StorageReference bookImagesStorage;
    private ArrayList<Book> books = new ArrayList<Book>();

    /** new announcement button */
    private FloatingActionButton newAnnoucementFab;
    private MyAnnounceRVAdapter adapter;
    //private Book newAnnounce; future modifications
    private LinearLayoutManager llm;
    //private ValueEventListener valueEventListener;
    private RecyclerView rv;
    private NavigationView navigationView;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_book);


        setupNavigationTools();
        findAndSetNewAnnouncementFab();
        setRecyclerView(books);

        // Setup FireBase
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        bookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));
        booksDb = firebaseDatabase.getReference(getString(R.string.books_key));
        userBooksDb = firebaseDatabase.getReference(getString(R.string.users_key)).child(firebaseUser.getUid()).child(getString(R.string.user_books_key));




        userBooksDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue()!=null)
                if (dataSnapshot.getValue().equals(getString(R.string.users_books_placeholder))){ /** no announcemnts */
                    AlertDialog.Builder no_books = new AlertDialog.Builder(MyBookActivity.this); //give a context to Dialog
                    no_books.setTitle(R.string.no_books_alert_title);
                    no_books.setMessage(R.string.no_books_suggestion);
                    no_books.setPositiveButton(android.R.string.ok, (dialog, which) -> {

                                Intent i = new Intent (getApplicationContext(), ShareBookActivity.class);
                                startActivity(i);
                                finish();

                            }
                    ).setNegativeButton(android.R.string.cancel,
                            (dialog, which) -> {
                                dialog.dismiss();

                            }
                    );

                    no_books.show();
                }else{ /** there are announcements */

                    if(books.isEmpty()){
                        Log.d("debug", "I am in the listener READ FROM DB");
                        Iterable<DataSnapshot> announces = dataSnapshot.getChildren();

                        final long numberOfannounces = dataSnapshot.getChildrenCount();

                        Log.d("debug","->"+numberOfannounces);

                        for(DataSnapshot announce :announces){

                            booksDb.child((String)announce.getValue()).orderByChild("creationTime").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(DataSnapshot dataSnapshot) {

                                    Book book = dataSnapshot.getValue(Book.class);
                                    book.setBookId((String)announce.getValue());
                                    books.add(book);

                                    if(books.size() == numberOfannounces){
                                        Log.d("debug",books.size()+"->"+numberOfannounces);
                                        MyBooksUtils.setMyBooks(books);

                                        adapter = new MyAnnounceRVAdapter(books, MyBookActivity.this, llm, bookImagesStorage);
                                        rv.setAdapter(adapter);

                                        Log.d("debug", "I am in the listener READ FROM DB: read");

                                    }
                                }

                                @Override
                                public void onCancelled(DatabaseError databaseError) {

                                    Toast.makeText(MyBookActivity.this,getString(R.string.databaseError),Toast.LENGTH_SHORT);

                                }
                            });
                        }

                    }

                    /* future implementation
                    Intent i = getIntent();

                    if (i != null && adapter != null) {
                        Log.d("NEWHHHHHHHH", "I am in the listener");
                        newAnnounce = i.getExtras().getParcelable("newAnnounce");

                        if (newAnnounce != null && books.isEmpty()) {//returning from a single addition
                            Log.d("NEWHHHHHHHH", "I am in listener after new announce");
                            Log.d("NEWHHHHHHHH", "I am in listener after new announce : "+books.size());
                            bookImagesStorage.child("/" + newAnnounce.getBookId() + "/0.jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {

                                    books = MyBooksUtils.getMyBooks(); // take user's books
                                    Book modifiedAnnounce = MyAnnounceRVAdapter.getUnderModification();
                                    if(modifiedAnnounce != null) {
                                        books.remove(MyAnnounceRVAdapter.getPositionUnderModificaiton());
                                        Log.d("NEWHHHHHHHH", "I am in listener after announce modified");
                                    }
                                    adapter = new MyAnnounceRVAdapter(books,MyBookActivity.this,llm);
                                    newAnnounce.setThumbnail(uri.toString());
                                    rv.setAdapter(adapter);
                                    adapter.addItem(0,newAnnounce);
                                    MyBooksUtils.setMyBooks((ArrayList<Book>)MyAnnounceRVAdapter.getAnnouncementsModel());


                                    Log.d("NEWHHHHHHHH", "added");
                                    Log.d("NEWHHHH","model"+MyAnnounceRVAdapter.getAnnouncementsModel().size());

                                }
                            });


                        }else
                            if(books.isEmpty()){ //activity called by other activities
                            Log.d("NEWHHHHHHHH", "I am in the listener READ FROM DB");
                            Iterable<DataSnapshot> announces = dataSnapshot.getChildren();

                            final long numberOfannounces = dataSnapshot.getChildrenCount();

                            Log.d("NEWHHH","->"+numberOfannounces);

                            for(DataSnapshot announce :announces){

                                booksDb.child((String)announce.getValue()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        //books.add(dataSnapshot.getValue(Book.class));

                                        bookImagesStorage.child("/"+announce.getValue()+"/0.jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                            @Override
                                            public void onSuccess(Uri uri) {
                                                Book book = dataSnapshot.getValue(Book.class);
                                                book.setBookId((String)announce.getValue());
                                                book.setThumbnail(uri.toString());
                                                books.add(book);


                                                if(books.size() == numberOfannounces){
                                                    Log.d("NEWHHH",books.size()+"->"+numberOfannounces);
                                                    MyBooksUtils.setMyBooks(books);

                                                    adapter = new MyAnnounceRVAdapter(books,MyBookActivity.this,llm);
                                                    rv.setAdapter(adapter);

                                                    Log.d("NEWHHHHHHHH", "I am in the listener READ FROM DB: read");

                                                }
                                            }
                                        });




                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                    }
                                });
                            }

                        }
                    }
                */



                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(MyBookActivity.this,getString(R.string.databaseError),Toast.LENGTH_SHORT);

            }
        });


        // Setup toolbar
        /*
        Toolbar sbaToolbar = findViewById(R.id.sba_toolbar);
        setSupportActionBar(sbaToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.mba_title);




        // Setup navbar
        setupNavbar();
        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        navBar.setSelectedItemId(R.id.navigation_myBook);
    }

    private void setupNavigationTools() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.sba_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.mba_title);
        }

        // Setup navigation drawer
        DrawerLayout drawer = findViewById(R.id.my_book_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.my_book_nav_view);
        navigationView.setNavigationItemSelectedListener(MyBookActivity.this);
        navigationView.setCheckedItem(R.id.drawer_navigation_myBook);

        // Update drawer with user info
        View nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        CircularImageView drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        TextView drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        TextView drawer_email = nav.findViewById(R.id.drawer_user_email);

        NavigationDrawerManager.setDrawerViews(getApplicationContext(), getWindowManager(), drawer_fullname,
                drawer_email, drawer_userPicture, NavigationDrawerManager.getNavigationDrawerProfile());

        // Setup bottom navbar
        UserInterface.setupNavigationBar(this, R.id.navigation_myBook);
        navBar = findViewById(R.id.navigation);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("books",books);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("debug","I am in onRestoreInstanceState");
        books = savedInstanceState.getParcelableArrayList("books");
        setRecyclerView(books);
    }

    private void findAndSetNewAnnouncementFab(){
        newAnnoucementFab = findViewById(R.id.fab_addBook);

        newAnnoucementFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),ShareBookActivity.class);
                startActivity(i);
            }
        });
    }

    private void setRecyclerView(List<Book> announcements){
        rv = (RecyclerView)findViewById(R.id.expanded_books);
        llm = new LinearLayoutManager(MyBookActivity.this, LinearLayoutManager.VERTICAL, false);
        rv.setLayoutManager(llm);
        rv.setItemAnimator(new DefaultItemAnimator());

        if (!announcements.isEmpty()) {
            adapter = new MyAnnounceRVAdapter(announcements, MyBookActivity.this, llm, bookImagesStorage);
            rv.setAdapter(adapter);
        }
    }


    /**
     * Navigation Drawer Listeners
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(id == R.id.drawer_navigation_profile){
            Intent i = new Intent(getApplicationContext(), TabbedShowProfileActivity.class);
            i.putExtra(getString(R.string.user_profile_data_key), NavigationDrawerManager.getUserParcelable(getApplicationContext()));
            startActivity(i);

        } else if (id == R.id.drawer_navigation_shareBook) {
            Intent i = new Intent(getApplicationContext(), ShareBookActivity.class);
            startActivity(i);

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

        DrawerLayout drawer = findViewById(R.id.my_book_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.my_book_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
        navigationView.setCheckedItem(R.id.drawer_navigation_myBook);
    }
}
