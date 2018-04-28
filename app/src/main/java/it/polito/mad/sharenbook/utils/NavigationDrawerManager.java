package it.polito.mad.sharenbook.utils;

import android.content.Context;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;

import it.polito.mad.sharenbook.model.UserProfile;

public class NavigationDrawerManager {

    private static NavigationDrawerProfile navigationDrawerProfile;

    public static void setNavigationDrawerProfileByUser (UserProfile user){

        if(navigationDrawerProfile == null) { // if not exits creates it
            navigationDrawerProfile = new NavigationDrawerProfile(user);
        }else { //otherwise the method is used to overwrite the fields
            navigationDrawerProfile.setUser_fullname(user.getFullname());
            navigationDrawerProfile.setUser_email(user.getEmail());
            navigationDrawerProfile.setUser_picturePath("images/" + user.getUserID() + ".jpg");
            navigationDrawerProfile.setPictureSignature(user.getPicture_timestamp());


        }


    }

    public static void setNavigationDrawerProfileByFields(String name,String email,String userID, String pictureSignature){

        if(navigationDrawerProfile == null) {
            navigationDrawerProfile = new NavigationDrawerProfile(name,email,"images/" + userID + ".jpg",pictureSignature);
        }else {
            navigationDrawerProfile.setUser_fullname(name);
            navigationDrawerProfile.setUser_email(email);
            navigationDrawerProfile.setUser_picturePath("images/" + userID + ".jpg");
            navigationDrawerProfile.setPictureSignature(pictureSignature);

        }

    }

    public static NavigationDrawerProfile getNavigationDrawerProfile(){
        return navigationDrawerProfile;
    }

    public static void setDrawerViews(Context context,WindowManager windowManager, TextView drawer_fullname, TextView drawer_email, CircularImageView drawer_userPicture,NavigationDrawerProfile navigationDrawerProfile){

        String profile_picture_signature = navigationDrawerProfile.getPictureSignature();
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
}
