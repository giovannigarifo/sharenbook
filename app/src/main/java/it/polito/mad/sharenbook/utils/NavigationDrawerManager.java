package it.polito.mad.sharenbook.utils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.onesignal.OneSignal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import it.polito.mad.sharenbook.MyBookActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.SearchActivity;
import it.polito.mad.sharenbook.ShareBookActivity;
import it.polito.mad.sharenbook.SplashScreenActivity;
import it.polito.mad.sharenbook.TabbedShowProfileActivity;
import it.polito.mad.sharenbook.model.UserProfile;

public class NavigationDrawerManager {

    private static NavigationDrawerProfile navigationDrawerProfile;

    public static void setNavigationDrawerProfileByUser (UserProfile user){

        if(navigationDrawerProfile == null) { // if not exits creates it
            navigationDrawerProfile = new NavigationDrawerProfile(user);
        }else { //otherwise the method is used to overwrite the fields
            navigationDrawerProfile.setUser_fullname(user.getFullname());
            navigationDrawerProfile.setUser_email(user.getEmail());
            navigationDrawerProfile.setUser_picturePath("images/" + user.getUsername() + ".jpg");
            navigationDrawerProfile.setPictureSignature(user.getPicture_timestamp());


        }


    }

    public static void setNavigationDrawerProfileByFields(String name,String email,String username, String pictureSignature){

        if(navigationDrawerProfile == null) {
            navigationDrawerProfile = new NavigationDrawerProfile(name,email,"images/" + username + ".jpg",pictureSignature);
        }else {
            navigationDrawerProfile.setUser_fullname(name);
            navigationDrawerProfile.setUser_email(email);
            navigationDrawerProfile.setUser_picturePath("images/" + username + ".jpg");
            navigationDrawerProfile.setPictureSignature(pictureSignature);

        }

    }

    public static NavigationDrawerProfile getNavigationDrawerProfile(){
        return navigationDrawerProfile;
    }

    public static void setDrawerViews(Context context,WindowManager windowManager, TextView drawer_fullname, TextView drawer_email, CircularImageView drawer_userPicture,NavigationDrawerProfile navigationDrawerProfile){

        if(navigationDrawerProfile == null)
            navigationDrawerProfile = new NavigationDrawerProfile(NavigationDrawerManager.getUserParcelable(context));

        String profile_picture_signature = null;
        if(navigationDrawerProfile.getPictureSignature()!=null)
            profile_picture_signature = navigationDrawerProfile.getPictureSignature();
        String default_picture_timestamp = "void";

        UserInterface.TextViewFontResize(navigationDrawerProfile.getUser_fullname().length(), windowManager, drawer_fullname);
        if(drawer_fullname != null )
            drawer_fullname.setText(navigationDrawerProfile.getUser_fullname());
        if(drawer_email != null)
            drawer_email.setText(navigationDrawerProfile.getUser_email());


        /** set drawer user picture **/

        if (!profile_picture_signature.equals(default_picture_timestamp)) {
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(navigationDrawerProfile.getUser_picturePath());
            UserInterface.showGlideImage(context, storageRef, drawer_userPicture,  Long.valueOf(profile_picture_signature));
        }

    }


    public static UserProfile getUserParcelable(Context context){

        SharedPreferences userData = context.getSharedPreferences(context.getString(R.string.userData_preferences), Context.MODE_PRIVATE);

        UserProfile user = new UserProfile(
                userData.getString(context.getString(R.string.uid_pref), "void"),
                userData.getString(context.getString(R.string.fullname_pref), "void"),
                userData.getString(context.getString(R.string.username_pref), "void"),
                userData.getString(context.getString(R.string.email_pref), "void"),
                userData.getString(context.getString(R.string.city_pref), "void"),
                userData.getString(context.getString(R.string.bio_pref), "void"),
                userData.getString(context.getString(R.string.picture_pref), "void")
                );

        String pref_categories_String = userData.getString(context.getString(R.string.categories_pref), "void");
        String[] categories = pref_categories_String.split(", ");
        ArrayList<String> selectedCategories = new ArrayList<String>(Arrays.asList(categories));

        String[] book_categories = context.getResources().getStringArray(R.array.book_categories);
        List<Integer> pref_categories_Int = new ArrayList<>();

        for (int i = 0; i < selectedCategories.size(); i++)
            pref_categories_Int.add(Arrays.asList(book_categories).indexOf(selectedCategories.get(i)));

        user.setCategories(pref_categories_Int);

        return user;

    }

    public static boolean onNavigationItemSelected(Activity activity, ViewPager viewPager, MenuItem item, Context context, DrawerLayout drawer, int callingActivity){
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(id == callingActivity){
            //Toast.makeText(context,"DO NOTHING",Toast.LENGTH_SHORT).show();
            return true; // do nothing
        }

        if (id == R.id.drawer_navigation_profile) {
            Intent i = new Intent(context, TabbedShowProfileActivity.class);
            i.putExtra(context.getString(R.string.user_profile_data_key), NavigationDrawerManager.getUserParcelable(context));
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(i);
        }
        else if (id == R.id.drawer_navigation_shareBook) {

            Intent i = new Intent(context, ShareBookActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(i);
        } else if (id == R.id.drawer_navigation_myBookPendingRequest && !(activity instanceof MyBookActivity)) {

            Intent my_books = new Intent(context, MyBookActivity.class);
            my_books.putExtra("openedFromDrawer",true);
            my_books.putExtra("showPageNumFromDrawer",1);
            my_books.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(my_books);

        }else if (id == R.id.drawer_navigation_myBookPendingRequest && (activity instanceof MyBookActivity)) {

            viewPager.setCurrentItem(1);

        }else if (id == R.id.drawer_navigation_myBookExchanges && !(activity instanceof MyBookActivity)) {

            Intent my_books = new Intent(context, MyBookActivity.class);
            my_books.putExtra("openedFromDrawer",true);
            my_books.putExtra("showPageNumFromDrawer",2);
            my_books.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(my_books);

        }else if (id == R.id.drawer_navigation_myBookExchanges && (activity instanceof MyBookActivity)) {

            viewPager.setCurrentItem(2);

        } else if (id == R.id.drawer_navigation_search) {

            Intent search = new Intent(context, SearchActivity.class);
            search.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            context.startActivity(search);

        }else if (id == R.id.drawer_navigation_logout) {

            AuthUI.getInstance()
                    .signOut(context)
                    .addOnCompleteListener(task -> {
                        Intent i = new Intent(context, SplashScreenActivity.class);
                        context.startActivity(i);
                        OneSignal.setSubscription(false);
                        Toast.makeText(context, context.getString(R.string.log_out), Toast.LENGTH_SHORT).show();

                        //clear all fragments from back stack
                        ((FragmentActivity)activity).getSupportFragmentManager().popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

                        activity.finish();
                    });

        }


        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

}
