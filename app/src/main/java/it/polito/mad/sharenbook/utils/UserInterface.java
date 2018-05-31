package it.polito.mad.sharenbook.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.support.design.widget.BottomNavigationView;
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
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.HashMap;

import it.polito.mad.sharenbook.App;
import it.polito.mad.sharenbook.MyBookActivity;
import it.polito.mad.sharenbook.MyChatsActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.ShowCaseActivity;

public class UserInterface {

    private static String[] bookCategories = App.getContext().getResources().getStringArray(R.array.book_categories);

    /*public static HashMap<String, Integer> cat_colors = new HashMap<String, Integer>() {{
        put(Arrays.asList(bookCategories).get(0),Color.argb(0,172,193 ,1));  // art, music, cinema
        put(Arrays.asList(bookCategories).get(1), Color.argb(142,36,170 ,1)); // biography
        put(Arrays.asList(bookCategories).get(2), Color.argb(67,160,71 ,1));  // Comics
        put(Arrays.asList(bookCategories).get(3), Color.argb(192,202,51 ,1));  // Economy
        put(Arrays.asList(bookCategories).get(4), Color.argb(255,238,88 ,1));  // Fantasy
        put(Arrays.asList(bookCategories).get(5), Color.argb(191,54,12 ,1));  // Cooking
        put(Arrays.asList(bookCategories).get(6), Color.argb(129,199,132 ,1));  // Hobby
        put(Arrays.asList(bookCategories).get(7), Color.argb(255,241,118 ,1));  // Kids
        put(Arrays.asList(bookCategories).get(8), Color.argb(255,234,0 ,1));  // Tech
        put(Arrays.asList(bookCategories).get(9), Color.argb(40,53,147 ,1));  // Edu
        put(Arrays.asList(bookCategories).get(10), Color.argb(239,154,154 ,1));  // LGBT
        put(Arrays.asList(bookCategories).get(11), Color.argb(213,0,0 ,1));  // Health
        put(Arrays.asList(bookCategories).get(12), Color.argb(141,110,99 ,1));  // History
        put(Arrays.asList(bookCategories).get(13), Color.argb(255,224,130 ,1));  // Enterteinment
        put(Arrays.asList(bookCategories).get(14), Color.argb(251,192,45 ,1));  // Rights
        put(Arrays.asList(bookCategories).get(15), Color.argb(159,168,218 ,1));  // Literature
        put(Arrays.asList(bookCategories).get(16), Color.argb(183,28,28 ,1));  // Medicine
        put(Arrays.asList(bookCategories).get(17), Color.argb(103,58,183 ,1));  // Thriller
        put(Arrays.asList(bookCategories).get(18), Color.argb(161,136,127 ,1));  // Politics
        put(Arrays.asList(bookCategories).get(19), Color.argb(129,212,250 ,1));  // Religion
        put(Arrays.asList(bookCategories).get(20), Color.argb(248,187,208 ,1));  // Romance
        put(Arrays.asList(bookCategories).get(21), Color.argb(123,31,162 ,1));  // Sci-fi
        put(Arrays.asList(bookCategories).get(22), Color.argb(244,81,30 ,1));  // Science
        put(Arrays.asList(bookCategories).get(23), Color.argb(255,213,79 ,1));  // Sport
        put(Arrays.asList(bookCategories).get(24), Color.argb(77,208,225 ,1));  // Travel
        put(Arrays.asList(bookCategories).get(25), Color.argb(207,216,220 ,1));  // Other
    }};*/

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
                        show_case.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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
