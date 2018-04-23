package it.polito.mad.sharenbook.Utils;

import android.view.View;
import android.widget.ScrollView;

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

}
