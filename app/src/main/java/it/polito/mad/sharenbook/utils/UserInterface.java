package it.polito.mad.sharenbook.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.signature.ObjectKey;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;

import it.polito.mad.sharenbook.MyBookActivity;
import it.polito.mad.sharenbook.MyChatsActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.SearchActivity;
import it.polito.mad.sharenbook.ShowCaseActivity;
import it.polito.mad.sharenbook.ShowProfileActivity;

public class UserInterface {

    private static final int widthT = 700;

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


    public static void showGlideImage(Context context, StorageReference ref, ImageView iv, long signature){

        GlideApp.with(context)
                .load(ref)
                .apply(RequestOptions.circleCropTransform()
                    .signature(new ObjectKey(signature))
                    .diskCacheStrategy(DiskCacheStrategy.ALL))
                .placeholder(AppCompatResources.getDrawable(context,R.drawable.ic_profile))
                .into(iv);
    }

    /**
     * fullNameResize method
     */
    public static void TextViewFontResize(int text_length, WindowManager winManager, TextView tv) {

        DisplayMetrics metrics = new DisplayMetrics();
        winManager.getDefaultDisplay().getMetrics(metrics);

        if (metrics.densityDpi != DisplayMetrics.DENSITY_HIGH || metrics.widthPixels < widthT) {

            if (text_length <= 16) {
                tv.setTextSize(2, 24);
            } else if (text_length > 16 && text_length <= 22) {
                tv.setTextSize(2, 18);
            } else {
                tv.setTextSize(2, 14);
            }
        }

    }

    /**
     * Convert dp to pixel
     */
    public static int convertDpToPixel(float dp){
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float px = dp * (metrics.densityDpi / 160f);
        return Math.round(px);
    }

    /**
     * Setup Bottom Navigation Bar
     */
    public static void setupNavigationBar(Activity activity, int selectedItem, boolean finishOnNav) {

        // Get navigationBar view
        BottomNavigationView navBar = activity.findViewById(R.id.navigation);

        // Set navigation_shareBook as selected item
        if (selectedItem != 0)
            navBar.setSelectedItemId(selectedItem);

        // Set the listeners for the navigation bar items
        navBar.setOnNavigationItemSelectedListener(item -> {

            switch (item.getItemId()) {
                case R.id.navigation_showcase:
                    if (!(activity instanceof ShowCaseActivity)) {
                        Intent show_case = new Intent(activity.getApplicationContext(), ShowCaseActivity.class);
                        show_case.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        activity.startActivity(show_case);
                        if (finishOnNav)
                            activity.finish();
                    }
                    break;

                case R.id.navigation_chat:
                    if (!(activity instanceof MyChatsActivity)) {
                        Intent myChats = new Intent(activity.getApplicationContext(), MyChatsActivity.class);
                        myChats.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        activity.startActivity(myChats);
                        if (finishOnNav)
                            activity.finish();
                    }
                    break;

                case R.id.navigation_myBook:
                    if (!(activity instanceof MyBookActivity)) {
                        Intent my_books = new Intent(activity.getApplicationContext(), MyBookActivity.class);
                        my_books.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        activity.startActivity(my_books);
                        if (finishOnNav)
                            activity.finish();
                    }
                    break;
            }

            return true;
        });
    }

    public static void setupNavigationBar(Activity activity, int selectedItem) {
        setupNavigationBar(activity, selectedItem, false);
    }
}
