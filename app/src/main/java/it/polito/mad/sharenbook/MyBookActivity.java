package it.polito.mad.sharenbook;

import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
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

public class MyBookActivity extends AppCompatActivity {



    private BottomNavigationView navBar;



    /** FireBase objects */
    private FirebaseUser firebaseUser;
    private DatabaseReference booksDb;
    private DatabaseReference userBooksDb;
    private StorageReference bookImagesStorage;
    private ArrayList<Book> books;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_book);




        /** teeeeest **/
        books = new ArrayList<Book>();

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

                    Iterable<DataSnapshot> announces = dataSnapshot.getChildren();

                    final long numberOfannounces = dataSnapshot.getChildrenCount();

                    for(DataSnapshot announce :announces){

                        booksDb.child((String)announce.getValue()).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                //books.add(dataSnapshot.getValue(Book.class));

                                bookImagesStorage.child("/"+announce.getValue()+"/0.jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        Book book = dataSnapshot.getValue(Book.class);
                                        book.setThumbnail(uri.toString());
                                        books.add(book);

                                        if(books.size() == numberOfannounces){

                                            setRecycle(books);

                                        }
                                    }
                                });


    /*
                                if(books.size() == numberOfannounces){

                                    setRecycle(books);

                                }

*/

                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {

                            }
                        });
                    }


                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

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

    private void setRecycle(List<Book> announcments){
        RecyclerView rv = (RecyclerView)findViewById(R.id.expanded_books);
        LinearLayoutManager llm = new LinearLayoutManager(MyBookActivity.this);
        rv.setLayoutManager(llm);

        RVAdapter adapter = new RVAdapter(announcments,getApplicationContext());
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
