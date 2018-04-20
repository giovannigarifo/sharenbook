package it.polito.mad.sharenbook.Utils;

import android.view.View;
import android.widget.ScrollView;

public class UserInterface {

    /**
     * Scroll to a specific View
     */
    public static void scrollToView(ScrollView scrollView, View view) {
        scrollView.post(() -> scrollView.smoothScrollTo(0, view.getBottom()));
    }
}
