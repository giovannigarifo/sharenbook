package it.polito.mad.sharenbook.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
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

import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.mad.sharenbook.App;
import it.polito.mad.sharenbook.ChatActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowBookActivity;
import it.polito.mad.sharenbook.ShowMoreActivity;
import it.polito.mad.sharenbook.ShowOthersProfile;
import it.polito.mad.sharenbook.WriteReviewActivity;
import it.polito.mad.sharenbook.adapters.ExchangesAdapter;
import it.polito.mad.sharenbook.model.Exchange;
import it.polito.mad.sharenbook.utils.GlideApp;


public class ExchangesFragment extends Fragment {

    /**
     * FireBase objects
     */
    private DatabaseReference takenBooksRef;
    private DatabaseReference givenBooksRef;
    private DatabaseReference archiveBooksRef;

    public RecyclerView takenBooksRV, givenBooksRV, archiveBooksRV;
    public TextView noTakenTV, noGivenTV, takenMoreTV, givenMoreTV, archiveMoreTV;
    private CardView archiveCV;

    private ExchangesAdapter takenBooksAdapter, givenBooksAdapter, archiveBooksAdapter;

    private String username;

    public static ExchangesFragment newInstance() {
        ExchangesFragment fragment = new ExchangesFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_show_exchanges, container, false);

        setupRecyclerViews(rootView);

        SharedPreferences userData = App.getContext().getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(getString(R.string.username_copy_key), "");

        takenBooksRef = FirebaseDatabase.getInstance().getReference("shared_books").child(username + "/taken_books");
        givenBooksRef = FirebaseDatabase.getInstance().getReference("shared_books").child(username + "/given_books");
        archiveBooksRef = FirebaseDatabase.getInstance().getReference("shared_books").child(username + "/archive_books");

        takenBooksRef.keepSynced(true);
        givenBooksRef.keepSynced(true);
        archiveBooksRef.keepSynced(true);

        noTakenTV = rootView.findViewById(R.id.noTakenBooksMsg);
        noGivenTV = rootView.findViewById(R.id.noGivenBooksMsg);
        takenMoreTV = rootView.findViewById(R.id.takenMoreButton);
        givenMoreTV = rootView.findViewById(R.id.givenMoreButton);
        archiveMoreTV = rootView.findViewById(R.id.archiveMoreButton);

        // Set MORE button listener
        takenMoreTV.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ShowMoreActivity.class);
            i.putExtra("moreType", ShowMoreActivity.TAKEN_BOOKS);
            startActivity(i);
        });

        // Set MORE button listener
        givenMoreTV.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ShowMoreActivity.class);
            i.putExtra("moreType", ShowMoreActivity.GIVEN_BOOKS);
            startActivity(i);
        });

        // Set MORE button listener
        archiveMoreTV.setOnClickListener(v -> {
            Intent i = new Intent(getActivity(), ShowMoreActivity.class);
            i.putExtra("moreType", ShowMoreActivity.ARCHIVE_BOOKS);
            startActivity(i);
        });

        archiveCV = rootView.findViewById(R.id.exchangeArchive);

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
        archiveBooksRV.setLayoutManager(archiveBooksLM);
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
                        takenBooksAdapter = new ExchangesAdapter(takenList, 0, username, getActivity());
                        takenBooksRV.setAdapter(takenBooksAdapter);

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
                        givenBooksAdapter = new ExchangesAdapter(takenList, 1, username, getActivity());
                        givenBooksRV.setAdapter(givenBooksAdapter);
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
                        archiveBooksAdapter = new ExchangesAdapter(archiveList, 2, username, getActivity());
                        archiveBooksRV.setAdapter(archiveBooksAdapter);
                        archiveCV.setVisibility(View.VISIBLE);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Log.d("ERROR", "There was an error while fetching taken books list");
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();

        if(takenBooksAdapter != null)
            takenBooksAdapter.clear();

        if(givenBooksAdapter != null)
            givenBooksAdapter.clear();

        if(archiveBooksAdapter != null)
            archiveBooksAdapter.clear();

        loadTakenBooks();
        loadGivenBooks();
        loadArchiveBooks();
    }
}
