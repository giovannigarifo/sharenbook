package it.polito.mad.sharenbook.adapters;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.LinearLayoutManager;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import it.polito.mad.sharenbook.App;
import it.polito.mad.sharenbook.ChatActivity;
import it.polito.mad.sharenbook.MyBookActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowBookActivity;
import it.polito.mad.sharenbook.ShowMoreActivity;
import it.polito.mad.sharenbook.ShowOthersProfile;
import it.polito.mad.sharenbook.WriteReviewActivity;
import it.polito.mad.sharenbook.fragments.ExchangesFragment;
import it.polito.mad.sharenbook.model.Exchange;
import it.polito.mad.sharenbook.utils.GenericFragmentDialog;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.Utils;

/**
 * Recycler View Adapter Class
 */
public class ExchangesAdapter extends RecyclerView.Adapter<ExchangesAdapter.ViewHolder> {

    private final Activity mActivity;
    private StorageReference mBookImagesStorage;
    private List<Exchange> exchangeList;
    private int listType;
    private String username;

    class ViewHolder extends RecyclerView.ViewHolder {
        ConstraintLayout mLayout;
        ImageView bookPhoto;
        TextView bookTitle;
        TextView bookDistance;
        ImageView bookOptions;
        TextView tvReviewDone;
        Button btnNotReviewed;

        ViewHolder(ConstraintLayout layout) {
            super(layout);
            mLayout = layout;
            bookPhoto = layout.findViewById(R.id.showcase_rv_book_photo);
            bookTitle = layout.findViewById(R.id.showcase_rv_book_title);
            bookDistance = layout.findViewById(R.id.showcase_rv_book_location);
            bookOptions = layout.findViewById(R.id.showcase_rv_book_options);
            tvReviewDone = layout.findViewById(R.id.exchange_reviewed);
            btnNotReviewed = layout.findViewById(R.id.exchange_not_reviewed);

        }
    }

    public ExchangesAdapter(List<Exchange> exchanges, int listType, String user, Activity activity) {
        mBookImagesStorage = FirebaseStorage.getInstance().getReference(App.getContext().getString(R.string.book_images_key));
        exchangeList = exchanges;
        this.listType = listType;
        this.username = user;
        this.mActivity = activity;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        int layoutCode = (mActivity instanceof MyBookActivity) ? R.layout.item_book_showcase_rv : R.layout.item_book_showmore_rv;

        ConstraintLayout layout = (ConstraintLayout) LayoutInflater.from(parent.getContext())
                .inflate(layoutCode, parent, false);

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

        if (listType == 2) {

            if (!exchange.isReviewed()) {

                holder.tvReviewDone.setVisibility(View.GONE);
                holder.btnNotReviewed.setVisibility(View.VISIBLE);

                holder.btnNotReviewed.setOnClickListener(v -> {

                    Intent startWriteReviewActivity = new Intent(App.getContext(), WriteReviewActivity.class);
                    startWriteReviewActivity.putExtra("bookId", exchange.getBookId());
                    startWriteReviewActivity.putExtra("bookTitle", exchange.getBookTitle());
                    startWriteReviewActivity.putExtra("creationTime", exchange.getCreationTime());
                    startWriteReviewActivity.putExtra("userNickName", exchange.getCounterpart());
                    startWriteReviewActivity.putExtra("isGiven", exchange.isGiven());
                    startWriteReviewActivity.putExtra("bookPhoto", exchange.getBookPhoto());
                    startWriteReviewActivity.putExtra("exchangeId", exchange.getExchangeId());

                    App.getContext().startActivity(startWriteReviewActivity);
                });

            } else {
                holder.btnNotReviewed.setVisibility(View.GONE);
                holder.tvReviewDone.setVisibility(View.VISIBLE);
            }

        }

        // Setup options menu
        holder.bookOptions.setOnClickListener(v -> {

            final PopupMenu popup = new PopupMenu(App.getContext(), v);
            popup.inflate(R.menu.exchanges_taken_rv_options_menu);
            popup.setOnMenuItemClickListener(item -> {

                switch (item.getItemId()) {

                    case R.id.contact_owner:
                        Intent chatActivity = new Intent(App.getContext(), ChatActivity.class);
                        chatActivity.putExtra("recipientUsername", exchange.getCounterpart());
                        App.getContext().startActivity(chatActivity);
                        return true;

                    case R.id.show_profile:
                        Intent showOwnerProfile = new Intent(App.getContext(), ShowOthersProfile.class);
                        showOwnerProfile.putExtra("username", exchange.getCounterpart());
                        App.getContext().startActivity(showOwnerProfile);
                        return true;

                    case R.id.return_book:
                        String title = mActivity.getString(R.string.return_book_title);
                        String message = mActivity.getString(R.string.return_book_message);
                        GenericFragmentDialog.show(mActivity, title, message, () -> returnBook(exchange));
                        return true;

                    default:
                        return false;
                }
            });

            if (listType != 0) {
                popup.getMenu().getItem(2).setVisible(false);
                String popupItemTitle = App.getContext().getString(R.string.contact_borrower, exchange.getCounterpart());
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

    private void returnBook(Exchange ex) {

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        // Create borrower returned book map
        Map<String, Object> borrowerReturnedBookMap = new HashMap<>();
        borrowerReturnedBookMap.put("counterpart", ex.getCounterpart());
        borrowerReturnedBookMap.put("bookId", ex.getBookId());
        borrowerReturnedBookMap.put("bookTitle", ex.getBookTitle());
        borrowerReturnedBookMap.put("bookPhoto", ex.getBookPhoto());
        borrowerReturnedBookMap.put("creationTime", ServerValue.TIMESTAMP);
        borrowerReturnedBookMap.put("given", false);
        borrowerReturnedBookMap.put("reviewed", false);

        // Create lender returned book map
        Map<String, Object> lenderReturnedBookMap = new HashMap<>();
        lenderReturnedBookMap.put("counterpart", username);
        lenderReturnedBookMap.put("bookId", ex.getBookId());
        lenderReturnedBookMap.put("bookTitle", ex.getBookTitle());
        lenderReturnedBookMap.put("bookPhoto", ex.getBookPhoto());
        lenderReturnedBookMap.put("creationTime", ServerValue.TIMESTAMP);
        lenderReturnedBookMap.put("given", true);
        lenderReturnedBookMap.put("reviewed", false);

        // Create transaction Map
        Map<String, Object> transaction = new HashMap<>();
        transaction.put(App.getContext().getString(R.string.shared_books_key) + "/" + ex.getCounterpart() + "/" + App.getContext().getString(R.string.given_books_key) + "/" + ex.getExchangeId(), null);
        transaction.put(App.getContext().getString(R.string.shared_books_key) + "/" + username + "/" + App.getContext().getString(R.string.taken_books_key) + "/" + ex.getExchangeId(), null);
        transaction.put(App.getContext().getString(R.string.shared_books_key) + "/" + ex.getCounterpart() + "/" + App.getContext().getString(R.string.archive_books_key) + "/" + ex.getExchangeId(), lenderReturnedBookMap);
        transaction.put(App.getContext().getString(R.string.shared_books_key) + "/" + username + "/" + App.getContext().getString(R.string.archive_books_key) + "/" + ex.getExchangeId(), borrowerReturnedBookMap);

        // Set book as not shared (available again)
        transaction.put(App.getContext().getString(R.string.books_key) + "/" + ex.getBookId() + "/shared", false);

        // Execute transaction
        rootRef.updateChildren(transaction, (databaseError, databaseReference) -> {
            if (databaseError == null) {

                // Update Algolia
                algoliaSetBookAsNotShared(ex.getBookId());

                //this.removeExchange(ex);

                String requestBody = "{"
                        + "\"app_id\": \"edfbe9fb-e0fc-4fdb-b449-c5d6369fada5\","

                        + "\"filters\": [{\"field\": \"tag\", \"key\": \"User_ID\", \"relation\": \"=\", \"value\": \"" + ex.getCounterpart() + "\"}],"

                        + "\"data\": {\"notificationType\": \"returnedBook\", \"senderName\": \"" + username + "\"},"
                        + "\"contents\": {\"en\": \"" + username + " has returned your book: " + ex.getBookTitle() +".\", " +
                        "\"it\": \"" + username + " ti ha restituto il libro: "+ ex.getBookTitle() +".\"},"
                        + "\"headings\": {\"en\": \"Your book has returned!\", \"it\": \"Ti è stato restituito un libro!\"}"
                        + "}";

                // Send notification
                Utils.sendNotification(requestBody);

                Toast.makeText(App.getContext(), R.string.book_returned_correctly, Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(App.getContext(), R.string.borrow_request_undone_fail, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void algoliaSetBookAsNotShared(String bookKey) {

        try {
            Client algoliaClient = new Client("K7HV32WVKQ", "80c98eabf83684293f3b8b330ca2486e");
            Index index = algoliaClient.getIndex("books");

            JSONObject ob = new JSONObject().put("shared", false);

            index.partialUpdateObjectAsync(ob, bookKey, true, (jsonObject, e) -> Log.d("DEBUG", "Algolia UPDATE request completed."));

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("error", "Unable to update Algolia index.");
        }
    }

    public void clear() {
        exchangeList.clear();
        notifyDataSetChanged();
    }

    private void removeExchange(Exchange exchange) {

        //Add book to archive Books
        if (mActivity instanceof MyBookActivity) {
            ExchangesFragment fragment = ((ExchangesFragment) ((MyBookActivity) mActivity).mSectionsPagerAdapter.getCurrentFragment());

            switch (listType) {
                case 0:
                    ExchangesAdapter archiveAdapter;
                    LinearLayoutManager llm = (LinearLayoutManager) fragment.archiveBooksRV.getLayoutManager();
                    if ((archiveAdapter = (ExchangesAdapter) fragment.archiveBooksRV.getAdapter()) != null) {
                        archiveAdapter.addExchange(exchange);
                        llm.scrollToPosition(0);
                    } else {
                        fragment.archiveBooksAdapter = new ExchangesAdapter(new ArrayList<>(Arrays.asList(exchange)), 2, username, mActivity);
                        fragment.archiveBooksRV.setAdapter(fragment.archiveBooksAdapter);
                        fragment.archiveCV.setVisibility(View.VISIBLE);
                    }

                    if (exchangeList.size() == 0) {
                        fragment.takenBooksRV.setVisibility(View.GONE);
                        fragment.noTakenTV.setVisibility(View.VISIBLE);
                        fragment.takenMoreTV.setVisibility(View.INVISIBLE);
                    }
                    break;
                default:
            }
        } else if (mActivity instanceof ShowMoreActivity) {
            mActivity.finish();
        }
    }

    public void addExchange(Exchange ex) {
        exchangeList.add(0, ex);
        notifyItemInserted(0);

    }

    public void remove(Exchange ex){
        int pos = -1;
        for(Exchange e : exchangeList){
           if(e.getExchangeId().equals(ex.getExchangeId())){
               pos = exchangeList.indexOf(e);
               break;
           }
        }
        if(pos != -1){
            exchangeList.remove(pos);
        }
        notifyItemRemoved(pos);

        if (mActivity instanceof ShowMoreActivity && this.getItemCount() == 0) {
            mActivity.finish();
        }
    }
}