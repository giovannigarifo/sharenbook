package it.polito.mad.sharenbook.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.polito.mad.sharenbook.ChatActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.SearchActivity;
import it.polito.mad.sharenbook.ShowBookActivity;
import it.polito.mad.sharenbook.ShowOthersProfile;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.model.UserProfile;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.UserInterface;

/**
 * This fragment read the searchResult activity from the parent activity SearchActivity
 * and then displays is in a recyclerview list
 */
public class SearchListFragment extends Fragment {

    //reference to the parent activity
    SearchActivity searchActivity;

    //algolia result obtained from parent
    private ArrayList<Book> searchResult; //list of book that matched the query

    //RecyclerView
    RecyclerView fragment_searchlist_rv;
    SearchBookAdapter sbAdapter;


    // Required empty public constructor
    public SearchListFragment() {
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment.
     */
    public static SearchListFragment newInstance() {

        SearchListFragment fragment = new SearchListFragment();
        fragment.setRetainInstance(true); //save state when rotating
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.searchActivity = (SearchActivity) getActivity(); //get parent activity reference
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search_list, container, false);
        return view;
    }


    /**
     * @param view
     * @param savedInstanceState
     */
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        this.searchResult = searchActivity.getSearchResult(); //read the collection to be displayed

        //setup recyclerview
        fragment_searchlist_rv = (RecyclerView) view.findViewById(R.id.fragment_searchlist_rv);
        StaggeredGridLayoutManager sglm;
        LinearLayoutManager llm;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

            sglm = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
            fragment_searchlist_rv.setLayoutManager(sglm);

        } else if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            llm = new LinearLayoutManager(this.getContext());
            llm.setOrientation(LinearLayoutManager.VERTICAL);
            fragment_searchlist_rv.setLayoutManager(llm);
        }

        sbAdapter = new SearchBookAdapter(this.searchResult, getContext());
        fragment_searchlist_rv.setAdapter(sbAdapter);
    }

    /**
     * Method called from SearchActivity to notify the fragment that the searchResult collection has changed
     */
    public void updateDisplayedSearchResult() {

        this.sbAdapter.notifyDataSetChanged();
    }
}


/*****************************
 * SearchBookAdapter class   *
 *****************************/
class SearchBookAdapter extends RecyclerView.Adapter<SearchBookAdapter.SearchBookViewHolder> {

    private List<Book> searchResult;
    private Context context;
    private UserProfile user;

    //constructor
    SearchBookAdapter(ArrayList<Book> searchResult, Context context) {
        this.searchResult = searchResult;
        this.context = context;
        user = NavigationDrawerManager.getUserParcelable(context);
    }

    //Inner Class that provides a reference to the views for each data item of the collection
    class SearchBookViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout item_search_result;

        SearchBookViewHolder(LinearLayout ll) {
            super(ll);
            this.item_search_result = ll;
        }
    }

    /**
     * Create new ViewHolder objects (invoked by the layout manager) and set the view to use to
     * display it's content
     *
     * @param parent   :
     * @param viewType :
     * @return BookPhotoViewHolder :
     */
    @NonNull
    @Override
    public SearchBookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater li = LayoutInflater.from(parent.getContext());
        LinearLayout ll = null;

        if (parent.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {

            ll = (LinearLayout) li.inflate(R.layout.book_item, parent, false);

        } else if (parent.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {

            ll = (LinearLayout) li.inflate(R.layout.book_item, parent, false);
        }

        return new SearchBookViewHolder(ll);
    }

    /**
     * Replace the contents of a ViewHolder (invoked by the layout manager)
     *
     * @param holder   :
     * @param position :
     */
    @Override
    public void onBindViewHolder(@NonNull SearchBookViewHolder holder, int position) {

        //hide card
        holder.item_search_result.setVisibility(View.INVISIBLE);

        //photo
        ImageView photo = holder.item_search_result.findViewById(R.id.book_photo);
        photo.setImageResource(R.drawable.book_photo_placeholder);

        Book book = searchResult.get(position);
        String fileName = book.getPhotosName().get(0);
        StorageReference thumbnailOrFirstPhotoRef = FirebaseStorage.getInstance().getReference().child("book_images/" + book.getBookId() + "/" + fileName);

        GlideApp.with(context).load(thumbnailOrFirstPhotoRef)
                .placeholder(R.drawable.book_photo_placeholder)
                .error(R.drawable.book_photo_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade(250))
                .into(photo);

        //title
        TextView title = holder.item_search_result.findViewById(R.id.book_title);
        title.setText(this.searchResult.get(position).getTitle());

        //author
        TextView authors = holder.item_search_result.findViewById(R.id.book_authors);
        authors.setText(this.searchResult.get(position).getAuthorsAsString());

        //creationTime
        TextView creationTime = holder.item_search_result.findViewById(R.id.book_creationTime);
        creationTime.setText(this.searchResult.get(position).getCreationTimeAsString(context));

        //location
        TextView location = holder.item_search_result.findViewById(R.id.book_location);
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        List<Address> place = new ArrayList<>();
        try {
            place.addAll(geocoder.getFromLocation(searchResult.get(position).getLocation_lat(), searchResult.get(position).getLocation_long(), 1));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!place.isEmpty())
            location.setText(place.get(0).getLocality() + ", " + place.get(0).getCountryName());
        else
            location.setText(R.string.unknown_place);


        //card listeners
        CardView card = holder.item_search_result.findViewById(R.id.book_item);

        card.setOnClickListener((v -> {
            Intent i = new Intent(context, ShowBookActivity.class);
            i.putExtra("book", searchResult.get(position));
            context.startActivity(i); // start activity without finishing in order to return back with back pressed
        }));

        //Set Chiptag data
        View chiptag = holder.item_search_result.findViewById(R.id.chiptag);
        TextView tv_chiptag = chiptag.findViewById(R.id.text_user);
        tv_chiptag.setText(searchResult.get(position).getOwner_username());
        ImageView iv_chiptag = chiptag.findViewById(R.id.img);
        holder.item_search_result.findViewById(R.id.edit_button).setVisibility(View.INVISIBLE);

        chiptag.setOnClickListener(view -> {
            if (!book.getOwner_username().equals(user.getUsername())) {
                final PopupMenu popup = new PopupMenu(view.getContext(), view);
                popup.getMenuInflater().inflate(R.menu.chiptag_menu, popup.getMenu());
                popup.setOnMenuItemClickListener(item -> {
                    switch (item.getItemId()) {
                        case R.id.contact_user:
                            Intent chatActivity = new Intent(context, ChatActivity.class);
                            chatActivity.putExtra("recipientUsername", book.getOwner_username());
                            context.startActivity(chatActivity);
                            break;
                        case R.id.show_profile:
                            Intent showOwnerProfile = new Intent(context, ShowOthersProfile.class);
                            showOwnerProfile.putExtra("username", book.getOwner_username());
                            context.startActivity(showOwnerProfile);
                            break;
                    }
                    return false;
                });
                popup.show();
            }
        });

        String username = searchResult.get(position).getOwner_username();

        DatabaseReference recipientPicSignature = FirebaseDatabase.getInstance().getReference("usernames").child(username).child("picSignature");
        recipientPicSignature.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    long picSignature = (long) dataSnapshot.getValue();
                    UserInterface.showGlideImage(context,
                            FirebaseStorage.getInstance().getReference().child("/images").child("/" + username + ".jpg"),
                            iv_chiptag,
                            picSignature);
                } else {
                    GlideApp.with(context).load(context.getResources().getDrawable(R.drawable.ic_profile)).into(iv_chiptag);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //show card
        holder.item_search_result.setVisibility(View.VISIBLE);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.searchResult.size();
    }

}

