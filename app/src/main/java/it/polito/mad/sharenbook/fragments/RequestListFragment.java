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
import java.util.Map;

import it.polito.mad.sharenbook.ChatActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowOthersProfile;
import it.polito.mad.sharenbook.utils.GenericFragmentDialog;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.UserInterface;
import it.polito.mad.sharenbook.utils.Utils;

public class RequestListFragment extends Fragment {

    private ArrayList<String> usernameList;
    private ArrayList<Long> requestTimeList;
    private String bookId, bookTitle, bookPhoto, bookOwner;
    private boolean isBookShared;

    private ListView requestListView;
    RequestsAdapter requestAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        usernameList = getArguments().getStringArrayList("usernameList");
        requestTimeList = new ArrayList<>();

        long[] requestTimeArray = getArguments().getLongArray("requestTimeArray");
        for (long aRequestTimeArray : requestTimeArray)
            requestTimeList.add(aRequestTimeArray);

        bookId = getArguments().getString("bookId");
        bookTitle = getArguments().getString("bookTitle");
        bookPhoto = getArguments().getString("bookPhoto");
        bookOwner = getArguments().getString("bookOwner");
        isBookShared = getArguments().getBoolean("isBookShared");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_list_request, container, false);

        TextView bookTitleText = rootView.findViewById(R.id.book_title);
        bookTitleText.setText(bookTitle);

        requestListView = rootView.findViewById(R.id.list_view_requests);
        requestAdapter = new RequestsAdapter(getActivity(), usernameList, requestTimeList);
        requestListView.setAdapter(requestAdapter);

        ImageView backButton = rootView.findViewById(R.id.back_button);
        backButton.setOnClickListener(v -> {
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        });

        return rootView;
    }



    class RequestsAdapter extends BaseAdapter {

        private Activity mActivity;
        private ArrayList<String> mUsernameList;
        private ArrayList<Long> mRequestTimeArray;

        private DatabaseReference usernamesDb;
        private StorageReference imagesStorage;

        class ViewHolder {
            ImageView userImage;
            TextView usernameText;
            TextView requestTimeText;
            ImageView optionsButton;
            Button acceptButton;
            Button rejectButton;

            void setButtonsEnabled(boolean status) {
                acceptButton.setEnabled(status);
                rejectButton.setEnabled(status);
                optionsButton.setClickable(status);
            }
        }

        RequestsAdapter(Activity activity, ArrayList<String> usernameList, ArrayList<Long> requestTimeArray) {
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

            String username = mUsernameList.get(position);
            long requestTime = mRequestTimeArray.get(position);

            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.request_list_item, parent, false);
            }

            ViewHolder holder = new ViewHolder();
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
            if (isBookShared) {
                holder.acceptButton.setAlpha(.3F);
                holder.acceptButton.setOnClickListener(v -> {
                    Toast.makeText(mActivity.getApplicationContext(), R.string.book_already_shared, Toast.LENGTH_SHORT).show();
                });
            } else {
                holder.acceptButton.setOnClickListener(v -> {
                    requestListView.setClickable(false); // Avoid double click
                    String title = mActivity.getString(R.string.accept_req_dialog);
                    String message = mActivity.getString(R.string.accept_req_dialog_msg, username);
                    GenericFragmentDialog.show(mActivity, title, message, () -> acceptRequest(username));
                });
            }

            holder.rejectButton.setOnClickListener(v -> {
                requestListView.setClickable(false); // // Avoid double click
                String title = mActivity.getString(R.string.reject_req_dialog);
                String message = mActivity.getString(R.string.reject_req_dialog_msg, username);
                GenericFragmentDialog.show(mActivity, title, message, () -> rejectRequest(username));
            });

            holder.optionsButton.setOnClickListener(v -> {
                showOptionsPopupMenu(v, username);
            });

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

        private void showOptionsPopupMenu(View v, String username) {

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
        }

        private void acceptRequest(String username) {

            DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

            // Get shared book id
            String exchangeId = rootRef.child(getString(R.string.shared_books_key)).push().getKey();

            // Create given book map
            Map<String, Object> givenBookMap = new HashMap<>();
            givenBookMap.put("counterpart", username);
            givenBookMap.put("bookId", bookId);
            givenBookMap.put("bookTitle", bookTitle);
            givenBookMap.put("bookPhoto", bookPhoto);
            givenBookMap.put("creationTime", ServerValue.TIMESTAMP);

            // Create given book map
            Map<String, Object> takenBookMap = new HashMap<>();
            takenBookMap.put("counterpart", bookOwner);
            takenBookMap.put("bookId", bookId);
            takenBookMap.put("bookTitle", bookTitle);
            takenBookMap.put("bookPhoto", bookPhoto);
            takenBookMap.put("creationTime", ServerValue.TIMESTAMP);

            // Create transaction Map
            Map<String, Object> transaction = new HashMap<>();
            transaction.put(getString(R.string.shared_books_key) + "/" + bookOwner + "/" + getString(R.string.given_books_key) + "/" + exchangeId, givenBookMap);
            transaction.put(getString(R.string.shared_books_key) + "/" + username + "/" + getString(R.string.taken_books_key) + "/" + exchangeId, takenBookMap);

            // Part of transaction to remove request data
            transaction.put(getString(R.string.usernames_key) + "/" + username + "/" + getString(R.string.borrow_requests_key) + "/" + bookId, null);
            transaction.put(getString(R.string.usernames_key) + "/" + bookOwner + "/" + getString(R.string.pending_requests_key) + "/" + bookId + "/" + username, null);

            // Set book as shared (unavailable)
            transaction.put(getString(R.string.books_key) + "/" + bookId + "/shared", true);

            // Execute transaction
            rootRef.updateChildren(transaction, (databaseError, databaseReference) -> {
                if (databaseError == null) {

                    // Update algolia
                    algoliaSetBookAsShared(bookId);

                    String requestBody = "{"
                            + "\"app_id\": \"edfbe9fb-e0fc-4fdb-b449-c5d6369fada5\","

                            + "\"filters\": [{\"field\": \"tag\", \"key\": \"User_ID\", \"relation\": \"=\", \"value\": \"" + username + "\"}],"

                            + "\"data\": {\"notificationType\": \"AcceptedRequest\", \"senderName\": \"" + bookOwner + "\"},"
                            + "\"contents\": {\"en\": \"" + bookOwner + " has accepted your borrow request for the book: " + bookTitle +".\", " +
                            "\"it\": \"" + bookOwner + " ha accettato la tua richiesta di prestito per il libro: "+ bookTitle +".\"},"
                            + "\"headings\": {\"en\": \"Your borrow request has been accepted!\", \"it\": \"La tua richiesta di prestito è stata accettata!\"}"
                            + "}";

                    // Send notification
                    Utils.sendNotification(requestBody);

                    Toast.makeText(mActivity.getApplicationContext(), R.string.borrow_request_accepted, Toast.LENGTH_LONG).show();

                    requestListView.setClickable(true);
                    if (getFragmentManager() != null) {
                        getFragmentManager().popBackStack();
                    }

                } else {
                    Toast.makeText(mActivity.getApplicationContext(), R.string.borrow_request_undone_fail, Toast.LENGTH_LONG).show();
                    requestListView.setClickable(true);
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

                    // Remove listview entry
                    int indexOfUsername = usernameList.indexOf(username);
                    usernameList.remove(indexOfUsername);
                    requestTimeList.remove(indexOfUsername);

                    if (usernameList.size() == 0) {
                        if (getFragmentManager() != null) {
                            getFragmentManager().popBackStack();
                        }
                    }

                    notifyDataSetChanged();

                    // Notification body
                    String requestBody = "{"
                            + "\"app_id\": \"edfbe9fb-e0fc-4fdb-b449-c5d6369fada5\","

                            + "\"filters\": [{\"field\": \"tag\", \"key\": \"User_ID\", \"relation\": \"=\", \"value\": \"" + username + "\"}],"

                            + "\"data\": {\"notificationType\": \"RejectedRequest\", \"senderName\": \"" + bookOwner + "\"},"
                            + "\"contents\": {\"en\": \"" + bookOwner + " has rejected your borrow request for the book: " + bookTitle +".\", " +
                            "\"it\": \"" + bookOwner + " ha rifiutato la tua richiesta di prestito per il libro: "+ bookTitle +".\"},"
                            + "\"headings\": {\"en\": \"Your borrow request has been rejected!\", \"it\": \"La tua richiesta di prestito è stata rifiutata!\"}"
                            + "}";

                    // Send notification
                    Utils.sendNotification(requestBody);
                    Toast.makeText(getContext(), R.string.borrow_request_rejected, Toast.LENGTH_LONG).show();
                    requestListView.setClickable(true);

                } else {
                    Toast.makeText(getContext(), R.string.borrow_request_undone_fail, Toast.LENGTH_LONG).show();
                    requestListView.setClickable(true);
                }
            });
        }

        private void algoliaSetBookAsShared(String bookKey) {

            try {
                Client algoliaClient = new Client("K7HV32WVKQ", "80c98eabf83684293f3b8b330ca2486e");
                Index index = algoliaClient.getIndex("books");

                JSONObject ob = new JSONObject().put("shared", true);

                index.partialUpdateObjectAsync(ob, bookKey, true, (jsonObject, e) -> Log.d("DEBUG", "Algolia UPDATE request completed."));

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e("error", "Unable to update Algolia index.");
            }
        }
    }
}
