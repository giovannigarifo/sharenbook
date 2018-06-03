package it.polito.mad.sharenbook.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.LinearSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.mad.sharenbook.App;
import it.polito.mad.sharenbook.ChatActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowBookActivity;
import it.polito.mad.sharenbook.ShowOthersProfile;
import it.polito.mad.sharenbook.WriteReviewActivity;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.model.BorrowRequest;
import it.polito.mad.sharenbook.model.Exchange;
import it.polito.mad.sharenbook.utils.GlideApp;

public class RequestsFragment extends Fragment{

    private RecyclerView giveReqRV, takeReqRV;
    private TextView noTakeReqsTV, noGiveReqsTV, giveReqMore, takeReqMore;
    private String username;

    private DatabaseReference takeRequestsRef, giveRequestsRef, booksRef;

    private RequestsFragment.RVAdapter giveReqsAdapter, takeReqsAdapter;

    private ChildEventListener childEventListener, childEventListener2;

    private BorrowRequest currSelectedRequest;

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
        giveReqsAdapter = new RequestsFragment.RVAdapter(0);
        giveReqRV.setAdapter(giveReqsAdapter);

        childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot requestSnapshot, String s) {

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

                        //giveReqsList.add(req);
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
        takeReqsAdapter = new RequestsFragment.RVAdapter(1);
        takeReqRV.setAdapter(takeReqsAdapter);

        List<BorrowRequest> takeReqsList = new ArrayList<>();

        childEventListener2 = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot requestSnapshot, String s) {

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

                        //giveReqsList.add(req);
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

                        //giveReqsList.add(req);
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



    /**
     * Recycler View Adapter Class
     */
    private class RVAdapter extends RecyclerView.Adapter<RequestsFragment.RVAdapter.ViewHolder> {

        private StorageReference mBookImagesStorage;
        private List<BorrowRequest> requests = new ArrayList<>();
        private int listType;

        class ViewHolder extends RecyclerView.ViewHolder {
            ConstraintLayout mLayout;
            ImageView bookPhoto;
            TextView bookTitle;
            TextView bookDistance;
            ImageView bookOptions;
            TextView tvReviewDone;
            Button btnNotReviewed;
            TextView req_counter, undo_req;

            ViewHolder(ConstraintLayout layout) {
                super(layout);
                mLayout = layout;
                bookPhoto = layout.findViewById(R.id.showcase_rv_book_photo);
                bookTitle = layout.findViewById(R.id.showcase_rv_book_title);
                bookDistance = layout.findViewById(R.id.showcase_rv_book_location);
                bookOptions = layout.findViewById(R.id.showcase_rv_book_options);
                tvReviewDone = layout.findViewById(R.id.exchange_reviewed);
                btnNotReviewed = layout.findViewById(R.id.exchange_not_reviewed);
                req_counter = layout.findViewById(R.id.req_counter);
                undo_req = layout.findViewById(R.id.undo_req_button);

            }
        }

        RVAdapter(int listType) {
            mBookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));
            this.listType = listType;
        }

        @NonNull
        @Override
        public RequestsFragment.RVAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book_showcase_rv, parent, false);

            return new RequestsFragment.RVAdapter.ViewHolder(layout);
        }

        void addRequest(BorrowRequest req){

            requests.add(getItemCount(), req);
            notifyDataSetChanged();
        }

        void updateRequest(BorrowRequest req){
            int pos = -1;
            for(BorrowRequest r : requests){
                if(r.getBookId().equals(req.getBookId())){
                    pos = requests.indexOf(r);
                    break;
                }
            }
            if(pos != -1) {
                requests.remove(pos);
                requests.add(pos, req);
                notifyItemChanged(pos);
            }
        }

        void removeBookId(String bId){
            int pos = -1;
            for(BorrowRequest r : requests){
                if(r.getBookId().equals(bId)){
                    pos = requests.indexOf(r);
                    break;
                }
            }
            if(pos != -1){
                requests.remove(pos);
                notifyItemRemoved(pos);
            }
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@NonNull RequestsFragment.RVAdapter.ViewHolder holder, int position) {
            BorrowRequest req = requests.get(position);
            String fileName = req.getThumbName();
            StorageReference photoRef = mBookImagesStorage.child(req.getBookId()).child(fileName);

            // Load book photo
            GlideApp.with(App.getContext())
                    .load(photoRef)
                    .placeholder(R.drawable.book_cover_portrait)
                    .into(holder.bookPhoto);

            // Set title
            holder.bookTitle.setText(req.getTitle());
            holder.bookDistance.setVisibility(View.GONE);

            //set request counter
            if(listType == 0) {
                holder.req_counter.setText(getString(R.string.borrow_req_counter, req.getRequests()));
                holder.req_counter.setVisibility(View.VISIBLE);

                holder.mLayout.setOnClickListener(v -> {
                    int mapSize = req.getRequestUsers().size();
                    ArrayList<String> requestKeys = new ArrayList<>();
                    long[] requestValues = new long[mapSize];

                    int i = 0;
                    for (Map.Entry r : req.getRequestUsers().entrySet()) {
                        requestKeys.add(i, (String) r.getKey());
                        requestValues[i] = (long)r.getValue();
                        i++;
                    }

                    Bundle bundle = new Bundle();
                    bundle.putStringArrayList("usernameList", requestKeys);
                    bundle.putLongArray("requestTimeArray", requestValues);
                    bundle.putString("bookId", req.getBookId());
                    bundle.putString("bookTitle", req.getTitle());
                    bundle.putString("bookPhoto", req.getThumbName());
                    bundle.putString("bookOwner", req.getOwner());

                    RequestListFragment requestFragment = new RequestListFragment();
                    requestFragment.setArguments(bundle);

                    // Remove old fragment
                    Fragment fragment = fragManager.findFragmentByTag("requestList");
                    if (fragment != null) {
                        fragManager.beginTransaction().remove(fragment).commit();
                        fragManager.popBackStack();
                    }

                    // Add new fragment
                    fragManager.beginTransaction()
                            .replace(R.id.inner_container, requestFragment, "requestList")
                            .addToBackStack(null)
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit();
                });
            } else {
                holder.undo_req.setVisibility(View.VISIBLE);

                holder.mLayout.setOnClickListener(v -> {
                    Intent i = new Intent(App.getContext(), ShowBookActivity.class);
                    i.putExtra("bookId", req.getBookId());
                    App.getContext().startActivity(i);
                });

                holder.undo_req.setOnClickListener(v -> {
                    currSelectedRequest = req;
                    showDialog();
                });

            }

            holder.bookOptions.setVisibility(View.GONE);

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return requests.size();
        }

    }

    public void showDialog() {
        GenericAlertDialog dialog = GenericAlertDialog.newInstance(
                R.string.undo_borrow_book, getString(R.string.undo_borrow_book_msg));

        dialog.show(fragManager, "undo_borrow_dialog");
    }

    public void undoRequest() {

        DatabaseReference usernamesDb = FirebaseDatabase.getInstance().getReference(getString(R.string.usernames_key));

        // Create transaction Map
        HashMap<String, Object> transaction = new HashMap<>();
        transaction.put(username + "/" + getString(R.string.borrow_requests_key) + "/" + currSelectedRequest.getBookId(), null);
        transaction.put(currSelectedRequest.getOwner() + "/" + getString(R.string.pending_requests_key) + "/" + currSelectedRequest.getBookId() + "/" + username, null);

        usernamesDb.updateChildren(transaction, (databaseError, databaseReference) -> {

            if(databaseError == null){
                takeReqsAdapter.removeBookId(currSelectedRequest.getBookId());
                Toast.makeText(App.getContext(), R.string.borrow_request_undone, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(App.getContext(), R.string.borrow_request_undone_fail, Toast.LENGTH_LONG).show();
            }

        });

    }

}
