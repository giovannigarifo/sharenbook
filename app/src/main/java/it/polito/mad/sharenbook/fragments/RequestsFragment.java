package it.polito.mad.sharenbook.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.polito.mad.sharenbook.App;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowMoreActivity;
import it.polito.mad.sharenbook.adapters.PendingRequestsAdapter;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.model.BorrowRequest;

public class RequestsFragment extends Fragment{

    private RecyclerView giveReqRV, takeReqRV;
    private TextView noTakeReqsTV, noGiveReqsTV, giveReqMore, takeReqMore;
    private String username;

    private DatabaseReference takeRequestsRef, giveRequestsRef, booksRef;

    private PendingRequestsAdapter giveReqsAdapter, takeReqsAdapter;

    private ChildEventListener childEventListener, childEventListener2;

    private FragmentManager fragManager;

    public static RequestsFragment newInstance() {
        RequestsFragment fragment = new RequestsFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_show_requests, container, false);

        setupRecyclerViews(rootView);

        username = getArguments().getString(getString(R.string.username_key));

        giveRequestsRef = FirebaseDatabase.getInstance().getReference("usernames").child(username).child("pendingRequests");
        takeRequestsRef = FirebaseDatabase.getInstance().getReference("usernames").child(username).child("borrowRequests");
        booksRef = FirebaseDatabase.getInstance().getReference(getString(R.string.books_key));

        giveRequestsRef.keepSynced(true);
        takeRequestsRef.keepSynced(true);

        noGiveReqsTV = rootView.findViewById(R.id.noGiveRequestsMsg);
        noTakeReqsTV = rootView.findViewById(R.id.noTakeRequestsMsg);
        giveReqMore = rootView.findViewById(R.id.giveMoreButton);
        takeReqMore = rootView.findViewById(R.id.takeMoreButton);

        // Set MORE button listener
        takeReqMore.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ShowMoreActivity.class);
            i.putExtra("moreType", ShowMoreActivity.TAKE_REQUESTS);
            startActivity(i);
        });

        // Set MORE button listener
        giveReqMore.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ShowMoreActivity.class);
            i.putExtra("moreType", ShowMoreActivity.GIVE_REQUESTS);
            startActivity(i);
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        fragManager = this.getActivity().getSupportFragmentManager();
    }

    @Override
    public void onPause() {
        super.onPause();
        giveRequestsRef.removeEventListener(childEventListener);
        takeRequestsRef.removeEventListener(childEventListener2);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkDataPresence();
        loadGiveRequests();
        loadTakeRequests();
    }

    private void loadGiveRequests() {

        // Specify an adapter
        giveReqsAdapter = new PendingRequestsAdapter(0, fragManager, getActivity());
        giveReqRV.setAdapter(giveReqsAdapter);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot requestSnapshot, String s) {

                if (giveReqsAdapter.getItemCount() == 0) {
                    giveReqRV.setVisibility(View.VISIBLE);
                    noGiveReqsTV.setVisibility(View.GONE);
                    giveReqMore.setVisibility(View.VISIBLE);
                }

                int numRequests = (int) requestSnapshot.getChildrenCount();
                String bookId = requestSnapshot.getKey();
                HashMap<String, Long> reqUsers = new HashMap<>();

                // Read requests
                for (DataSnapshot user : requestSnapshot.getChildren()) {
                    reqUsers.put(user.getKey(), (Long) user.getValue());
                }

                booksRef.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Book book = dataSnapshot.getValue(Book.class);
                        BorrowRequest req = new BorrowRequest(reqUsers, bookId, book.getTitle(), book.getAuthorsAsString(), book.getCreationTimeAsString(getContext()), numRequests, book.getPhotosName().get(0), book.getOwner_username(), book.isShared());

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

                booksRef.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Book book = dataSnapshot.getValue(Book.class);
                        BorrowRequest req = new BorrowRequest(reqUsers, bookId, book.getTitle(), book.getAuthorsAsString(), book.getCreationTimeAsString(getContext()), numRequests, book.getPhotosName().get(0), book.getOwner_username(), book.isShared());

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
                    giveReqRV.setVisibility(View.GONE);
                    noGiveReqsTV.setVisibility(View.VISIBLE);
                    giveReqMore.setVisibility(View.INVISIBLE);
                }

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        giveRequestsRef.addChildEventListener(childEventListener);


    }

    private void loadTakeRequests() {

        // Specify an adapter
        takeReqsAdapter = new PendingRequestsAdapter(1, fragManager, getActivity());
        takeReqRV.setAdapter(takeReqsAdapter);

        childEventListener2 = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot requestSnapshot, String s) {

                if (takeReqsAdapter.getItemCount() == 0) {
                    takeReqRV.setVisibility(View.VISIBLE);
                    noTakeReqsTV.setVisibility(View.GONE);
                    takeReqMore.setVisibility(View.VISIBLE);
                }

                int numRequests = (int) requestSnapshot.getChildrenCount();
                String bookId = requestSnapshot.getKey();
                HashMap<String, Long> reqUsers = new HashMap<>();

                // Read requests
                for (DataSnapshot user : requestSnapshot.getChildren()) {
                    reqUsers.put(user.getKey(), (Long) user.getValue());
                }

                booksRef.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Book book = dataSnapshot.getValue(Book.class);
                        BorrowRequest req = new BorrowRequest(reqUsers, bookId, book.getTitle(), book.getAuthorsAsString(), book.getCreationTimeAsString(getContext()), numRequests, book.getPhotosName().get(0), book.getOwner_username(), book.isShared());

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

                booksRef.child(bookId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        Book book = dataSnapshot.getValue(Book.class);
                        BorrowRequest req = new BorrowRequest(reqUsers, bookId, book.getTitle(), book.getAuthorsAsString(), book.getCreationTimeAsString(getContext()), numRequests, book.getPhotosName().get(0), book.getOwner_username(), book.isShared());

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
                    takeReqRV.setVisibility(View.GONE);
                    noTakeReqsTV.setVisibility(View.VISIBLE);
                    takeReqMore.setVisibility(View.INVISIBLE);
                }

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        takeRequestsRef.addChildEventListener(childEventListener2);

    }

    public void checkDataPresence() {

        giveRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.getChildrenCount() == 0) {
                    giveReqRV.setVisibility(View.GONE);
                    noGiveReqsTV.setVisibility(View.VISIBLE);
                    giveReqMore.setVisibility(View.INVISIBLE);
                } else {
                    giveReqRV.setVisibility(View.VISIBLE);
                    noGiveReqsTV.setVisibility(View.GONE);
                    giveReqMore.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("ERROR", "There was an error while fetching give requests!");
            }
        });

        takeRequestsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                if (dataSnapshot.getChildrenCount() == 0) {
                    takeReqRV.setVisibility(View.GONE);
                    noTakeReqsTV.setVisibility(View.VISIBLE);
                    takeReqMore.setVisibility(View.INVISIBLE);
                } else {
                    takeReqRV.setVisibility(View.VISIBLE);
                    noTakeReqsTV.setVisibility(View.GONE);
                    takeReqMore.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("ERROR", "There was an error while fetching take requests!");
            }
        });

    }


    private void setupRecyclerViews(View view) {

        // give requests
        giveReqRV = view.findViewById(R.id.giveBooksRV);
        giveReqRV.setHasFixedSize(true);
        LinearLayoutManager giveReqLM = new LinearLayoutManager(App.getContext(), LinearLayoutManager.HORIZONTAL, false);
        giveReqRV.setLayoutManager(giveReqLM);
        LinearSnapHelper takenLinearSnapHelper = new LinearSnapHelper();
        takenLinearSnapHelper.attachToRecyclerView(giveReqRV);

        // take requests
        takeReqRV = view.findViewById(R.id.takeBooksRV);
        takeReqRV.setHasFixedSize(true);
        LinearLayoutManager takeReqLM = new LinearLayoutManager(App.getContext(), LinearLayoutManager.HORIZONTAL, false);
        takeReqRV.setLayoutManager(takeReqLM);
        LinearSnapHelper givenLinearSnapHelper = new LinearSnapHelper();
        givenLinearSnapHelper.attachToRecyclerView(takeReqRV);

    }

}
