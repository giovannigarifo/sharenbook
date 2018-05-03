package it.polito.mad.sharenbook.utils;

import android.app.Activity;
import android.net.Uri;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.model.Book;

/**
 * Created by Davide on 03/05/2018.
 */
public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private Activity context;

    public CustomInfoWindowAdapter(Activity context){
        this.context = context;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        View view = context.getLayoutInflater().inflate(R.layout.customwindow, null);

        Book book = (Book) marker.getTag();

        TextView tvTitle =  view.findViewById(R.id.tv_title);
        TextView tvSubTitle = view.findViewById(R.id.tv_subtitle);
        ImageView thumbnail = view.findViewById(R.id.iv_thumbnail);

        tvTitle.setText(marker.getTitle());
        tvSubTitle.setText(book.getAuthorsAsString());


        int thumbnailOrFirstPhotoPosition = book.getNumPhotos() - 1;
        StorageReference thumbnailOrFirstPhotoRef = FirebaseStorage.getInstance().getReference().child("book_images/" + book.getBookId()  + "/" + thumbnailOrFirstPhotoPosition + ".jpg");

        GlideApp.with(context).load(thumbnailOrFirstPhotoRef)
                .placeholder(R.drawable.book_photo_placeholder)
                .error(R.drawable.book_photo_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .into(thumbnail);

        return view;
    }
}
