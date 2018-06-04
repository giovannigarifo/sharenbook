package it.polito.mad.sharenbook.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;

import it.polito.mad.sharenbook.ChatActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowBookActivity;
import it.polito.mad.sharenbook.ShowCaseActivity;
import it.polito.mad.sharenbook.ShowMoreActivity;
import it.polito.mad.sharenbook.ShowOthersProfile;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.Utils;

public class ShowBooksAdapter extends RecyclerView.Adapter<ShowBooksAdapter.ViewHolder> {

    private Activity mActivity;
    private Context mAppContext;
    private StorageReference mBookImagesStorage;
    private List<Book> mBookList;
    private Location mLocation;
    private String user_id;
    private LinkedHashSet<String> favoritesBookIdList;
    private HashSet<String> requestedBookIdList;

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout mLayout;
        ImageView bookPhoto;
        TextView bookTitle;
        TextView bookDistance;
        ImageView bookOptions;
        ImageView bookmarkIcon;
        ImageView bookUnavailable;

        public ViewHolder(ConstraintLayout layout) {
            super(layout);
            mLayout = layout;
            bookPhoto = layout.findViewById(R.id.showcase_rv_book_photo);
            bookTitle = layout.findViewById(R.id.showcase_rv_book_title);
            bookDistance = layout.findViewById(R.id.showcase_rv_book_location);
            bookOptions = layout.findViewById(R.id.showcase_rv_book_options);
            bookmarkIcon = layout.findViewById(R.id.showcase_rv_book_shared);
            bookUnavailable = layout.findViewById(R.id.showcase_rv_unavailable_bg);
        }
    }

    public ShowBooksAdapter(Activity activity, List<Book> bookList, Location location, LinkedHashSet<String> favorites, HashSet<String> requested) {
        mActivity = activity;
        mAppContext = activity.getApplicationContext();
        mBookImagesStorage = FirebaseStorage.getInstance().getReference(mActivity.getString(R.string.book_images_key));
        mBookList = bookList;
        mLocation = location;
        user_id = FirebaseAuth.getInstance().getCurrentUser().getUid();
        favoritesBookIdList = favorites;
        requestedBookIdList = requested;
    }

    // Create new views (invoked by the layout manager)
    @NonNull
    @Override
    public ShowBooksAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // create a new view
        int layoutCode = (mActivity instanceof ShowCaseActivity) ? R.layout.item_book_showcase_rv : R.layout.item_book_showmore_rv;
        ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(parent.getContext()).inflate(layoutCode, parent, false);

        return new ViewHolder(layout);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull ShowBooksAdapter.ViewHolder holder, int position) {

        Book book = mBookList.get(position);
        String fileName = book.getPhotosName().get(0);
        StorageReference photoRef = mBookImagesStorage.child(book.getBookId()).child(fileName);

        // Load book photo
        GlideApp.with(mActivity)
                .load(photoRef)
                .placeholder(R.drawable.book_cover_portrait)
                .into(holder.bookPhoto);

        // Put bookmark icon if already shared
        if (book.isShared())
            holder.bookUnavailable.setVisibility(View.VISIBLE);
        else
            holder.bookUnavailable.setVisibility(View.GONE);


        // Set title
        holder.bookTitle.setText(book.getTitle());

        // Set distance
        if (mLocation != null) {
            String distance = Utils.distanceBetweenLocations(
                    mLocation.getLatitude(),
                    mLocation.getLongitude(),
                    book.getLocation_lat(),
                    book.getLocation_long());
            holder.bookDistance.setText(distance);
            holder.bookDistance.setVisibility(View.VISIBLE);

        } else {
            holder.bookDistance.setVisibility(View.GONE);
        }

        // Set listener
        holder.mLayout.setOnClickListener(v -> {
            Intent i = new Intent(mActivity, ShowBookActivity.class);
            i.putExtra("book", book);
            mActivity.startActivity(i);
        });

        // Setup options menu
        holder.bookOptions.setOnClickListener(v -> {
            showOptionsPopupMenu(v, book);
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mBookList.size();
    }

    private void showOptionsPopupMenu(View v, Book book) {

        final PopupMenu popup = new PopupMenu(mActivity, v);
        popup.inflate(R.menu.showcase_rv_options_menu);
        popup.setOnMenuItemClickListener(item -> {

            DatabaseReference favoriteBooksRef = FirebaseDatabase.getInstance()
                    .getReference(mActivity.getString(R.string.users_key))
                    .child(user_id)
                    .child(mActivity.getString(R.string.user_favorites_key))
                    .child(book.getBookId());

            switch (item.getItemId()) {

                case R.id.add_to_favorites:
                    favoriteBooksRef.setValue(ServerValue.TIMESTAMP, (databaseError, databaseReference) -> {
                        if (databaseError == null) {
                            Toast.makeText(mAppContext, R.string.showcase_add_favorite, Toast.LENGTH_SHORT).show();
                        } else
                            Log.d("FIREBASE ERROR", "Favorite -> " + databaseError.getMessage());
                    });
                    return true;

                case R.id.del_from_favorites:
                    favoriteBooksRef.removeValue((databaseError, databaseReference) -> {
                        if (databaseError == null) {
                            Toast.makeText(mAppContext, R.string.showcase_del_favorite, Toast.LENGTH_SHORT).show();
                        } else
                            Log.d("FIREBASE ERROR", "Favorite -> " + databaseError.getMessage());
                    });
                    return true;

                case R.id.contact_owner:
                    Intent chatActivity = new Intent(mActivity, ChatActivity.class);
                    chatActivity.putExtra("recipientUsername", book.getOwner_username());
                    mActivity.startActivity(chatActivity);
                    return true;

                case R.id.show_profile:
                    Intent showOwnerProfile = new Intent(mActivity, ShowOthersProfile.class);
                    showOwnerProfile.putExtra("username", book.getOwner_username());
                    mActivity.startActivity(showOwnerProfile);
                    return true;

                case R.id.borrow_book:
                    if (mActivity instanceof ShowCaseActivity) {
                        ShowCaseActivity act = (ShowCaseActivity)mActivity;
                        act.selectedBookOwner = book.getOwner_username();
                        act.selectedBookId = book.getBookId();
                        act.showDialog();
                    } else if (mActivity instanceof ShowMoreActivity) {
                        ShowMoreActivity act = (ShowMoreActivity)mActivity;
                        act.selectedBookOwner = book.getOwner_username();
                        act.selectedBookId = book.getBookId();
                        act.showDialog();
                    }
                    return true;

                default:
                    return false;
            }
        });

        // Disable contact owner menu entry if is an user's book
        if (book.getOwner_uid().equals(user_id)) {
            popup.getMenu().getItem(0).setEnabled(false);
            popup.getMenu().getItem(2).setEnabled(false);
            popup.getMenu().getItem(3).setEnabled(false);
            popup.getMenu().getItem(4).setEnabled(false);

        } else {
            if (favoritesBookIdList.contains(book.getBookId())) {
                popup.getMenu().getItem(0).setVisible(false);
                popup.getMenu().getItem(1).setVisible(true);
            }
            if (book.isShared()) {
                popup.getMenu().getItem(4).setTitle(R.string.book_unavailable).setEnabled(false);
            } else if (requestedBookIdList.contains(book.getBookId())) {
                popup.getMenu().getItem(4).setVisible(false);
                popup.getMenu().getItem(5).setVisible(true);
            }
        }

        popup.show();
    }
}