package it.polito.mad.sharenbook.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.App;
import it.polito.mad.sharenbook.ChatActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowBookActivity;
import it.polito.mad.sharenbook.ShowCaseActivity;
import it.polito.mad.sharenbook.ShowOthersProfile;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.model.Exchange;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.Utils;


public class ExchangesFragment extends Fragment {

    /** FireBase objects */
    private DatabaseReference takenBooksRef;
    private DatabaseReference givenBooksRef;

    private RecyclerView takenBooksRV, givenBooksRV, archiveBooksRV;
    private TextView noTakenTV, noGivenTV, takenMoreTV, givenMoreTV;

    private String username;

    public static ExchangesFragment newInstance() {
        ExchangesFragment fragment = new ExchangesFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        View rootView = inflater.inflate(R.layout.fragment_show_exchanges, container, false);

        setupRecyclerViews(rootView);

        SharedPreferences userData = App.getContext().getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(getString(R.string.username_copy_key), "");
        takenBooksRef = FirebaseDatabase.getInstance().getReference("shared_books").child(username + "/taken_books");
        givenBooksRef = FirebaseDatabase.getInstance().getReference("shared_books").child(username + "/given_books");

        noTakenTV = rootView.findViewById(R.id.noTakenBooksMsg);
        noGivenTV = rootView.findViewById(R.id.noGivenBooksMsg);
        takenMoreTV = rootView.findViewById(R.id.takenMoreButton);
        givenMoreTV = rootView.findViewById(R.id.givenMoreButton);

        loadTakenBooks();
        loadGivenBooks();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    private void setupRecyclerViews(View view) {

        // TAKEN BOOKS recycler view
        takenBooksRV = view.findViewById(R.id.takenBooksRV);
        takenBooksRV.setHasFixedSize(true);
        LinearLayoutManager takenBooksLM = new LinearLayoutManager(App.getContext(), LinearLayoutManager.HORIZONTAL, false);
        takenBooksRV.setLayoutManager(takenBooksLM);
        LinearSnapHelper takenLinearSnapHelper = new LinearSnapHelper();
        takenLinearSnapHelper.attachToRecyclerView(takenBooksRV);

        // GIVEN BOOKS recycler view
        givenBooksRV = view.findViewById(R.id.givenBooksRV);
        givenBooksRV.setHasFixedSize(true);
        LinearLayoutManager givenBooksLM = new LinearLayoutManager(App.getContext(), LinearLayoutManager.HORIZONTAL, false);
        givenBooksRV.setLayoutManager(givenBooksLM);
        LinearSnapHelper givenLinearSnapHelper = new LinearSnapHelper();
        givenLinearSnapHelper.attachToRecyclerView(givenBooksRV);

        // ARCHIVE BOOKS recycler view
        archiveBooksRV = view.findViewById(R.id.archiveBooksRV);
        archiveBooksRV.setHasFixedSize(true);
        LinearLayoutManager archiveBooksLM = new LinearLayoutManager(App.getContext(), LinearLayoutManager.HORIZONTAL, false);
        archiveBooksRV.setLayoutManager(archiveBooksLM );
        LinearSnapHelper archiveLinearSnapHelper = new LinearSnapHelper();
        archiveLinearSnapHelper.attachToRecyclerView(archiveBooksRV);

    }

    private void loadTakenBooks() {

        // Load Taken Book RV
        takenBooksRef.orderByChild("creationTime")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        List<Exchange> takenList = new ArrayList<>();

                        if (dataSnapshot.getChildrenCount() == 0) {
                            takenBooksRV.setVisibility(View.GONE);
                            noTakenTV.setVisibility(View.VISIBLE);
                            takenMoreTV.setVisibility(View.INVISIBLE);
                            return;
                        }

                        // Read exchanges
                        for (DataSnapshot exchangeSnapshot : dataSnapshot.getChildren()) {
                            Exchange exchange = exchangeSnapshot.getValue(Exchange.class);
                            exchange.setExchangeId(exchangeSnapshot.getKey());
                            takenList.add(0, exchange);
                        }

                        // Specify an adapter
                        RVAdapter takenBooksAdapter = new RVAdapter(takenList, 0);
                        takenBooksRV.setAdapter(takenBooksAdapter);
                        //findViewById(R.id.showcase_cw_lastbook).setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("ERROR", "There was an error while fetching taken books list");
                    }
                });
    }

    private void loadGivenBooks(){

        // Load Given Book RV
        givenBooksRef.orderByChild("creationTime")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        List<Exchange> takenList = new ArrayList<>();

                        if (dataSnapshot.getChildrenCount() == 0) {
                            givenBooksRV.setVisibility(View.GONE);
                            noGivenTV.setVisibility(View.VISIBLE);
                            givenMoreTV.setVisibility(View.INVISIBLE);
                            return;
                        }

                        // Read exchanges
                        for (DataSnapshot exchangeSnapshot : dataSnapshot.getChildren()) {
                            Exchange exchange = exchangeSnapshot.getValue(Exchange.class);
                            exchange.setExchangeId(exchangeSnapshot.getKey());
                            takenList.add(0, exchange);
                        }

                        // Specify an adapter
                        RVAdapter takenBooksAdapter = new RVAdapter(takenList,1);
                        givenBooksRV.setAdapter(takenBooksAdapter);
                        //findViewById(R.id.showcase_cw_lastbook).setVisibility(View.VISIBLE);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("ERROR", "There was an error while fetching taken books list");
                    }
                });
    }

    /**
     * Recycler View Adapter Class
     */
    private class RVAdapter extends RecyclerView.Adapter<RVAdapter.ViewHolder> {

        private StorageReference mBookImagesStorage;
        private List<Exchange> exchangeList;
        private int listType;

        class ViewHolder extends RecyclerView.ViewHolder {
            ConstraintLayout mLayout;
            ImageView bookPhoto;
            TextView bookTitle;
            TextView bookDistance;
            ImageView bookOptions;

            ViewHolder(ConstraintLayout layout) {
                super(layout);
                mLayout = layout;
                bookPhoto = layout.findViewById(R.id.showcase_rv_book_photo);
                bookTitle = layout.findViewById(R.id.showcase_rv_book_title);
                bookDistance = layout.findViewById(R.id.showcase_rv_book_location);
                bookOptions = layout.findViewById(R.id.showcase_rv_book_options);
            }
        }

        RVAdapter(List<Exchange> exchanges, int listType) {
            mBookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));
            exchangeList = exchanges;
            this.listType = listType;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_book_showcase_rv, parent, false);

            return new ViewHolder(layout);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Exchange exchange = exchangeList.get(position);
            String fileName = exchange.getBookPhoto();
            StorageReference photoRef = mBookImagesStorage.child(exchange.getBookId()).child(fileName);

            // Load book photo
            GlideApp.with(App.getContext())
                    .load(photoRef)
                    .placeholder(R.drawable.book_cover_portrait)
                    .into(holder.bookPhoto);

            // Set title
            holder.bookTitle.setText(exchange.getBookTitle());
            holder.bookDistance.setVisibility(View.GONE);

            // Set listener
            holder.mLayout.setOnClickListener(v -> {
                Intent i = new Intent(App.getContext(), ShowBookActivity.class);
                i.putExtra("bookId", exchange.getBookId());
                App.getContext().startActivity(i);
            });

            // Setup options menu
            holder.bookOptions.setOnClickListener(v -> {

                final PopupMenu popup = new PopupMenu(getContext(), v);
                popup.inflate(R.menu.exchanges_taken_rv_options_menu);
                popup.setOnMenuItemClickListener(item -> {

                    switch (item.getItemId()) {

                        case R.id.contact_owner:
                            Intent chatActivity = new Intent(getContext(), ChatActivity.class);
                            chatActivity.putExtra("recipientUsername", exchange.getCounterpart());
                            getContext().startActivity(chatActivity);
                            return true;

                        case R.id.show_profile:
                            Intent showOwnerProfile = new Intent(getContext(), ShowOthersProfile.class);
                            showOwnerProfile.putExtra("username", exchange.getCounterpart());
                            getContext().startActivity(showOwnerProfile);
                            return true;

                        case R.id.return_book:
                            //TODO return book
                            return true;

                        default:
                            return false;
                    }
                });

                if(listType == 1){
                    popup.getMenu().getItem(2).setVisible(false);
                    String popupItemTitle = getString(R.string.contact_borrower, exchange.getCounterpart());
                    popup.getMenu().getItem(0).setTitle(popupItemTitle);
                }

                popup.show();
            });
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return exchangeList.size();
        }
    }

}
