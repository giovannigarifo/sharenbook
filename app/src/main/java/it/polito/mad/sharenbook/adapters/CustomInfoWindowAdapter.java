package it.polito.mad.sharenbook.adapters;

import android.app.Activity;
import android.content.Context;
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
import it.polito.mad.sharenbook.utils.GlideApp;

/**
 * Created by Davide on 03/05/2018.
 */
public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

    private Context context;

    public CustomInfoWindowAdapter(Context context){
        this.context = context;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {
        View view = ((Activity) context).getLayoutInflater().inflate(R.layout.customwindow, null);

        Book book = (Book) marker.getTag();

        TextView tvTitle =  view.findViewById(R.id.tv_title);
        TextView tvSubTitle = view.findViewById(R.id.tv_subtitle);
        TextView tvHint = view.findViewById(R.id.tv_hint);
        ImageView thumbnail = view.findViewById(R.id.iv_thumbnail);

        tvTitle.setText(marker.getTitle());
        tvSubTitle.setText(book.getAuthorsAsString());
        tvHint.setText(R.string.infoWindow_hint);

        int thumbnailOrFirstPhotoPosition = book.getNumPhotos() - 1;
        StorageReference thumbnailOrFirstPhotoRef = FirebaseStorage.getInstance().getReference().child("book_images/" + book.getBookId()  + "/" + thumbnailOrFirstPhotoPosition + ".jpg");

        GlideApp.with(context).load(thumbnailOrFirstPhotoRef)
                .error(R.drawable.book_photo_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade(500))
                .into(thumbnail);

        return view;
    }
}
