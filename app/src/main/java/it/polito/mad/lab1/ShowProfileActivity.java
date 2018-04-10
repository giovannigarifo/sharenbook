package it.polito.mad.lab1;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.mikhaellopez.circularimageview.CircularImageView;

public class ShowProfileActivity extends Activity {

    //views
    private TextView tv_userFullName, tv_userNickName, tv_userRatingInfo,
            tv_userCityHeading, tv_userBioHeading, tv_userEmailHeading,
            tv_userCityContent, tv_userBioContent, tv_userEmailContent;

    private BottomNavigationView navBar;

    private FloatingActionButton goEdit_button;

    private CircularImageView userPicture;

    //key value database
    private SharedPreferences editedProfile;

    //Firebase references
    FirebaseDatabase firedb;

    //default profile values
    private String default_city;
    private String default_bio;
    private String default_email;
    private String default_fullname;
    private String default_username;
    private String default_picture_path;

    //result values returned by called activities
    private static final int EDIT_RETURN_VALUE = 1;

    private int widthT = 700;

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


        /**
         * Firebase Connection test: it works.
         *
        firedb = FirebaseDatabase.getInstance();
        DatabaseReference dbRef = firedb.getReference(); //url inferred from google-services.json

        //obtain unique key
        DatabaseReference itemRef = dbRef.push();

        //push value
        itemRef.setValue("Hello World");
        */



        //retrieve the shared preference file
        editedProfile = context.getSharedPreferences(getString(R.string.profile_preferences), Context.MODE_PRIVATE);

        //retrieve the default values
        default_city = context.getResources().getString(R.string.default_city);
        default_bio = context.getResources().getString(R.string.default_bio);
        default_email = context.getResources().getString(R.string.default_email);
        default_fullname = context.getResources().getString(R.string.default_fullname_heading);
        default_username = context.getResources().getString(R.string.default_username_heading);
        default_picture_path = context.getResources().getString(R.string.default_picture_path);

        //modify default typography
        getViewsAndSetTypography();

        //get references to UI elements
        goEdit_button = (FloatingActionButton) findViewById(R.id.fab_edit);
        navBar = (BottomNavigationView) findViewById(R.id.navigation);
        userPicture = (CircularImageView) findViewById(R.id.userPicture);

        /**
         * userPicture
         */

        //set user picture
        final String choosenPicture = editedProfile.getString(getString(R.string.userPicture_key), default_picture_path);
        if (!choosenPicture.equals(getString(R.string.default_picture_path)))
            userPicture.setImageURI(Uri.parse(choosenPicture));

        //register callback that start the showPicture activity when user clicks the photo
        userPicture.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
            i.putExtra("PicturePath", choosenPicture);
            startActivity(i);
        });


        /**
         * goEdit_Button
         */
        goEdit_button.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
            startActivityForResult(i, EDIT_RETURN_VALUE);
        });


        /**
         * navBar
         */

        //set navigation_profile as selected item
        navBar.setSelectedItemId(R.id.navigation_profile);

        //set the listener for the navigation bar items
        navBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_showcase:
                    Toast.makeText(getApplicationContext(), "Selected Showcase!", Toast.LENGTH_SHORT).show();
                    break;

                case R.id.navigation_profile:
                    break;

                case R.id.navigation_shareBook:
                    Intent i = new Intent(getApplicationContext(), ShareBookActivity.class);
                    startActivity(i);
                    finish();
                    break;
            }
            return true;
        });

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

                fullNameResize();
                tv_userFullName.setText(editedProfile.getString(getString(R.string.fullname_key), default_fullname));
                tv_userNickName.setText(editedProfile.getString(getString(R.string.username_key), default_username));
                tv_userCityContent.setText(editedProfile.getString(getString(R.string.city_key), default_city));
                tv_userBioContent.setText(editedProfile.getString(getString(R.string.bio_key), default_bio));
                tv_userEmailContent.setText(editedProfile.getString(getString(R.string.email_key), default_email));

                final String choosenPicture = editedProfile.getString(getString(R.string.userPicture_key), default_picture_path);

                if (!choosenPicture.equals(default_picture_path))
                    userPicture.setImageURI(Uri.parse(choosenPicture));

                userPicture.setOnClickListener(v -> {

                            Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
                            i.putExtra("PicturePath", choosenPicture);
                            startActivity(i);
                        }
                );
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
    private void fullNameResize() {

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Log.d("Metrics:", "width:" + metrics.widthPixels);

        if (metrics.densityDpi != metrics.DENSITY_HIGH || metrics.widthPixels < widthT) {

            int fullname_lenght = editedProfile.getString(getString(R.string.fullname_key), default_fullname).length();

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
     *
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
        fullNameResize();
        tv_userFullName.setText(editedProfile.getString(getString(R.string.fullname_key), default_fullname));

        tv_userNickName.setTypeface(robotoLight);
        tv_userNickName.setText(editedProfile.getString(getString(R.string.username_key), default_username));
        tv_userRatingInfo.setTypeface(robotoLight);

        //headings
        tv_userCityHeading.setTypeface(robotoBold);
        tv_userBioHeading.setTypeface(robotoBold);
        tv_userEmailHeading.setTypeface(robotoBold);

        //contents
        tv_userCityContent.setTypeface(robotoLight);
        tv_userCityContent.setText(editedProfile.getString(getString(R.string.city_key), default_city));

        tv_userBioContent.setTypeface(robotoLight);
        tv_userBioContent.setText(editedProfile.getString(getString(R.string.bio_key), default_bio));

        tv_userEmailContent.setTypeface(robotoLight);
        tv_userEmailContent.setText(editedProfile.getString(getString(R.string.email_key), default_email));
    }

}