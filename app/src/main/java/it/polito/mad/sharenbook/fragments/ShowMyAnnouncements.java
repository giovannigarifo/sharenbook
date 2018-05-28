package it.polito.mad.sharenbook.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
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

import it.polito.mad.sharenbook.App;
import it.polito.mad.sharenbook.adapters.AnnouncementAdapter;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShareBookActivity;
import it.polito.mad.sharenbook.model.Book;


public class ShowMyAnnouncements extends Fragment {

    /** FireBase objects */
    private FirebaseUser firebaseUser;
    private DatabaseReference booksDb;
    private DatabaseReference userBooksDb;

    private AnnouncementAdapter adapter;
    private LinearLayoutManager llm;
    private RecyclerView rv;

    private ArrayList<Book> books = new ArrayList<>();

    public static ShowMyAnnouncements newInstance() {
        ShowMyAnnouncements fragment = new ShowMyAnnouncements();

        //Bundle args = new Bundle();
        //args.putInt("someInt", someInt);
        //fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        View rootView = inflater.inflate(R.layout.fragment_show_announcements, container, false);
        rv = rootView.findViewById(R.id.expanded_books);

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

                        AlertDialog.Builder no_books = new AlertDialog.Builder(App.getContext()); //give a context to Dialog
                        no_books.setTitle(R.string.no_books_alert_title);
                        no_books.setMessage(R.string.no_books_suggestion);
                        no_books.setPositiveButton(android.R.string.ok, (dialog, which) -> {

                                    Intent i = new Intent (App.getContext(), ShareBookActivity.class);
                                    startActivity(i);
                                    getActivity().finish();

                                }
                        ).setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> {
                                    dialog.dismiss();

                                }
                        );

                        no_books.show();

                        Log.d("No books", "I am here");

                    }else{ /** there are announcements */

                        if(books.isEmpty()){

                            Iterable<DataSnapshot> announces = dataSnapshot.getChildren();

                            for(DataSnapshot announce : announces){

                                booksDb.child((String)announce.getValue()).orderByChild("creationTime").addListenerForSingleValueEvent(new ValueEventListener() {
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
