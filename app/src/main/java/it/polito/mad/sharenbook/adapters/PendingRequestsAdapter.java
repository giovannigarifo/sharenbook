package it.polito.mad.sharenbook.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.mad.sharenbook.App;
import it.polito.mad.sharenbook.MyBookActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowBookActivity;
import it.polito.mad.sharenbook.fragments.RequestListFragment;
import it.polito.mad.sharenbook.model.BorrowRequest;
import it.polito.mad.sharenbook.utils.GenericFragmentDialog;
import it.polito.mad.sharenbook.utils.GlideApp;

/**
 * Recycler View Adapter Class
 */
public class PendingRequestsAdapter extends RecyclerView.Adapter<PendingRequestsAdapter.ViewHolder> {

    private final Activity mActivity;
    private final String username;
    private StorageReference mBookImagesStorage;
    private List<BorrowRequest> requests = new ArrayList<>();
    private int listType;
    private FragmentManager fragManager;

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

    public PendingRequestsAdapter(int listType, FragmentManager fragMan, Activity activity) {
        mBookImagesStorage = FirebaseStorage.getInstance().getReference(App.getContext().getString(R.string.book_images_key));
        this.listType = listType;
        this.fragManager = fragMan;
        this.mActivity = activity;

        SharedPreferences userData = activity.getSharedPreferences(activity.getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(activity.getString(R.string.username_copy_key), "");
    }

    @NonNull
    @Override
    public PendingRequestsAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        int layoutCode = (mActivity instanceof MyBookActivity) ? R.layout.item_book_showcase_rv : R.layout.item_book_showmore_rv;

        ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(layoutCode, parent, false);

        return new PendingRequestsAdapter.ViewHolder(layout);
    }

    public void addRequest(BorrowRequest req){

        requests.add(0, req);
        notifyDataSetChanged();
    }

    public void updateRequest(BorrowRequest req){
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

    public void removeBookId(String bId){
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

    public void clear(){
        int size = requests.size();
        requests.clear();
        notifyItemRangeRemoved(0, size);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(@NonNull PendingRequestsAdapter.ViewHolder holder, int position) {
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
            holder.req_counter.setText(App.getContext().getString(R.string.borrow_req_counter, req.getRequests()));
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
                bundle.putBoolean("isBookShared", req.getBookShared());

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
                String title = mActivity.getString(R.string.undo_borrow_book);
                String message = mActivity.getString(R.string.undo_borrow_book_msg);
                GenericFragmentDialog.show(mActivity, title, message, () -> undoRequest(req));
            });

        }

        holder.bookOptions.setVisibility(View.GONE);

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return requests.size();
    }

    private void undoRequest(BorrowRequest currSelectedRequest) {

        DatabaseReference usernamesDb = FirebaseDatabase.getInstance().getReference(mActivity.getString(R.string.usernames_key));

        // Create transaction Map
        HashMap<String, Object> transaction = new HashMap<>();
        transaction.put(username + "/" + mActivity.getString(R.string.borrow_requests_key) + "/" + currSelectedRequest.getBookId(), null);
        transaction.put(currSelectedRequest.getOwner() + "/" + mActivity.getString(R.string.pending_requests_key) + "/" + currSelectedRequest.getBookId() + "/" + username, null);

        usernamesDb.updateChildren(transaction, (databaseError, databaseReference) -> {

            if(databaseError == null){
                //takeReqsAdapter.removeBookId(currSelectedRequest.getBookId());
                Toast.makeText(App.getContext(), R.string.borrow_request_undone, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(App.getContext(), R.string.borrow_request_undone_fail, Toast.LENGTH_LONG).show();
            }

        });

    }

}
