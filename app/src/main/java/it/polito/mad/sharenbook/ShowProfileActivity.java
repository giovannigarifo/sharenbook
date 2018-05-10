package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mancj.materialsearchbar.MaterialSearchBar;
import com.mikhaellopez.circularimageview.CircularImageView;

import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.UserInterface;
import it.polito.mad.sharenbook.model.UserProfile;


public class ShowProfileActivity  extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MaterialSearchBar.OnSearchActionListener {

    /**
     * views
     **/
    private TextView tv_userFullName, tv_userNickName, tv_userRatingInfo,
            tv_userCityContent, tv_userBioContent, tv_userEmailContent;

    private BottomNavigationView navBar;

    private FloatingActionButton goEdit_button;

    private CircularImageView userPicture;

    private String searchState;

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
     * isValid values returned by called activities
     **/
    private static final int EDIT_RETURN_VALUE = 1;

    private UserProfile user;

    private String profile_picture_signature;

    /** DRAWER AND SEARCHBAR **/
    private MaterialSearchBar searchBar;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private View nav;
    private TextView drawer_fullname;
    private TextView drawer_email;
    private CircularImageView drawer_userPicture;


    /**
     * onCreate callback
     *
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
        getViews();

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

        /* set drawer **/

        setDrawer();

        /*
         * set texts
         */
        UserInterface.TextViewFontResize(user.getFullname().length(), getWindowManager(), tv_userFullName);
        tv_userFullName.setText(user.getFullname());
        tv_userNickName.setText(user.getUsername());
        tv_userCityContent.setText(user.getCity());
        tv_userBioContent.setText(user.getBio());
        tv_userEmailContent.setText(user.getEmail());

        /*
         * goEdit_Button
         */
        goEdit_button.setOnClickListener(v -> {

            Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
            i.putExtra(getString(R.string.user_profile_data_key), user);
            startActivityForResult(i, EDIT_RETURN_VALUE);

        });

        setupNavbar();

        /*
         * SearchBar
         */
        //setupSearchBar(findViewById(R.id.searchBar));

        searchStatusCheck(data);


    }

    private void searchStatusCheck(Bundle data){
        if(data.getString("searchState")!=null) {
            searchState = data.getString("searchState");
            if (searchState.equals("enabled")) {
                navBar.setVisibility(View.GONE);
                goEdit_button.setVisibility(View.GONE);
            } else {
                navBar.setVisibility(View.VISIBLE);
                goEdit_button.setVisibility(View.VISIBLE);
            }
        }
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
                case R.id.navigation_profile:
                    break;

                case R.id.navigation_search:
                    startSearchActivity(null);
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
        outState.putString("searchState",searchState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        searchState = savedInstanceState.getString("searchState");
    }

    /**
     * onActivityResult callback
     *
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        String default_picture_path = "void";

        if (requestCode == EDIT_RETURN_VALUE) {

            if (resultCode == RESULT_OK) {

                Bundle userData = data.getExtras();
                user = userData.getParcelable(getString(R.string.user_profile_data_key));


                /* update user info in nav drawer */
                NavigationDrawerManager.setDrawerViews(getApplicationContext(),
                        getWindowManager(),drawer_fullname,drawer_email,drawer_userPicture,NavigationDrawerManager.getNavigationDrawerProfile());

                navigationView.setCheckedItem(R.id.drawer_navigation_profile);


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



                /*
                 * set texts
                 */
                UserInterface.TextViewFontResize(user.getFullname().length(), getWindowManager(), tv_userFullName);
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
     * Starts the search activity with the appropriate bundle
     */
    private void startSearchActivity(CharSequence searchInputText){
        Intent i = new Intent(getApplicationContext(), SearchActivity.class);
        if(searchInputText!=null)
            i.putExtra("searchInputText",searchInputText);
        startActivity(i);
    }


    private void setDrawer(){

        /* DRAWER AND SEARCHBAR */

        drawer =  findViewById(R.id.show_profile_drawer_layout);
        navigationView =  findViewById(R.id.show_profile_nav_view);
        searchBar =  findViewById(R.id.searchBar);

        navigationView.setCheckedItem(R.id.drawer_navigation_profile);
        navigationView.setNavigationItemSelectedListener(ShowProfileActivity.this);
        searchBar.setOnSearchActionListener(ShowProfileActivity.this);

        nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        drawer_email = nav.findViewById(R.id.drawer_user_email);

        NavigationDrawerManager.setDrawerViews(getApplicationContext(),
                getWindowManager(),drawer_fullname,drawer_email,drawer_userPicture,NavigationDrawerManager.getNavigationDrawerProfile());
    }


    /**
     * getViews method
     */
    private void getViews() {

        //get views
        tv_userFullName = findViewById(R.id.tv_userFullName);
        tv_userNickName = findViewById(R.id.tv_userNickName);
        tv_userRatingInfo = findViewById(R.id.tv_userRatingInfo);

        tv_userCityContent = findViewById(R.id.tv_userCityContent);
        tv_userBioContent = findViewById(R.id.tv_userBioContent);
        tv_userEmailContent = findViewById(R.id.tv_userEmailContent);

        // set views text
        UserInterface.TextViewFontResize(user.getFullname().length(), getWindowManager(), tv_userFullName);
        tv_userFullName.setText(user.getFullname());
        tv_userNickName.setText(user.getUsername());

        tv_userCityContent.setText(user.getCity());
        tv_userBioContent.setText(user.getBio());
        tv_userEmailContent.setText(user.getEmail());
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.show_profile_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);

        } else {
            super.onBackPressed();

        }
        navigationView.setCheckedItem(R.id.drawer_navigation_profile);

    }


    /**
     * Navigation Drawer Listeners
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.drawer_navigation_shareBook) {
            Intent i = new Intent(getApplicationContext(), ShareBookActivity.class);
            startActivity(i);
        } else if (id == R.id.drawer_navigation_myBook) {
            Intent my_books = new Intent(getApplicationContext(), MyBookActivity.class);
            startActivity(my_books);
        } else if (id == R.id.drawer_navigation_logout) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(task -> {
                        Intent i = new Intent(getApplicationContext(), SplashScreenActivity.class);
                        startActivity(i);
                        Toast.makeText(getApplicationContext(), getString(R.string.log_out), Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }

        DrawerLayout drawer = findViewById(R.id.show_profile_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onSearchStateChanged(boolean enabled) {
        searchState = enabled ? "enabled" : "disabled";
        Log.d("debug", "Search " + searchState);
        if( searchState.equals("enabled")) {
            navBar.setVisibility(View.GONE);
            goEdit_button.setVisibility(View.GONE);
        }
        else {
            navBar.setVisibility(View.VISIBLE);
            goEdit_button.setVisibility(View.VISIBLE);
        }

    }

    //send intent to SearchActivity
    @Override
    public void onSearchConfirmed(CharSequence searchInputText) {

        startSearchActivity(searchInputText);
    }

    @Override
    public void onButtonClicked(int buttonCode) {
        switch (buttonCode){
            case MaterialSearchBar.BUTTON_NAVIGATION:
                drawer.openDrawer(Gravity.START);
                break;
            case MaterialSearchBar.BUTTON_SPEECH:
                break;
            case MaterialSearchBar.BUTTON_BACK:
                searchBar.disableSearch();
                break;
        }

    }
}