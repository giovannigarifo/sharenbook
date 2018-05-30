package it.polito.mad.sharenbook.adapters;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.polito.mad.sharenbook.EditBookActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowBookActivity;
import it.polito.mad.sharenbook.model.Book;

public class AnnouncementAdapter extends RecyclerView.Adapter<AnnouncementAdapter.AnnounceViewHolder>{

    private static List<Book> announcements;
    private static Book underModification;
    private static int positionUnderModificaiton;
    private Context context;
    private LinearLayoutManager llm;
    private StorageReference mBookImagesStorage;

   public AnnouncementAdapter(Context context, LinearLayoutManager llm) {
        announcements = new ArrayList<>();
        this.context = context;
        this.llm = llm;
        this.mBookImagesStorage = FirebaseStorage.getInstance().getReference("book_images");
        underModification = null;
    }

    @NonNull
    @Override
    public AnnouncementAdapter.AnnounceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.book_item, parent, false);
        AnnounceViewHolder announceViewHolder = new AnnounceViewHolder(v);
        return announceViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AnnouncementAdapter.AnnounceViewHolder holder, int position) {

        if(!announcements.isEmpty()) {
            // Get Book Image

            StorageReference photoRef = mBookImagesStorage.child(announcements.get(position).getBookId() + "/" + announcements.get(position).getPhotosName().get(0));
            Glide.with(context).load(photoRef).into(holder.bookPhoto);

            holder.bookTitle.setText(announcements.get(position).getTitle());

            String authors = context.getString(R.string.authors, announcements.get(position).getAuthorsAsString());
            holder.bookAuthors.setText(authors);

            holder.bookCreationTime.setText(announcements.get(position).getCreationTimeAsString(context));

            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> place = new ArrayList<>();
            try {
                place.addAll(geocoder.getFromLocation(announcements.get(position).getLocation_lat(), announcements.get(position).getLocation_long(), 1));
            }catch (IOException e) {
                e.printStackTrace();
            }

            if(!place.isEmpty())
                holder.bookLocation.setText(place.get(0).getLocality() + ", " + place.get(0).getCountryName());
            else
                holder.bookLocation.setText(R.string.unknown_place);

            holder.chiptag.setVisibility(View.INVISIBLE);

            holder.editButton.setOnClickListener(v -> {
                Intent i = new Intent(context, EditBookActivity.class);
                i.putExtra("book", announcements.get(position));
                context.startActivity(i);
                underModification = announcements.get(position);
                positionUnderModificaiton = position;

            });

            holder.cv.setOnClickListener(v -> {
                Intent i = new Intent(context, ShowBookActivity.class);
                i.putExtra("book",announcements.get(position));
                context.startActivity(i); // start activity without finishing in order to return back with back pressed
            });
        }

    }


    @Override
    public int getItemCount() {
        return announcements.size();
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    public static List<Book> getAnnouncementsModel(){
        return announcements;
    }

    public static int getPositionUnderModificaiton() {
        return positionUnderModificaiton;
    }

    public static void setPositionUnderModificaiton(int positionUnderModificaiton) {
        AnnouncementAdapter.positionUnderModificaiton = positionUnderModificaiton;
    }

    public static Book getUnderModification() {
        return underModification;
    }

    public static void setUnderModification(Book underModification) {
        AnnouncementAdapter.underModification = underModification;
    }

    public void addMultipleItems(int startPosition, List<Book> announces){
        if(!announcements.containsAll(announces)) {
            announcements.addAll(startPosition, announces);
        }
            this.notifyItemRangeInserted(startPosition, announces.size());

    }

    public void addItem(Book announce){

        if(!announcements.contains(announce))
            announcements.add(getItemCount(), announce);

        this.notifyItemInserted(getItemCount());
    }

    public void removeItem(int position){
        announcements.remove(position);
        notifyItemRemoved(position);
    }

    public void clearAnnouncements(){
        announcements.clear();
        notifyDataSetChanged();
    }



    public class AnnounceViewHolder extends RecyclerView.ViewHolder {
        CardView cv;
        TextView bookTitle;
        TextView bookAuthors;
        TextView bookCreationTime;
        TextView bookLocation;
        ImageView bookPhoto;
        Button editButton;
        View chiptag;

        AnnounceViewHolder(View itemView) {
            super(itemView);
            cv = itemView.findViewById(R.id.book_item);
            bookTitle = itemView.findViewById(R.id.book_title);
            bookAuthors = itemView.findViewById(R.id.book_authors);
            bookCreationTime = itemView.findViewById(R.id.book_creationTime);
            bookLocation = itemView.findViewById(R.id.book_location);
            bookPhoto = itemView.findViewById(R.id.book_photo);
            editButton = itemView.findViewById(R.id.edit_button);
            chiptag = itemView.findViewById(R.id.chiptag);
        }
    }

}
