package it.polito.mad.sharenbook.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.polito.mad.sharenbook.App;
import it.polito.mad.sharenbook.adapters.AnnouncementAdapter;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShareBookActivity;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.Utils;


public class ShowMyAnnouncementsFragment extends Fragment {

    /** FireBase objects */
    private FirebaseUser firebaseUser;
    private DatabaseReference booksDb;
    private DatabaseReference userBooksDb;

    private AnnouncementAdapter adapter;
    private LinearLayoutManager llm;
    private RecyclerView rv;
    private FloatingActionButton newAnnoucementFab;
    private CardView no_book_cv;

    private ArrayList<Book> books = new ArrayList<>();

    public static ShowMyAnnouncementsFragment newInstance() {
        ShowMyAnnouncementsFragment fragment = new ShowMyAnnouncementsFragment();

        //Bundle args = new Bundle();
        //args.putInt("someInt", someInt);
        //fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        View rootView = inflater.inflate(R.layout.fragment_show_announcements, container, false);
        rv = rootView.findViewById(R.id.expanded_books);
        newAnnoucementFab = rootView.findViewById(R.id.fab_addBook);
        no_book_cv = rootView.findViewById(R.id.card_no_books);

        SetNewAnnouncementFab();
        setRecyclerView();

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        adapter.clearAnnouncements();
        loadAnnouncements();
    }

    private void setRecyclerView(){

        llm = new LinearLayoutManager(App.getContext(), LinearLayoutManager.VERTICAL, false);
        rv.setLayoutManager(llm);
        rv.setItemAnimator(new DefaultItemAnimator());

        adapter = new AnnouncementAdapter(App.getContext(), llm);
        rv.setAdapter(adapter);

    }

    private void SetNewAnnouncementFab(){

        newAnnoucementFab.setOnClickListener(v -> {
            Intent i = new Intent(getContext(), ShareBookActivity.class);
            startActivity(i);
        });
    }

    private void loadAnnouncements(){

        // Setup FireBase
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        booksDb = firebaseDatabase.getReference(getString(R.string.books_key));
        userBooksDb = firebaseDatabase.getReference(getString(R.string.users_key)).child(firebaseUser.getUid()).child(getString(R.string.user_books_key));

        userBooksDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.getValue()!=null)

                    if (dataSnapshot.getValue().equals(getString(R.string.users_books_placeholder))){ /** no announcemnts */

                        no_book_cv.setVisibility(View.VISIBLE);
                        rv.setVisibility(View.GONE);

                    }else{ /** there are announcements */

                        if(books.isEmpty()){

                            no_book_cv.setVisibility(View.GONE);
                            rv.setVisibility(View.VISIBLE);

                            Iterable<DataSnapshot> announces = dataSnapshot.getChildren();
                            List<DataSnapshot> announcesReverse = Utils.toReverseList(announces);

                            for(DataSnapshot announce : announcesReverse){

                                booksDb.child((String)announce.getValue()).addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {

                                        Book book = dataSnapshot.getValue(Book.class);
                                        book.setBookId((String)announce.getValue());

                                        adapter.addItem(book);
                                    }

                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {

                                        Toast.makeText(App.getContext() ,getString(R.string.databaseError),Toast.LENGTH_SHORT);

                                    }
                                });
                            }

                        }

                    }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(App.getContext() ,getString(R.string.databaseError),Toast.LENGTH_SHORT);

            }
        });
    }

}
