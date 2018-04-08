package it.polito.mad.lab1;

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
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mikhaellopez.circularimageview.CircularImageView;

public class showProfile extends Activity {

    //views
    private TextView tv_userFullName, tv_userNickName, tv_userRatingInfo,
            tv_userCityHeading, tv_userBioHeading, tv_userEmailHeading, tv_userCityContent, tv_userBioContent, tv_userEmailContent;

    private BottomNavigationView navBar;

    private FloatingActionButton goEdit_button;

    //preferences

    private SharedPreferences editedProfile;

    //default profile values
    private String default_city;
    private String default_bio;
    private String default_email;
    private String default_fullname;
    private String default_username;
    private String default_picture_path;

    private CircularImageView userPicture;


    //result values returned by called activities
    private static final int EDIT_RETURN_VALUE =1;

    private int widthT=700;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_show_profile);

        /*
            SharedPreferences: verify if the profile has been edited otherwise set default values
        */

        //retrieve the shared preference file
        Context context = this.getApplicationContext();
        editedProfile = context.getSharedPreferences(
                getString(R.string.profile_preferences), Context.MODE_PRIVATE);



        //retrieve the default values if the profile is not yet edited
        default_city = context.getResources().getString(R.string.default_city);
        default_bio = context.getResources().getString(R.string.default_bio);
        default_email = context.getResources().getString(R.string.default_email);
        default_fullname = context.getResources().getString(R.string.default_fullname_heading);
        default_username = context.getResources().getString(R.string.default_username_heading);
        default_picture_path = context.getResources().getString(R.string.default_picture_path);






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

        //get view button
        goEdit_button = (FloatingActionButton) findViewById(R.id.fab_edit);

        //set views font to Roboto and text
        Typeface robotoBold = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");
        tv_userFullName.setTypeface(robotoBold);
        fullNameResize();
        tv_userFullName.setText(editedProfile.getString(getString(R.string.fullname_key),default_fullname));

        tv_userCityHeading.setTypeface(robotoBold);
        tv_userBioHeading.setTypeface(robotoBold);
        tv_userEmailHeading.setTypeface(robotoBold);

        Typeface robotoLight = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");
        tv_userNickName.setTypeface(robotoLight);
        tv_userNickName.setText(editedProfile.getString(getString(R.string.username_key),default_username));

        tv_userRatingInfo.setTypeface(robotoLight);

        tv_userCityContent.setTypeface(robotoLight);
        tv_userCityContent.setText(editedProfile.getString(getString(R.string.city_key),default_city));

        tv_userBioContent.setTypeface(robotoLight);
        tv_userBioContent.setText(editedProfile.getString(getString(R.string.bio_key),default_bio));

        tv_userEmailContent.setTypeface(robotoLight);
        tv_userEmailContent.setText(editedProfile.getString(getString(R.string.email_key),default_email));

        navBar = (BottomNavigationView) findViewById(R.id.navigation);

        userPicture = (CircularImageView) findViewById(R.id.userPicture);
        final String choosenPicture = editedProfile.getString(getString(R.string.userPicture_key),default_picture_path);

        if(!choosenPicture.equals(getString(R.string.default_picture_path)))
            userPicture.setImageURI(Uri.parse(choosenPicture));

        userPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i= new Intent(getApplicationContext(),showPicture.class);

                i.putExtra("PicturePath",choosenPicture);

                startActivity(i);
            }
        });

        //set navigation_profile as selected item
        navBar.setSelectedItemId(R.id.navigation_profile);

        //set the listened for the navigation bar items
        navBar.setOnNavigationItemSelectedListener(
            new BottomNavigationView.OnNavigationItemSelectedListener() {
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_showcase:
                        Toast.makeText(getApplicationContext(), "Selected Showcase!", Toast.LENGTH_SHORT).show();
                        break;

                    case R.id.navigation_profile:
                        Toast.makeText(getApplicationContext(), "Selected Profile!", Toast.LENGTH_SHORT).show();
                        break;

                    case R.id.navigation_shareBook:
                        Toast.makeText(getApplicationContext(), "Selected Share Book!", Toast.LENGTH_SHORT).show();
                        break;
                }
                return true;
            }
        });


        //set the listener for the edit button
        goEdit_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i=new Intent(getApplicationContext(),editProfile.class);


                startActivityForResult(i, EDIT_RETURN_VALUE);
            }
        });





    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){

        super.onActivityResult(requestCode,resultCode,data);

        String default_picture_path="void";
        if(requestCode== EDIT_RETURN_VALUE){
            if(resultCode==RESULT_OK){

                fullNameResize();
                tv_userFullName.setText(editedProfile.getString(getString(R.string.fullname_key),default_fullname));
                tv_userNickName.setText(editedProfile.getString(getString(R.string.username_key),default_username));
                tv_userCityContent.setText(editedProfile.getString(getString(R.string.city_key),default_city));
                tv_userBioContent.setText(editedProfile.getString(getString(R.string.bio_key),default_bio));
                tv_userEmailContent.setText(editedProfile.getString(getString(R.string.email_key),default_email));

               final String choosenPicture = editedProfile.getString(getString(R.string.userPicture_key),default_picture_path);

                if(!choosenPicture.equals(default_picture_path))
                    userPicture.setImageURI(Uri.parse(choosenPicture));
                userPicture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i= new Intent(getApplicationContext(),showPicture.class);

                        i.putExtra("PicturePath",choosenPicture);

                        startActivity(i);
                    }
                });
            }
        }

    }

    private void fullNameResize(){

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);


        Log.d("Metrics:", "width:"+metrics.widthPixels);

        if(metrics.densityDpi != metrics.DENSITY_HIGH || metrics.widthPixels<widthT){
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


}