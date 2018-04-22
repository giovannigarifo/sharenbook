package it.polito.mad.sharenbook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;

import it.polito.mad.sharenbook.Utils.UserInterface;
import it.polito.mad.sharenbook.model.UserProfile;

public class ShowProfileActivity extends Activity {

    /**
     * views
     **/

    private TextView tv_userFullName, tv_userNickName, tv_userRatingInfo,
            tv_userCityHeading, tv_userBioHeading, tv_userEmailHeading,
            tv_userCityContent, tv_userBioContent, tv_userEmailContent;

    private BottomNavigationView navBar;

    private FloatingActionButton goEdit_button;

    private CircularImageView userPicture;

    /**
     * default profile values
     **/

    private String default_city;
    private String default_bio;
    private String default_email;
    private String default_fullname;
    private String default_username;
    private String default_picture_timestamp;

    /**
     * result values returned by called activities
     **/
    private static final int EDIT_RETURN_VALUE = 1;

    private int widthT = 700;

    private UserProfile user;

    private String profile_picture_signature;


    /**
     * onCreate callback
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE); //disable title bar
        setContentView(R.layout.activity_show_profile); //load view
        Context context = this.getApplicationContext(); //retrieve context

        //retrieve the default values
        default_city = context.getResources().getString(R.string.default_city);
        default_bio = context.getResources().getString(R.string.default_bio);
        default_email = context.getResources().getString(R.string.default_email);
        default_fullname = context.getResources().getString(R.string.default_fullname_heading);
        default_username = context.getResources().getString(R.string.default_username_heading);
        default_picture_timestamp = context.getResources().getString(R.string.default_picture_path);

        /* Take user data from the bundle */
        Bundle data;
        if (savedInstanceState == null) //ShowProfile is started by SplashActivity
            data = getIntent().getExtras();
        else                            //otherwise landascape -> portrait or viceversa
            data = savedInstanceState;

        user = data.getParcelable(getString(R.string.user_profile_data_key));

        //modify default typography
        getViewsAndSetTypography();

        //get references to UI elements
        goEdit_button = findViewById(R.id.fab_edit);
        navBar = findViewById(R.id.navigation);
        userPicture = findViewById(R.id.userPicture);

        //Set profile picture
        profile_picture_signature = user.getPicture_timestamp();

            if (!profile_picture_signature.equals(default_picture_timestamp)) {
                StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/"+user.getUserID()+".jpg");
                UserInterface.showGlideImage(getApplicationContext(), storageRef, userPicture,  Long.valueOf(profile_picture_signature));

                userPicture.setOnClickListener(v -> {
                    Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
                    i.putExtra("PictureSignature", profile_picture_signature);
                    i.putExtra("userId", user.getUserID());
                    startActivity(i);
                });

        }

        /**
         * set texts
         */
        fullNameResize(user);
        tv_userFullName.setText(user.getFullname());
        tv_userNickName.setText(user.getUsername());
        tv_userCityContent.setText(user.getCity());
        tv_userBioContent.setText(user.getBio());
        tv_userEmailContent.setText(user.getEmail());


        /**
         * userPicture
         */

        //set user picture
        final String choosenPicture;

        /**
         * goEdit_Button
         */
        goEdit_button.setOnClickListener(v -> {

            Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
            i.putExtra(getString(R.string.user_profile_data_key), user);
            startActivityForResult(i, EDIT_RETURN_VALUE);

        });


        setupNavbar();


    }

    /**
     * navBar
     */

    private void setupNavbar() {


        //set navigation_profile as selected item
        navBar.setSelectedItemId(R.id.navigation_profile);


        //set the listener for the navigation bar items
        navBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_logout:
                    //Toast.makeText(getApplicationContext(), "Selected Showcase!", Toast.LENGTH_SHORT).show();
                    AuthUI.getInstance()
                            .signOut(this)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                public void onComplete(@NonNull Task<Void> task) {
                                    Intent i = new Intent(getApplicationContext(), SplashScreenActivity.class);
                                    startActivity(i);
                                    Toast.makeText(getApplicationContext(), getString(R.string.log_out), Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            });

                    break;

                case R.id.navigation_profile:
                    break;

                case R.id.navigation_shareBook:
                    Intent i = new Intent(getApplicationContext(), ShareBookActivity.class);
                    startActivity(i);
                    break;
                case R.id.navigation_myBook:
                    Intent my_books = new Intent(getApplicationContext(), MyBookActivity.class);
                    startActivity(my_books);
                    break;
            }
            return true;
        });


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState); //the activity is going to be destroyed I need to save user
        outState.putParcelable(getString(R.string.user_profile_data_key), user);
    }

    /**
     * onActivityResult callback
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        String default_picture_path = "void";

        if (requestCode == EDIT_RETURN_VALUE) {

            if (resultCode == RESULT_OK) {

                Bundle userData = data.getExtras();
                user = userData.getParcelable(getString(R.string.user_profile_data_key));

                profile_picture_signature = user.getPicture_timestamp();

                if (!profile_picture_signature.equals(default_picture_path)) {

                        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/"+user.getUserID()+".jpg");
                        UserInterface.showGlideImage(getApplicationContext(), storageRef, userPicture,  Long.valueOf(profile_picture_signature));

                        userPicture.setOnClickListener(v -> {
                            Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
                            i.putExtra("PictureSignature", profile_picture_signature);
                            i.putExtra("userId", user.getUserID());
                            startActivity(i);
                        });

                }

                /**
                 * set texts
                 */
                fullNameResize(user);
                tv_userFullName.setText(user.getFullname());
                tv_userNickName.setText(user.getUsername());
                tv_userCityContent.setText(user.getCity());
                tv_userBioContent.setText(user.getBio());
                tv_userEmailContent.setText(user.getEmail());
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        navBar.setSelectedItemId(R.id.navigation_profile);
    }

    /**
     * fullNameResize method
     */
    private void fullNameResize(UserProfile user) {

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Log.d("Metrics:", "width:" + metrics.widthPixels);

        if (metrics.densityDpi != metrics.DENSITY_HIGH || metrics.widthPixels < widthT) {

            int fullname_lenght = user.getFullname().length();

            if (fullname_lenght <= 16) {
                tv_userFullName.setTextSize(2, 24);
            } else if (fullname_lenght > 16 && fullname_lenght <= 22) {
                tv_userFullName.setTextSize(2, 18);
            } else {
                tv_userFullName.setTextSize(2, 14);
            }
        }

    }


    /**
     * getViewsAndSetTypography method
     */
    private void getViewsAndSetTypography() {

        //get views
        tv_userFullName = (TextView) findViewById(R.id.tv_userFullName);
        tv_userNickName = (TextView) findViewById(R.id.tv_userNickName);
        tv_userRatingInfo = (TextView) findViewById(R.id.tv_userRatingInfo);

        tv_userCityHeading = (TextView) findViewById(R.id.tv_userCityHeading);
        tv_userBioHeading = (TextView) findViewById(R.id.tv_userBioHeading);
        tv_userEmailHeading = (TextView) findViewById(R.id.tv_userEmailHeading);
        tv_userCityContent = (TextView) findViewById(R.id.tv_userCityContent);
        tv_userBioContent = (TextView) findViewById(R.id.tv_userBioContent);
        tv_userEmailContent = (TextView) findViewById(R.id.tv_userEmailContent);

        //retrieve fonts
        Typeface robotoBold = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");
        Typeface robotoLight = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

        /**
         * set views font and view text
         */

        tv_userFullName.setTypeface(robotoBold);
        fullNameResize(user);
        tv_userFullName.setText(user.getFullname());

        tv_userNickName.setTypeface(robotoLight);
        tv_userNickName.setText(user.getUsername());
        tv_userRatingInfo.setTypeface(robotoLight);

        //headings
        tv_userCityHeading.setTypeface(robotoBold);
        tv_userBioHeading.setTypeface(robotoBold);
        tv_userEmailHeading.setTypeface(robotoBold);

        //contents
        tv_userCityContent.setTypeface(robotoLight);
        tv_userCityContent.setText(user.getCity());

        tv_userBioContent.setTypeface(robotoLight);
        tv_userBioContent.setText(user.getBio());

        tv_userEmailContent.setTypeface(robotoLight);
        tv_userEmailContent.setText(user.getEmail());
    }

}