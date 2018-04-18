package it.polito.mad.sharenbook;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.firebase.ui.auth.data.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.sharenbook.model.UserProfile;

import static android.content.ContentValues.TAG;

public class SplashScreenActivity extends Activity {

    private static final int RC_SIGN_IN = 123;

    /** Firebase variables */
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private DatabaseReference dbReference;
    private FirebaseDatabase firebaseDatabase;
    private FirebaseUser firebaseUser;

    private UserProfile user;

    /**default profile values*/
    private String default_city;
    private String default_bio;
    private String default_email;
    private String default_fullname;
    private String default_username;
    private String default_picture_path;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        /** Set default values */
        default_city = getString(R.string.default_city);
        default_bio = getString(R.string.default_bio);
        default_email = getString(R.string.default_email);
        default_fullname = getString(R.string.default_fullname_heading);
        default_username = getString(R.string.default_username_heading);
        default_picture_path = getString(R.string.default_picture_path);

        firebaseInitAndReading();

    }


    /**
     * firebase init and reading method
     */
    private void firebaseInitAndReading(){

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        if(firebaseUser != null) { /** User is signed in -> just check if his profile is completed or not*/

            FirebaseStorage storage = FirebaseStorage.getInstance();
            StorageReference storageRef = storage.getReference();
            firebaseDatabase = FirebaseDatabase.getInstance();

            /** Retrieve User profile data */
            dbReference = firebaseDatabase.getReference(getString(R.string.users_key)).child(firebaseUser.getUid()).child(getString(R.string.profile_key));

            dbReference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    Object profileData = dataSnapshot.getValue();

                    if(profileData == null){ //TODO check if this part is necessary or not
                        Log.d("ERROR: ", "Database not consistent! New account for this user.");

                        Map<String,Object> users_dataPlaceholder = new HashMap<String,Object>();
                        Map<String,Object> profile_books = new HashMap<String,Object>();

                        /**
                         * Create user to pass it in the bundle for EditProfile
                         */

                        String photoUri;
                        if(firebaseUser.getPhotoUrl() == null){
                            photoUri = default_picture_path;
                        } else {
                            photoUri = firebaseUser.getPhotoUrl().toString();
                        }

                        UserProfile user = new UserProfile(firebaseUser.getUid(),
                                firebaseUser.getDisplayName(), default_username, firebaseUser.getEmail(),
                                default_city, default_bio,
                                photoUri
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
                                    //i.putExtra("from","signup");
                                    startActivity(i);
                                    finish();

                                }else {

                                    Toast.makeText(getApplicationContext(), "ERROR: backend database error", Toast.LENGTH_SHORT).show();
                                    finish();
                                }
                            }
                        });

                    }

                    else if (dataSnapshot.getValue().equals(getString(R.string.profile_value_placeholder))) {

                        /**
                         * Profile is empty -> start EditProfile
                         */

                        //Load user data from the registration account used
                        String photoUri;
                        if(firebaseUser.getPhotoUrl() == null){
                            photoUri = default_picture_path;
                        } else {
                            photoUri = firebaseUser.getPhotoUrl().toString();
                        }

                        user = new UserProfile(firebaseUser.getUid(),
                                firebaseUser.getDisplayName(), default_username, firebaseUser.getEmail(),
                                default_city, default_bio,
                                photoUri
                        );

                        Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
                        i.putExtra(getString(R.string.user_profile_data_key), user);
                        startActivity(i);
                        finish();

                    }else {

                        /**
                         * Profile is completed -> start ShowProfile
                         */

                        user = dataSnapshot.getValue(UserProfile.class);

                        user.setUserID(firebaseUser.getUid());

                        storageRef.child("images/"+user.getUserID()+".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                user.setPicture_uri(uri);
                                Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                                i.putExtra(getString(R.string.user_profile_data_key), user);
                                startActivity(i);
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception exception) {
                                user.setPicture_uri(Uri.parse(default_picture_path));
                                Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                                i.putExtra(getString(R.string.user_profile_data_key), user);
                                startActivity(i);
                                finish();
                            }
                        });

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

            /**
             * The user is not signed-in or logged-in
             */

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


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        /**
         * Here from Sign_In or Log_In
         */

        if (requestCode == RC_SIGN_IN) {

            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {

                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                if(firebaseUser != null) {

                    /**
                     * In this case the user has correctly done the SIGN_IN or LOG_IN ->
                     * in any case he should go to EditProfile only when the profile is empty
                     * otherwise go to ShowProfile
                     */

                    FirebaseStorage storage = FirebaseStorage.getInstance();
                    StorageReference storageRef = storage.getReference();

                    firebaseDatabase = FirebaseDatabase.getInstance();
                    dbReference = firebaseDatabase.getReference(getString(R.string.users_key)).child(firebaseUser.getUid()).child(getString(R.string.profile_key));

                    dbReference.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {

                            Object profileData = dataSnapshot.getValue();

                            if(profileData == null) {

                                /**
                                 * The user has just done the Registration -> create new profile on the DB
                                 */

                                Map<String,Object> users_dataPlaceholder = new HashMap<String,Object>();
                                Map<String,Object> profile_books = new HashMap<String,Object>();

                                /**
                                 * Create user to pass it in the bundle for EditProfile
                                 */

                                String photoUri;
                                if(firebaseUser.getPhotoUrl() == null){
                                    photoUri = default_picture_path;
                                } else {
                                    photoUri = firebaseUser.getPhotoUrl().toString();
                                }

                                user = new UserProfile(firebaseUser.getUid(),
                                        firebaseUser.getDisplayName(), default_username, firebaseUser.getEmail(),
                                        default_city, default_bio,
                                        photoUri
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
                                            startActivity(i);
                                            finish();

                                        }else {

                                            Toast.makeText(getApplicationContext(), "ERROR: backend database error", Toast.LENGTH_SHORT).show();
                                            finish();
                                        }
                                    }
                                });


                            } else {
                                /**
                                 * The user has just done the Login -> just check if profile is empty or not
                                 */
                                if (profileData.equals(getString(R.string.profile_value_placeholder))) {


                                    String photoUri;
                                    if(firebaseUser.getPhotoUrl() == null){
                                        photoUri = default_picture_path;
                                    } else {
                                        photoUri = firebaseUser.getPhotoUrl().toString();
                                    }

                                    user = new UserProfile(firebaseUser.getUid(),
                                            firebaseUser.getDisplayName(), default_username, firebaseUser.getEmail(),
                                            default_city, default_bio,
                                            photoUri
                                    );

                                    Intent i = new Intent(getApplicationContext(), EditProfileActivity.class);
                                    i.putExtra(getString(R.string.user_profile_data_key), user);
                                    startActivity(i);
                                    finish();

                                }else {

                                    user = dataSnapshot.getValue(UserProfile.class);

                                    user.setUserID(firebaseUser.getUid());

                                    storageRef.child("images/"+user.getUserID()+".jpg").getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                        @Override
                                        public void onSuccess(Uri uri) {
                                            user.setPicture_uri(uri);
                                            Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                                            i.putExtra(getString(R.string.user_profile_data_key), user);
                                            startActivity(i);
                                            finish();
                                        }
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception exception) {
                                            user.setPicture_uri(Uri.parse(default_picture_path));
                                            Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                                            i.putExtra(getString(R.string.user_profile_data_key), user);
                                            startActivity(i);
                                            finish();
                                        }
                                    });

                                }

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

                }

            } else{

                /** Signin or Login failed */

                if (response == null) {
                    // User pressed back button
                    //showSnackbar(R.string.sign_in_cancelled);
                    Toast.makeText(this, "Sign-In has been canceled", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    Toast.makeText(this, "ERROR: No internet connection!", Toast.LENGTH_SHORT).show();
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
