package it.polito.mad.sharenbook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mikhaellopez.circularimageview.CircularImageView;

import it.polito.mad.sharenbook.model.UserProfile;

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
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseUser firebaseUser;

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

    private UserProfile user;

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
         * User creation
         */

        Bundle data = getIntent().getExtras();
        user = data.getParcelable(getString(R.string.user_profile_data_key));

        if(user.getPicture_uri() != null) {
            userPicture.setImageURI(user.getPicture_uri());
            userPicture.setOnClickListener(v -> {
                Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
                i.putExtra("PicturePath", user.getPicture_uri().toString());
                startActivity(i);
            });
        }

        /**
         * set texts
         */
        fullNameResize();
        tv_userFullName.setText(user.getFullname());
        tv_userNickName.setText(user.getUsername());
        tv_userCityContent.setText(user.getCity());
        tv_userBioContent.setText(user.getBio());
        tv_userEmailContent.setText(user.getEmail());

        //user = new UserProfile();

        //firebaseInitAndReading();



        /**
         * userPicture
         */

        //set user picture
        final String choosenPicture;



        //= editedProfile.getString(getString(R.string.userPicture_key), default_picture_path);
        //if (!choosenPicture.equals(getString(R.string.default_picture_path)))
        //    userPicture.setImageURI(Uri.parse(choosenPicture));

        //register callback that start the showPicture activity when user clicks the photo
       // userPicture.setOnClickListener(v -> {
         //   Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
         //   i.putExtra("PicturePath", choosenPicture);
         //   startActivity(i);
       // });





        /*

        final String choosenPicture = editedProfile.getString(getString(R.string.userPicture_key), default_picture_path);

        if (!choosenPicture.equals(default_picture_path))
            userPicture.setImageURI(Uri.parse(choosenPicture));

        userPicture.setOnClickListener(v -> {

                    Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
                    i.putExtra("PicturePath", choosenPicture);
                    startActivity(i);
                }
        );
*/
        /**
         * goEdit_Button
         */
        goEdit_button.setOnClickListener(v -> {


            /**
             *   Create User Object
             */

/*
            UserProfile user = new UserProfile(
                    firebaseUser.getUid(),
                    editedProfile.getString(getString(R.string.fullname_key), default_fullname),
                    null,
                    editedProfile.getString(getString(R.string.email_key), default_email),
                    null,null,
                    choosenPicture.toString()
            );
*/
            Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
            i.putExtra(getString(R.string.user_profile_data_key),user);
            i.putExtra("from","profile");
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
                    //Toast.makeText(getApplicationContext(), "Selected Showcase!", Toast.LENGTH_SHORT).show();
                    AuthUI.getInstance()
                            .signOut(this)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                public void onComplete(@NonNull Task<Void> task) {
                                    Intent i = new Intent(getApplicationContext(), SplashScreenActivity.class);
                                    startActivity(i);
                                    Toast.makeText(getApplicationContext(), "Signed Out!", Toast.LENGTH_SHORT).show();
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
            }
            return true;
        });





    }

    /**
     * firebase init and reading method
     */
    private void firebaseInitAndReading(){

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        firebaseDatabase = FirebaseDatabase.getInstance();


        databaseReference = firebaseDatabase.getReference(getString(R.string.users_key)).child(firebaseUser.getUid()).child(getString(R.string.profile_key));

        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                user = dataSnapshot.getValue(UserProfile.class);
                user.setUserID(firebaseUser.getUid());


                if(user.getPicture_uri() != null) {
                    userPicture.setImageURI(user.getPicture_uri());
                    userPicture.setOnClickListener(v -> {
                        Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
                        i.putExtra("PicturePath", user.getPicture_uri().toString());
                        startActivity(i);
                    });
                }
                user.setPicture_uri(Uri.parse(default_picture_path));
                /**
                 * set texts
                 */
                fullNameResize();
                tv_userFullName.setText(user.getFullname());
                tv_userNickName.setText(user.getUsername());
                tv_userCityContent.setText(user.getCity());
                tv_userBioContent.setText(user.getBio());
                tv_userEmailContent.setText(user.getEmail());

                Log.d("DATA:",user.getUserID());
                Log.d("DATA:",user.getFullname());
                Log.d("DATA:",user.getUsername());
                Log.d("DATA:",user.getEmail());
                Log.d("DATA:",user.getCity());
                Log.d("DATA:",user.getBio());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

                if(databaseError != null){
                    Toast.makeText(getApplicationContext(), "ERROR: backend database error", Toast.LENGTH_SHORT).show();
                    finish();
                }

            }
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