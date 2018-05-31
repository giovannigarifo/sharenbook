package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.onesignal.OneSignal;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.sharenbook.utils.ConnectionChangedListener;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.model.UserProfile;

import static android.content.ContentValues.TAG;
import static it.polito.mad.sharenbook.utils.NetworkUtilities.checkNetworkConnection;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;

    /* Firebase variables */
    private FirebaseAuth firebaseAuth;
    private DatabaseReference dbReference;
    private static FirebaseDatabase firebaseDatabase;
    private FirebaseUser firebaseUser;
    private StorageReference storageReference;

    private UserProfile user;

    /* Default profile values*/
    private String default_city;
    private String default_bio;
    private String default_username;
    private String default_picture_signature;

    private SharedPreferences usernamePref;
    private SharedPreferences userProfileData;
    private SharedPreferences.Editor write_userProfileData;

    private static ConnectionChangedListener connListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /* Set default values */
        default_city = getString(R.string.default_city);
        default_bio = getString(R.string.default_bio);
        default_username = getString(R.string.default_username_heading);
        default_picture_signature = getString(R.string.default_picture_path);

        usernamePref = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);

        userProfileData = getSharedPreferences(getString(R.string.userData_preferences), Context.MODE_PRIVATE);
        write_userProfileData = userProfileData.edit();

        initFirebase();

        buildInternetRequestDialog();

        /*
         * Set a listener for network connection loss during authentication operations
         */
        checkNetworkConnection();

       /* connListener = new ConnectionChangedListener() {
            @Override
            public void OnConnectionStateChanged() {
                if(!NetworkUtilities.isConnected()){
                    internetRequest.show();
                }
            }
        };
        NetworkUtilities.addConnectionStateListener(connListener);
*/


        /*
         * Execute User authentication
         */
        checkAuthentication();

    }


    /**
     * Method for Firebase Initialization
     */
    private void initFirebase(){
        /*Database*/
        firebaseDatabase = FirebaseDatabase.getInstance();

        /*Authentication*/
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        /*Storage*/
        storageReference = FirebaseStorage.getInstance().getReference();


    }


    /**
     * This method manages User Authentication
     */
    private void checkAuthentication(){

        if(firebaseUser != null) { /* User is already Logged in -> just check if his profile is completed or not*/

            /* Retrieve User profile data */
            dbReference = firebaseDatabase.getReference(getString(R.string.users_key)).child(firebaseUser.getUid()).child(getString(R.string.profile_key));
            dbReference.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    Object profileData = dataSnapshot.getValue();

                    /*In this case the user is registered but there is not an associated profile in firebase DB*/
                    if(profileData == null){

                        Log.d("WARNING: ", "Database not consistent! New account for this user.");
                        createNewProfile();

                    }
                    else if (dataSnapshot.getValue().equals(getString(R.string.profile_value_placeholder))) {
                        /* Profile is empty -> start EditProfile */

                        createDefaultUserProfile();
                        goEditProfile();

                    }else {
                        /* Profile is completed -> start ShowProfile */
                        user = dataSnapshot.getValue(UserProfile.class);    //take user data
                        user.setUserID(firebaseUser.getUid());
                        usernamePref.edit().putString(getString(R.string.username_copy_key), user.getUsername()).commit();

                        goShowCase();

                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                    if (databaseError != null) {
                        Toast.makeText(getApplicationContext(), getString(R.string.internal_error), Toast.LENGTH_SHORT).show();
                        finish();
                    }

                }
            });

        }else {
            /* The user is not signed-in or logged-in -> Show the Authentication UI */
            startActivityForResult(
                    AuthUI.getInstance().createSignInIntentBuilder()                    // Get an instance of AuthUI
                            .setAvailableProviders(Arrays.asList(
                                    new AuthUI.IdpConfig.GoogleBuilder().build(),
                                    new AuthUI.IdpConfig.TwitterBuilder().build(),
                                    new AuthUI.IdpConfig.EmailBuilder().build()))
                            .setLogo(R.mipmap.ic_launcher)
                            .build(),
                    RC_SIGN_IN);
        }

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /* Here from Sign_In or Log_In */
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {

                firebaseUser = firebaseAuth.getCurrentUser();

                if(firebaseUser != null) {
                    /*
                     * In this case the user has correctly done the SIGN_IN or LOG_IN ->
                     * in any case he should go to EditProfile only when the profile is empty
                     * otherwise go to ShowProfile
                     */
                    // Read user profile data from DB
                    dbReference = firebaseDatabase.getReference(getString(R.string.users_key)).child(firebaseUser.getUid()).child(getString(R.string.profile_key));
                    dbReference.addListenerForSingleValueEvent(new ValueEventListener() {

                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            Object profileData = dataSnapshot.getValue();

                            if(profileData == null) {

                                /* The user has just done the Registration -> create new profile on the DB */
                                createNewProfile();

                            } else {
                                /* The user has just done the Login -> check if profile is empty or not */

                                if (profileData.equals(getString(R.string.profile_value_placeholder))) {

                                    createDefaultUserProfile();
                                    goEditProfile();

                                }else {
                                    /*Login succesfull and profile not empty */

                                    user = dataSnapshot.getValue(UserProfile.class);
                                    usernamePref.edit().putString(getString(R.string.username_copy_key), user.getUsername()).commit();
                                    user.setUserID(firebaseUser.getUid());

                                    OneSignal.sendTag("User_ID", user.getUsername());  //let this user be identified on oneSignal
                                    OneSignal.setSubscription(true);

                                    goShowCase();

                                }
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            if (databaseError != null) {
                                Toast.makeText(getApplicationContext(), getString(R.string.internal_error), Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }

                    });

                }

            } else {
                /* Signin or Login failed */
                if (response == null) { // User pressed back button
                    Toast.makeText(this, getString(R.string.sign_in_cancelled), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(this, getString(R.string.unknown_error), Toast.LENGTH_SHORT).show();
                Log.e(TAG, getString(R.string.unknown_error), response.getError());
            }

        }

    }


    private void createNewProfile(){

        Map<String,Object> users_dataPlaceholder = new HashMap<>();
        Map<String,Object> user_entries = new HashMap<>();

        createDefaultUserProfile();

        users_dataPlaceholder.put(firebaseUser.getUid(), getString(R.string.empty_user_value));
        user_entries.put(getString(R.string.profile_key),getString(R.string.profile_value_placeholder));
        user_entries.put(getString(R.string.user_books_key),getString(R.string.users_books_placeholder));
        user_entries.put(getString(R.string.user_favorites_key),getString(R.string.users_favorites_placeholder));

        dbReference = FirebaseDatabase.getInstance().getReference(getString(R.string.users_key));
        dbReference.updateChildren(users_dataPlaceholder, (databaseError, databaseReference) -> {

            if(databaseError == null) {
                dbReference.child(firebaseUser.getUid()).updateChildren(user_entries);
                goEditProfile();

            }else {
                Toast.makeText(getApplicationContext(), getString(R.string.internal_error), Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }


    private void goShowCase(){

        /* Take the profile picture signature from the storage */
        storageReference = FirebaseStorage.getInstance().getReference();
        StorageReference profile_pic_ref = storageReference.child("images/"+user.getUsername()+".jpg");

        Task task = profile_pic_ref.getMetadata();
        task.addOnSuccessListener(result -> {
            StorageMetadata metadata = (StorageMetadata) result;
            user.setPicture_timestamp(String.valueOf(metadata.getCreationTimeMillis()));
            saveDataAndShowCase();
        });
        task.addOnFailureListener(exception -> {
            user.setPicture_timestamp(default_picture_signature);
            saveDataAndShowCase();
        });

    }

    private void saveDataAndShowCase(){
        /* Save user data in shared preferences */
        write_userProfileData.putString(getString(R.string.username_pref), user.getUsername()).commit();
        write_userProfileData.putString(getString(R.string.uid_pref), user.getUserID()).commit();
        write_userProfileData.putString(getString(R.string.bio_pref), user.getBio()).commit();
        write_userProfileData.putString(getString(R.string.city_pref), user.getCity()).commit();
        write_userProfileData.putString(getString(R.string.email_pref), user.getEmail()).commit();
        write_userProfileData.putString(getString(R.string.fullname_pref), user.getFullname()).commit();
        write_userProfileData.putString(getString(R.string.picture_pref), user.getPicture_timestamp()).commit();
        String[] bookCategories = getResources().getStringArray(R.array.book_categories);
        write_userProfileData.putString(getString(R.string.categories_pref), user.getCategoriesAsString(bookCategories)).commit();

        NavigationDrawerManager.setNavigationDrawerProfileByUser(user);  //init NavigationDrawerProfile
        Intent i = new Intent(getApplicationContext(), ShowCaseActivity.class);
        startActivity(i);
        finish();
    }


    /** Start EditProfile and pass the user data */
    private void goEditProfile(){
        Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
        i.putExtra(getString(R.string.user_profile_data_key),user);
        startActivity(i);
        finish();
    }


    /** Set default values for user profile */
    private void createDefaultUserProfile(){
        /*
         * Create user to be passed in the bundle to activities
         */
        user = new UserProfile(firebaseUser.getUid(),
                firebaseUser.getDisplayName(), default_username, firebaseUser.getEmail(),
                default_city, default_bio,
                default_picture_signature
        );
    }


    private void buildInternetRequestDialog(){
        AlertDialog.Builder internetRequest = new AlertDialog.Builder(SplashScreenActivity.this);
        internetRequest.setTitle(R.string.no_internet_connection);
        internetRequest.setMessage(R.string.network_alert);
        internetRequest.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
                    startActivityForResult(settingsIntent, 9003);
                }
        ).setNegativeButton(android.R.string.cancel,
                (dialog, which) -> dialog.dismiss()
        );
    }

}


