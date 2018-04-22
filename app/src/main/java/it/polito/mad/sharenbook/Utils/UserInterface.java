package it.polito.mad.sharenbook.Utils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ScrollView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.storage.StorageReference;

public class UserInterface {

    /**
     * Scroll to a specific View
     */
    public static void scrollToView(ScrollView scrollView, View view) {
        scrollView.post(() -> scrollView.smoothScrollTo(0, view.getBottom()));
    }


    public static void showGlideImage(Context context, StorageReference ref, ImageView iv, long creationTime){

        GlideApp.with(context)
                .load(ref)
                .apply(RequestOptions.circleCropTransform()
                    .placeholder(getProgressBarIndeterminate(context))
                    .signature(new ObjectKey(creationTime))
                    .diskCacheStrategy(DiskCacheStrategy.ALL))
                .into(iv);

    }

    public static Drawable getProgressBarIndeterminate(Context context) {
        final int[] attrs = {android.R.attr.indeterminateDrawable};
        final int attrs_indeterminateDrawable_index = 0;
        TypedArray a = context.obtainStyledAttributes(android.R.style.Widget_ProgressBar, attrs);
        try {
            return a.getDrawable(attrs_indeterminateDrawable_index);
        } finally {
            a.recycle();
        }
    }

}
