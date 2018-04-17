package it.polito.mad.sharenbook;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.sharenbook.model.UserProfile;

import static android.content.ContentValues.TAG;

public class SplashScreenActivity extends Activity {

    private static final int RC_SIGN_IN = 123;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseDatabase firebaseDB;
    private DatabaseReference dbReference;

    //Firebase references
    //private DatabaseReference databaseReference;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseUser firebaseUser;

    private UserProfile user;

    //default profile values
    private String default_city;
    private String default_bio;
    private String default_email;
    private String default_fullname;
    private String default_username;
    private String default_picture_path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

         /**
           Verify if the user is already signed-in

         */

        //user = new UserProfile();

        default_city = getString(R.string.default_city);
        default_bio = getString(R.string.default_bio);
        default_email = getString(R.string.default_email);
        default_fullname = getString(R.string.default_fullname_heading);
        default_username = getString(R.string.default_username_heading);
        default_picture_path = getString(R.string.default_picture_path);

        firebaseInitAndReading();



/*
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if(firebaseAuth.getCurrentUser() != null) {

                    Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                    i.putExtra(getString(R.string.uid_key),firebaseAuth.getCurrentUser().getUid());
                    Log.d("UID:",firebaseAuth.getCurrentUser().getUid());
                    Log.d("UID:",firebaseAuth.getCurrentUser().getProviderId());

                    startActivity(i);
                    finish();

                }else {
                    // User not signed in
                    startActivityForResult(
                            // Get an instance of AuthUI based on the default app
                            AuthUI.getInstance().createSignInIntentBuilder()
                                    .setAvailableProviders(Arrays.asList(
                                            new AuthUI.IdpConfig.GoogleBuilder().build(),
                                            new AuthUI.IdpConfig.FacebookBuilder().build(),
                                            new AuthUI.IdpConfig.TwitterBuilder().build(),
                                            new AuthUI.IdpConfig.EmailBuilder().build()))
                                    .setLogo(R.mipmap.ic_launcher)
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

*/
    }

    private void checkAuth(){

        firebaseAuth = FirebaseAuth.getInstance();


        if(firebaseAuth.getCurrentUser()!=null){
            /**
             *  User already signed in
            */

            /**
             *  check se il valore che ha per chiave la uid corrente è empty profile -> EditProfile
             *  altrimenti ShowProfile
             */

            String userId = firebaseAuth.getCurrentUser().getUid();

            /*
            DatabaseReference userRef = firebaseDB.getReference().child(getString(R.string.users_key)).child(userId);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    String profileState = (String) dataSnapshot.getValue();

                    if(profileState.equals("empty_profile")){
                        //User has not set profile data -> EditProfileActivity
                        //TODO Complete this part
                        Log.d("ERROR", "You must complete your profile creation before continuing!");

                    } else {
                        //User has a valid profile -> ShowProfileActivity

                        Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                        i.putExtra(getString(R.string.uid_key), userId);
                        startActivity(i);
                        finish();

                    }

                }

                @Override
                public void onCancelled(DatabaseError Error) {

                }
            });

*/

            Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
            i.putExtra(getString(R.string.uid_key), userId);
            startActivity(i);
            finish();

        }else {
            // User not signed in
            startActivityForResult(
                    // Get an instance of AuthUI based on the default app
                    AuthUI.getInstance().createSignInIntentBuilder()
                            .setAvailableProviders(Arrays.asList(
                                    new AuthUI.IdpConfig.GoogleBuilder().build(),
                                    new AuthUI.IdpConfig.FacebookBuilder().build(),
                                    new AuthUI.IdpConfig.TwitterBuilder().build(),
                                    new AuthUI.IdpConfig.EmailBuilder().build()))
                            .setLogo(R.mipmap.ic_launcher)
                            .build(),
                    RC_SIGN_IN);
        }

    }

    /**
     * firebase init and reading method
     */
    private void firebaseInitAndReading(){

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        if(firebaseUser != null) {


            //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            firebaseDatabase = FirebaseDatabase.getInstance();


            dbReference = firebaseDatabase.getReference(getString(R.string.users_key)).child(firebaseUser.getUid()).child(getString(R.string.profile_key));

            dbReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {


                    if (dataSnapshot.getValue().equals(getString(R.string.profile_value_placeholder))) {

                        user = new UserProfile(firebaseUser.getUid(),default_fullname,default_username,default_email,
                                default_city,default_bio,default_picture_path);
                        Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
                        i.putExtra(getString(R.string.user_profile_data_key), user);
                        i.putExtra("from", "splash");
                        startActivity(i);
                        finish();

                    }else {

                        user = dataSnapshot.getValue(UserProfile.class);

                        user.setUserID(firebaseUser.getUid());

                        user.setPicture_uri(Uri.parse(getString(R.string.default_picture_path)));


                        Log.d("DATA:", user.getUserID());
                        Log.d("DATA:", user.getFullname());
                        Log.d("DATA:", user.getUsername());
                        Log.d("DATA:", user.getEmail());
                        Log.d("DATA:", user.getCity());
                        Log.d("DATA:", user.getBio());

                        Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                        i.putExtra(getString(R.string.user_profile_data_key), user);
                        i.putExtra("from", "splash");
                        startActivity(i);
                        finish();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                    if (databaseError != null) {
                        Toast.makeText(getApplicationContext(), "ERROR: backend database error", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                }
            });


        }else {
            // User not signed in
            startActivityForResult(
                    // Get an instance of AuthUI based on the default app
                    AuthUI.getInstance().createSignInIntentBuilder()
                            .setAvailableProviders(Arrays.asList(
                                    new AuthUI.IdpConfig.GoogleBuilder().build(),
                                    new AuthUI.IdpConfig.FacebookBuilder().build(),
                                    new AuthUI.IdpConfig.TwitterBuilder().build(),
                                    new AuthUI.IdpConfig.EmailBuilder().build()))
                            .setLogo(R.mipmap.ic_launcher)
                            .build(),
                    RC_SIGN_IN);
        }


    }
/*
    @Override
    protected void onStart() {
        super.onStart();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (authStateListener != null) {
            firebaseAuth.removeAuthStateListener(authStateListener);
        }
    }


*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {

            IdpResponse response = IdpResponse.fromResultIntent(data);


            // Successfully signed in

            if (resultCode == RESULT_OK) {


                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();



                if(firebaseUser != null) {

                    Map<String,Object> users_dataPlaceholder = new HashMap<String,Object>();
                    Map<String,Object> profile_books = new HashMap<String,Object>();

                    /**
                     * CREATE USER TO PASS IN THE BUNDLE FOR EDIT_PROFILE
                     */

                    UserProfile user = new UserProfile(firebaseUser.getUid(),
                            firebaseUser.getDisplayName(), null, firebaseUser.getEmail(),
                            null, null,
                            firebaseUser.getPhotoUrl().toString()
                            );

                    users_dataPlaceholder.put(firebaseUser.getUid(), getString(R.string.empty_user_value));
                    profile_books.put(getString(R.string.profile_key),getString(R.string.profile_value_placeholder));
                    profile_books.put(getString(R.string.user_books_key),getString(R.string.users_books_placeholder));

                    dbReference = FirebaseDatabase.getInstance().getReference(getString(R.string.users_key));

                    dbReference.updateChildren(users_dataPlaceholder, new DatabaseReference.CompletionListener() {

                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {

                            if(databaseError == null) {
                                dbReference.child(firebaseUser.getUid()).updateChildren(profile_books);
                                Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
                                i.putExtra(getString(R.string.user_profile_data_key),user);
                                i.putExtra("from","signup");
                                startActivity(i);
                                finish();

                            }else {

                                Toast.makeText(getApplicationContext(), "ERROR: backend database error", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        }
                    });
/*
                    Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
                    i.putExtra(getString(R.string.user_profile_data_key),user);
                    i.putExtra("from","signup");
                    startActivity(i);
                    finish();
                    */
                }
            } else{

            //if (resultCode != RESULT_OK){
                // Sign in failed

                if (response == null) {
                    // User pressed back button
                    //showSnackbar(R.string.sign_in_cancelled);
                    Toast.makeText(this, "ERROR: Sign-In Failed", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, "ERROR: Sign-In Failed 2", Toast.LENGTH_SHORT).show();
                    //showSnackbar(R.string.no_internet_connection);
                    return;
                }

                Toast.makeText(this, "ERROR: Unknown error", Toast.LENGTH_SHORT).show();
                //showSnackbar(R.string.unknown_error);
                Log.e(TAG, "Sign-in error: ", response.getError());

            }

        }
    }

}
