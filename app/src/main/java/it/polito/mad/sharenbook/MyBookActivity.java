package it.polito.mad.sharenbook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;


import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.MyBooksUtils;

public class MyBookActivity extends AppCompatActivity {



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



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_book);


        findAndSetNewAnnouncementFab();
        setRecyclerView(books);

        // Setup FireBase
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        userBooksDb = firebaseDatabase.getReference(getString(R.string.users_key)).child(firebaseUser.getUid()).child(getString(R.string.user_books_key));




        userBooksDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
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
                                finish();
                            }
                    );

                    no_books.show();
                }else{ /** there are announcements */


                    bookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));
                    booksDb = firebaseDatabase.getReference(getString(R.string.books_key));

                    if(books.isEmpty()){
                        Log.d("debug", "I am in the listener READ FROM DB");
                        Iterable<DataSnapshot> announces = dataSnapshot.getChildren();

                        final long numberOfannounces = dataSnapshot.getChildrenCount();

                        Log.d("debug","->"+numberOfannounces);

                        for(DataSnapshot announce :announces){

                            booksDb.child((String)announce.getValue()).orderByChild("creationTime").addListenerForSingleValueEvent(new ValueEventListener() {
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
                                                Log.d("debug",books.size()+"->"+numberOfannounces);
                                                MyBooksUtils.setMyBooks(books);

                                                adapter = new MyAnnounceRVAdapter(books,MyBookActivity.this,llm);
                                                rv.setAdapter(adapter);

                                                Log.d("debug", "I am in the listener READ FROM DB: read");

                                            }
                                        }
                                    });




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
        Toolbar sbaToolbar = findViewById(R.id.sba_toolbar);
        setSupportActionBar(sbaToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.mba_title);




        // Setup navbar
        setupNavbar();
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
                finish();

            }
        });
    }

    private void setRecyclerView(List<Book> announcments){
        rv = (RecyclerView)findViewById(R.id.expanded_books);
        llm = new LinearLayoutManager(MyBookActivity.this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        rv.setLayoutManager(llm);
        rv.setItemAnimator(new DefaultItemAnimator());


        adapter = new MyAnnounceRVAdapter(announcments,MyBookActivity.this,llm);
        rv.setAdapter(adapter);


    }



    private void setupNavbar() {
        navBar = findViewById(R.id.navigation);


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

                    break;
            }

            return true;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Terminate activity (actionbar left arrow pressed)
        finish();
        return true;
    }
}
