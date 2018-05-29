package it.polito.mad.sharenbook.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
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
import java.util.Map;

import it.polito.mad.sharenbook.ChatActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowOthersProfile;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.UserInterface;
import it.polito.mad.sharenbook.utils.Utils;

public class RequestListFragment extends Fragment {

    private ArrayList<String> usernameList;
    private long[] requestTimeArray;
    private String bookId, bookTitle, bookPhoto, bookOwner;
    private String userId;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        usernameList = getArguments().getStringArrayList("usernameList");
        requestTimeArray = getArguments().getLongArray("requestTimeArray");
        bookId = getArguments().getString("bookId");
        bookTitle = getArguments().getString("bookTitle");
        bookPhoto = getArguments().getString("bookPhoto");
        bookOwner = getArguments().getString("bookOwner");
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_list_request, container, false);
        ListView requestListView = rootView.findViewById(R.id.list_view_requests);

        RequestsAdapter requestAdapter = new RequestsAdapter(getActivity(), usernameList, requestTimeArray);
        requestListView.setAdapter(requestAdapter);

        return rootView;
    }

    class RequestsAdapter extends BaseAdapter {

        private Activity mActivity;
        private ArrayList<String> mUsernameList;
        private long[] mRequestTimeArray;

        private DatabaseReference usernamesDb;
        private StorageReference imagesStorage;

        class ViewHolder {
            ImageView userImage;
            TextView usernameText;
            TextView requestTimeText;
            ImageView optionsButton;
            Button acceptButton;
            Button rejectButton;
        }

        RequestsAdapter(Activity activity, ArrayList<String> usernameList, long[] requestTimeArray) {
            mActivity = activity;
            mUsernameList = usernameList;
            mRequestTimeArray = requestTimeArray;
            usernamesDb = FirebaseDatabase.getInstance().getReference(getString(R.string.usernames_key));
            imagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.user_images_key));
        }

        @Override
        public int getCount() {
            return mUsernameList.size();
        }

        @Override
        public Object getItem(int position) {
            return mUsernameList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {

                String username = mUsernameList.get(position);
                long requestTime = mRequestTimeArray[position];

                ViewHolder holder = new ViewHolder();

                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.request_list_item, parent, false);
                holder.userImage = convertView.findViewById(R.id.user_image);
                holder.usernameText = convertView.findViewById(R.id.username);
                holder.requestTimeText = convertView.findViewById(R.id.request_time);
                holder.optionsButton = convertView.findViewById(R.id.options_button);
                holder.acceptButton = convertView.findViewById(R.id.accept_button);
                holder.rejectButton = convertView.findViewById(R.id.reject_button);

                // Get user picSignature
                showUserPhoto(username, holder);

                // Bind item values
                holder.usernameText.setText(username);
                holder.requestTimeText.setText(Utils.convertTime(requestTime, "dd MMM, HH:mm"));

                // Assign click listeners
                holder.acceptButton.setOnClickListener(v -> {
                    acceptRequest(username);
                });
                holder.rejectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        rejectRequest(username);
                    }
                });
                holder.optionsButton.setOnClickListener(v -> {

                    final PopupMenu popup = new PopupMenu(mActivity, v);
                    popup.inflate(R.menu.chiptag_menu);
                    popup.setOnMenuItemClickListener(item -> {

                        switch (item.getItemId()) {

                            case R.id.contact_user:
                                Intent chatActivity = new Intent(mActivity, ChatActivity.class);
                                chatActivity.putExtra("recipientUsername", username);
                                mActivity.startActivity(chatActivity);
                                return true;

                            case R.id.show_profile:
                                Intent showOwnerProfile = new Intent(mActivity, ShowOthersProfile.class);
                                showOwnerProfile.putExtra("username", username);
                                mActivity.startActivity(showOwnerProfile);
                                return true;

                            default:
                                return false;
                        }
                    });

                    popup.show();
                });
            }

            return  convertView;
        }

        private void showUserPhoto(String username, ViewHolder holder) {

            usernamesDb.child(username).child(getString(R.string.pic_signature_key)).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    if (dataSnapshot.exists()) {
                        long picSignature = (long) dataSnapshot.getValue();
                        UserInterface.showGlideImage(mActivity, imagesStorage.child(username + ".jpg"), holder.userImage, picSignature);

                    } else {
                        GlideApp.with(mActivity).load(mActivity.getResources().getDrawable(R.drawable.ic_profile)).into(holder.userImage);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        }

        private void acceptRequest(String username) {

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

            // Get shared book id
            String sharingId = rootRef.child(getString(R.string.users_key)).child(userId).child(getString(R.string.shared_books_key)).push().getKey();

            // Create shared book map
            Map<String, Object> sharedBookMap = new HashMap<>();
            sharedBookMap.put("borrower", username);
            sharedBookMap.put("bookId", bookId);
            sharedBookMap.put("bookTitle", bookTitle);
            sharedBookMap.put("bookPhoto", bookPhoto);
            sharedBookMap.put("creationTime", ServerValue.TIMESTAMP);
            sharedBookMap.put("returned", false);

            // Create transaction Map
            Map<String, Object> transaction = new HashMap<>();
            transaction.put(getString(R.string.users_key) + "/" + userId + "/" + getString(R.string.shared_books_key) + "/" + sharingId, sharedBookMap);

            // Part of transaction to remove request data
            transaction.put(getString(R.string.usernames_key) + "/" + username + "/" + getString(R.string.borrow_requests_key) + "/" + bookId, null);
            transaction.put(getString(R.string.usernames_key) + "/" + bookOwner + "/" + getString(R.string.pending_requests_key) + "/" + bookId + "/" + username, null);

            // Set book as shared (unavailable)
            transaction.put(getString(R.string.books_key) + "/" + bookId + "/shared", true);

            // Execute transaction
            rootRef.updateChildren(transaction, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if (databaseError == null) {

                        // Send notification
                        //sendNotification(selectedBookOwner, username);
                        Toast.makeText(getContext(), R.string.borrow_request_accepted, Toast.LENGTH_LONG).show();
                        if (getFragmentManager() != null) {
                            getFragmentManager().popBackStack();
                        }

                    } else {
                        Toast.makeText(getContext(), R.string.borrow_request_undone_fail, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        private void rejectRequest(String username) {

            // Create transaction Map
            Map<String, Object> transaction = new HashMap<>();

            // Part of transaction to remove request data
            transaction.put(username + "/" + getString(R.string.borrow_requests_key) + "/" + bookId, null);
            transaction.put(bookOwner + "/" + getString(R.string.pending_requests_key) + "/" + bookId + "/" + username, null);

            usernamesDb.updateChildren(transaction, (databaseError, databaseReference) -> {
                if (databaseError == null) {

                    // Send notification
                    //sendNotification(selectedBookOwner, username);
                    Toast.makeText(getContext(), R.string.borrow_request_rejected, Toast.LENGTH_LONG).show();
                    if (getFragmentManager() != null) {
                        getFragmentManager().popBackStack();
                    }

                } else {
                    Toast.makeText(getContext(), R.string.borrow_request_undone_fail, Toast.LENGTH_LONG).show();
                }
            });
        }
    }
}
