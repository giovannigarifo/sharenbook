package it.polito.mad.sharenbook.Utils;

import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.storage.StorageReference;

import it.polito.mad.sharenbook.EditProfileActivity;
import it.polito.mad.sharenbook.R;
import com.mancj.materialsearchbar.MaterialSearchBar;

public class UserInterface {

    /**
     * Scroll to top of a specific View
     */
    public static void scrollToViewTop(ScrollView scrollView, View view) {
        scrollView.post(() -> scrollView.smoothScrollTo(0, view.getTop()));
    }

    /**
     * Scroll to bottom of a specific View
     */
    public static void scrollToViewBottom(ScrollView scrollView, View view) {
        scrollView.post(() -> scrollView.smoothScrollTo(0, view.getBottom()));
    }


    public static void showGlideImage(Context context, StorageReference ref, ImageView iv, long creationTime){

        GlideApp.with(context)
                .load(ref)
                .apply(RequestOptions.circleCropTransform()
                    .signature(new ObjectKey(creationTime))
                    .diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(iv);

    }

}
