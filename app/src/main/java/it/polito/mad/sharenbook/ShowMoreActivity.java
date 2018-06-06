package it.polito.mad.sharenbook;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import it.polito.mad.sharenbook.adapters.ExchangesAdapter;
import it.polito.mad.sharenbook.adapters.PendingRequestsAdapter;
import it.polito.mad.sharenbook.adapters.ShowBooksAdapter;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.model.BorrowRequest;
import it.polito.mad.sharenbook.model.Exchange;

public class ShowMoreActivity extends AppCompatActivity {

    public static final int LAST_BOOKS = 0;
    public static final int FAVORITES_BOOKS = 1;
    public static final int CLOSE_BOOKS = 2;
    public static final int GIVE_REQUESTS = 3;
    public static final int TAKE_REQUESTS = 4;
    public static final int GIVEN_BOOKS = 5;
    public static final int TAKEN_BOOKS = 6;
    public static final int ARCHIVE_BOOKS = 7;

    private int moreType;

    private Location mLocation;

    private DatabaseReference booksDb;
    private DatabaseReference favoritesDb;
    private DatabaseReference borrowRequestsDb;
    private DatabaseReference giveRequestsRef, takeRequestsRef, takenBooksRef, givenBooksRef, archiveBooksRef;

    private ValueEventListener requestedBooksListener, favoriteBooksListener;
    private ChildEventListener giveRequestsListener, takeRequestsListener, takenBooksListener, booksListener;

    private PendingRequestsAdapter giveReqsAdapter, takeReqsAdapter;

    private String user_id, username;
    private LinkedHashSet<String> favoritesBookIdList;
    private HashSet<String> requestedBookIdList;

    private RecyclerView moreBooksRV;
    private ExchangesAdapter takenBooksAdapter;
    private ShowBooksAdapter booksAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_more);

        // Get bundle info
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            moreType = bundle.getInt("moreType", 0);
            double latitude = bundle.getDouble("latitude", 200);
            double longitude = bundle.getDouble("longitude", 200);

            if (latitude != 200 && longitude != 200) {
                mLocation = new Location("");
                mLocation.setLatitude(latitude);
                mLocation.setLongitude(longitude);
            }

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
        giveRequestsRef = db.getReference("usernames").child(username).child("pendingRequests");
        takeRequestsRef = db.getReference("usernames").child(username).child("borrowRequests");
        takenBooksRef = db.getReference("shared_books").child(username + "/taken_books");
        givenBooksRef = db.getReference("shared_books").child(username + "/given_books");
        archiveBooksRef = db.getReference("shared_books").child(username + "/archive_books");

        // Create requested and favorite books listeners
        createListeners();

        // Setup recyclerview
        moreBooksRV = findViewById(R.id.showcase_rv_more);
        moreBooksRV.setHasFixedSize(true);
        GridLayoutManager moreBooksLM = new GridLayoutManager(this, getGridColumnCount());
        moreBooksRV.setLayoutManager(moreBooksLM);

        // Load books
        loadBooks();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(moreType < GIVE_REQUESTS) {
            borrowRequestsDb.removeEventListener(requestedBooksListener);
            favoritesDb.removeEventListener(favoriteBooksListener);
            booksDb.removeEventListener(booksListener);
        } else if (moreType == GIVE_REQUESTS) {
            giveRequestsRef.removeEventListener(giveRequestsListener);
        } else if (moreType == TAKE_REQUESTS) {
            takeRequestsRef.removeEventListener(takeRequestsListener);
        } else if (moreType == TAKEN_BOOKS) {
            takenBooksRef.removeEventListener(takenBooksListener);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(moreType < GIVE_REQUESTS) {
            borrowRequestsDb.addValueEventListener(requestedBooksListener);
            favoritesDb.orderByValue().addValueEventListener(favoriteBooksListener);
            booksDb.addChildEventListener(booksListener);
        } else if (moreType == GIVE_REQUESTS) {
            giveReqsAdapter.clear();
            giveRequestsRef.addChildEventListener(giveRequestsListener);
        } else if (moreType == TAKE_REQUESTS) {
            takeReqsAdapter.clear();
            takeRequestsRef.addChildEventListener(takeRequestsListener);
        } else if (moreType == TAKEN_BOOKS){
            takenBooksRef.addChildEventListener(takenBooksListener);
        }
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }

    private Activity getActivityContext() {
        return this;
    }

    private String getToolbarTitle() {

        switch (moreType) {
            case LAST_BOOKS:
                return getString(R.string.showcase_lastbook);
            case FAVORITES_BOOKS:
                return getString(R.string.showcase_favorites);
            case CLOSE_BOOKS:
                return getString(R.string.showcase_closebooks);
            case GIVE_REQUESTS:
                return getString(R.string.request_give);
            case TAKE_REQUESTS:
                return getString(R.string.request_take);
            case GIVEN_BOOKS:
                return getString(R.string.exchanges_given);
            case TAKEN_BOOKS:
                return getString(R.string.exchanges_taken);
            case ARCHIVE_BOOKS:
                return getString(R.string.exchanges_archive);
            default:
                return "";
        }
    }

    private void createListeners() {

        requestedBookIdList = new HashSet<>();
        favoritesBookIdList = new LinkedHashSet<>();

        //Create takenBooks listener
        takenBooksListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Exchange ex = dataSnapshot.getValue(Exchange.class);
                ex.setExchangeId(dataSnapshot.getKey());

                if(takenBooksAdapter != null){
                    takenBooksAdapter.remove(ex);

                    if(takenBooksAdapter.getItemCount() == 0){
                        finish();
                    }
                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

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

        // Create child changed books listener
        booksListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

                Book updatedBook = dataSnapshot.getValue(Book.class);
                updatedBook.setBookId(dataSnapshot.getKey());

                if (booksAdapter != null)
                    booksAdapter.updateItem(updatedBook);
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
    }

    private int getGridColumnCount() {

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        double wInches = (double) metrics.widthPixels / metrics.densityDpi;

        if (wInches <= 3.0) return 3;
        else if (wInches <= 3.8) return 4;
        else if (wInches <= 4.6) return 5;
        else if (wInches <= 5.4) return 6;
        else if (wInches <= 6.2) return 7;
        else if (wInches <= 7.0) return 8;
        else return 9;
    }

    private void loadBooks() {

        if (moreType == LAST_BOOKS) {
            loadLastBooks();
        } else if (moreType == CLOSE_BOOKS) {

            if (mLocation != null)
                loadCloseBooks();
            else finish();

        } else if (moreType == GIVE_REQUESTS) {
            loadGiveRequests();
        } else if (moreType == TAKE_REQUESTS) {
            loadTakeRequests();
        } else if (moreType == GIVEN_BOOKS) {
            loadGivenBooks();
        } else if (moreType == TAKEN_BOOKS) {
            loadTakenBooks();
        } else if (moreType == ARCHIVE_BOOKS) {
            loadArchiveBooks();
        }
    }

    private void loadGiveRequests() {

        // Specify an adapter
        giveReqsAdapter = new PendingRequestsAdapter(0, getSupportFragmentManager(), this);
        moreBooksRV.setAdapter(giveReqsAdapter);

        giveRequestsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot requestSnapshot, String s) {

                int numRequests = (int) requestSnapshot.getChildrenCount();
                String bookId = requestSnapshot.getKey();
                HashMap<String, Long> reqUsers = new HashMap<>();

                // Read requests
                for (DataSnapshot user : requestSnapshot.getChildren()) {
                    reqUsers.put(user.getKey(), (Long) user.getValue());
                }

                booksDb.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Book book = dataSnapshot.getValue(Book.class);
                        BorrowRequest req = new BorrowRequest(reqUsers, bookId, book.getTitle(), book.getAuthorsAsString(), book.getCreationTimeAsString(getActivityContext()), numRequests, book.getPhotosName().get(0), book.getOwner_username(), book.isShared());

                        giveReqsAdapter.addRequest(req);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(App.getContext(), getString(R.string.databaseError), Toast.LENGTH_SHORT).show();
                    }

                });

            }

            @Override
            public void onChildChanged(DataSnapshot requestSnapshot, String s) {

                int numRequests = (int) requestSnapshot.getChildrenCount();
                String bookId = requestSnapshot.getKey();
                HashMap<String, Long> reqUsers = new HashMap<>();

                // Read requests
                for (DataSnapshot user : requestSnapshot.getChildren()) {
                    reqUsers.put(user.getKey(), (Long) user.getValue());
                }

                booksDb.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Book book = dataSnapshot.getValue(Book.class);
                        BorrowRequest req = new BorrowRequest(reqUsers, bookId, book.getTitle(), book.getAuthorsAsString(), book.getCreationTimeAsString(getActivityContext()), numRequests, book.getPhotosName().get(0), book.getOwner_username(), book.isShared());

                        giveReqsAdapter.updateRequest(req);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(App.getContext(), getString(R.string.databaseError), Toast.LENGTH_SHORT).show();
                    }

                });

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String bookId = dataSnapshot.getKey();
                giveReqsAdapter.removeBookId(bookId);

                if (giveReqsAdapter.getItemCount() == 0) {
                    Toast.makeText(App.getContext(), getString(R.string.no_requests), Toast.LENGTH_SHORT).show();
                    finish();
                }

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

    }


    private void loadTakeRequests() {

        // Specify an adapter
        takeReqsAdapter = new PendingRequestsAdapter(1, getSupportFragmentManager(), this);
        moreBooksRV.setAdapter(takeReqsAdapter);

        takeRequestsListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot requestSnapshot, String s) {

                int numRequests = (int) requestSnapshot.getChildrenCount();
                String bookId = requestSnapshot.getKey();
                HashMap<String, Long> reqUsers = new HashMap<>();

                // Read requests
                for (DataSnapshot user : requestSnapshot.getChildren()) {
                    reqUsers.put(user.getKey(), (Long) user.getValue());
                }

                booksDb.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Book book = dataSnapshot.getValue(Book.class);
                        BorrowRequest req = new BorrowRequest(reqUsers, bookId, book.getTitle(), book.getAuthorsAsString(), book.getCreationTimeAsString(getActivityContext()), numRequests, book.getPhotosName().get(0), book.getOwner_username(), book.isShared());

                        takeReqsAdapter.addRequest(req);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(App.getContext(), getString(R.string.databaseError), Toast.LENGTH_SHORT).show();
                    }

                });

            }

            @Override
            public void onChildChanged(DataSnapshot requestSnapshot, String s) {

                int numRequests = (int) requestSnapshot.getChildrenCount();
                String bookId = requestSnapshot.getKey();
                HashMap<String, Long> reqUsers = new HashMap<>();

                // Read requests
                for (DataSnapshot user : requestSnapshot.getChildren()) {
                    reqUsers.put(user.getKey(), (Long) user.getValue());
                }

                booksDb.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Book book = dataSnapshot.getValue(Book.class);
                        BorrowRequest req = new BorrowRequest(reqUsers, bookId, book.getTitle(), book.getAuthorsAsString(), book.getCreationTimeAsString(getActivityContext()), numRequests, book.getPhotosName().get(0), book.getOwner_username(), book.isShared());

                        takeReqsAdapter.updateRequest(req);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(App.getContext(), getString(R.string.databaseError), Toast.LENGTH_SHORT).show();
                    }

                });

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                String bookId = dataSnapshot.getKey();
                takeReqsAdapter.removeBookId(bookId);

                if (takeReqsAdapter.getItemCount() == 0) {
                    Toast.makeText(App.getContext(), getString(R.string.no_requests), Toast.LENGTH_SHORT).show();
                    finish();
                }

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

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
                booksAdapter = new ShowBooksAdapter(getActivityContext(), bookList, mLocation, favoritesBookIdList, requestedBookIdList);
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
                        booksAdapter = new ShowBooksAdapter(getActivityContext(), bookList, mLocation, favoritesBookIdList, requestedBookIdList);
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
                        booksAdapter = new ShowBooksAdapter(getActivityContext(), bookList, mLocation, favoritesBookIdList, requestedBookIdList);
                        moreBooksRV.setAdapter(booksAdapter);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }
    }

    private void loadTakenBooks() {

        // Load Taken Book RV
        takenBooksRef.orderByChild("creationTime")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        List<Exchange> takenList = new ArrayList<>();

                        // Read exchanges
                        for (DataSnapshot exchangeSnapshot : dataSnapshot.getChildren()) {
                            Exchange exchange = exchangeSnapshot.getValue(Exchange.class);
                            exchange.setExchangeId(exchangeSnapshot.getKey());
                            takenList.add(0, exchange);
                        }

                        // Specify an adapter
                        takenBooksAdapter = new ExchangesAdapter(takenList, 0, username, getActivityContext());
                        moreBooksRV.setAdapter(takenBooksAdapter);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("ERROR", "There was an error while fetching taken books list");
                    }
                });
    }

    private void loadGivenBooks() {

        // Load Given Book RV
        givenBooksRef.orderByChild("creationTime")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        List<Exchange> takenList = new ArrayList<>();

                        // Read exchanges
                        for (DataSnapshot exchangeSnapshot : dataSnapshot.getChildren()) {
                            Exchange exchange = exchangeSnapshot.getValue(Exchange.class);
                            exchange.setExchangeId(exchangeSnapshot.getKey());
                            takenList.add(0, exchange);
                        }

                        // Specify an adapter
                        ExchangesAdapter givenBooksAdapter = new ExchangesAdapter(takenList, 1, username, getActivityContext());
                        moreBooksRV.setAdapter(givenBooksAdapter);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("ERROR", "There was an error while fetching taken books list");
                    }
                });
    }

    private void loadArchiveBooks() {

        // Load Archive Book RV
        archiveBooksRef.orderByChild("creationTime")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        List<Exchange> archiveList = new ArrayList<>();

                        if (dataSnapshot.getChildrenCount() == 0)
                            return;

                        // Read exchanges
                        for (DataSnapshot exchangeSnapshot : dataSnapshot.getChildren()) {
                            Exchange exchange = exchangeSnapshot.getValue(Exchange.class);
                            exchange.setExchangeId(exchangeSnapshot.getKey());
                            archiveList.add(0, exchange);
                        }

                        // Specify an adapter
                        ExchangesAdapter archiveBooksAdapter = new ExchangesAdapter(archiveList, 2, username, getActivityContext());
                        moreBooksRV.setAdapter(archiveBooksAdapter);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("ERROR", "There was an error while fetching taken books list");
                    }
                });
    }

}
