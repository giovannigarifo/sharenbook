package it.polito.mad.sharenbook;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.content.res.AppCompatResources;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import it.polito.mad.sharenbook.Utils.ImageUtils;
import it.polito.mad.sharenbook.Utils.UserInterface;
import it.polito.mad.sharenbook.model.UserProfile;


public class EditProfileActivity extends Activity {

    //context of the activity
    private Context context;

    ProgressDialog progressDialog = null;

    // request codes to edit user photo
    private static final int REQUEST_CAMERA = ImageUtils.REQUEST_CAMERA;
    private static final int REQUEST_GALLERY = ImageUtils.REQUEST_GALLERY;
    private static final int MULTIPLE_PERMISSIONS = 3;

    //views
    private TextView tv_userFullNameHeading_edit, tv_userNameHeading_edit, tv_userEmailHeading_edit, tv_userCityHeading_edit, tv_userBioHeading_edit;
    private EditText et_userFullName, et_userNickName, et_userCity, et_userBio, et_userEmail;
    private FloatingActionButton save_button;
    private FloatingActionButton fab_editPhoto;

    /* user photo views */
    private com.mikhaellopez.circularimageview.CircularImageView userPicture;

    //Shared Preferences
    private SharedPreferences editedProfile_copy;
    private SharedPreferences.Editor writeProfile_copy;

    //default profile values
    private String default_city;
    private String default_bio;
    private String default_email;
    private String default_fullname;
    private String default_username;
    private String default_picture_path;
    private String fullname;

    private String username;
    private String email;
    private String city;
    private String bio;

    //Required Permissions
    private String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    //Regex for input validation
    private Pattern RFC822_email_regex = Pattern.compile("(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*:(?:(?:\\r\\n)?[ \\t])*(?:(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*)(?:,\\s*(?:(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*|(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)*\\<(?:(?:\\r\\n)?[ \\t])*(?:@(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*(?:,@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*)*:(?:(?:\\r\\n)?[ \\t])*)?(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\"(?:[^\\\"\\r\\\\]|\\\\.|(?:(?:\\r\\n)?[ \\t]))*\"(?:(?:\\r\\n)?[ \\t])*))*@(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*)(?:\\.(?:(?:\\r\\n)?[ \\t])*(?:[^()<>@,;:\\\\\".\\[\\] \\000-\\031]+(?:(?:(?:\\r\\n)?[ \\t])+|\\Z|(?=[\\[\"()<>@,;:\\\\\".\\[\\]]))|\\[([^\\[\\]\\r\\\\]|\\\\.)*\\](?:(?:\\r\\n)?[ \\t])*))*\\>(?:(?:\\r\\n)?[ \\t])*))*)?;\\s*)");

    private Pattern fullname_regex = Pattern.compile("([A-Z]([a-z])*[\\s][A-Za-z]([a-z])*[\\s]?)([A-Za-z]([a-z])*[\\s]?)*");

    private Pattern city_regex = Pattern.compile("([A-Z][a-z]*)[\\s]?[,]?[\\s]?([A-Z][a-z]*)?[\\s]?");

    //User profile data
    private UserProfile user;

    //Firebase References
    private DatabaseReference dbReference;
    private StorageReference storageReference;

    // ImageUtils for image handling
    private ImageUtils imageUtils;

    /**
     * onCreate callback
     *
     * @param savedInstanceState
     */
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_edit_profile);

        if (getCallingActivity() == null && savedInstanceState == null) {
            AlertDialog.Builder completeProfileAlert = new AlertDialog.Builder(EditProfileActivity.this); //give a context to Dialog
            completeProfileAlert.setTitle(R.string.complete_profile_title);
            completeProfileAlert.setMessage(R.string.complete_profile_rational);
            completeProfileAlert.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                        hasPermissions();
                    }
            );

            completeProfileAlert.show();
        } else
            hasPermissions();

        //modify default typography
        getViewsAndSetTypography();

        context = this.getApplicationContext();

        //retrieve the default values if the profile is not yet edited
        default_city = context.getResources().getString(R.string.default_city);
        default_bio = context.getResources().getString(R.string.default_bio);
        default_email = context.getResources().getString(R.string.default_email);
        default_fullname = context.getResources().getString(R.string.default_fullname);
        default_username = context.getResources().getString(R.string.default_username);
        default_picture_path = context.getResources().getString(R.string.default_picture_path);

        // Create a storage reference from our app
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        /* Get user Data from the Intent Extras*/
        Bundle data = getIntent().getExtras();
        user = data.getParcelable(getString(R.string.user_profile_data_key));

        // Get the database reference to this User Data
        dbReference = FirebaseDatabase.getInstance().getReference(getString(R.string.users_key)).child(user.getUserID());

        // Initialize image class
        imageUtils = new ImageUtils(this);

        // Methods that implements the final part of onCreate
        if ((savedInstanceState == null) || (savedInstanceState.isEmpty())) { //First time make copies and visualize stable profile
            onCreateWithBundleEmpty(user);
        } else {
            onCreateWithBundleNotEmpty();

            // retrieve currentPhotoUri for imageUtils class
            Uri currentPhotoUri = Uri.parse(savedInstanceState.getString("currentPhotoUri"));
            imageUtils.setCurrentPhotoUri(currentPhotoUri);
        }
    }


    /**
     * onCreateWithBundleEmpty method
     */
    private void onCreateWithBundleEmpty(UserProfile user) {

        // Copy for rollbacks
        editedProfile_copy = context.getSharedPreferences(getString(R.string.profile_preferences_copy), Context.MODE_PRIVATE);
        writeProfile_copy = editedProfile_copy.edit();

        save_button = findViewById(R.id.fab_save);

        /* String fullname */
        if (user.getFullname() != null && !user.getFullname().equals(default_fullname)) {
            fullname = user.getFullname();
            et_userFullName.setText(fullname);
        } else {
            fullname = default_fullname;
            et_userFullName.setHint(fullname);
        }
        writeProfile_copy.putString(getString(R.string.fullname_copy_key), fullname).commit();


        /* String username */
        if (user.getUsername() != null && !user.getUsername().equals(default_username)) {
            username = user.getUsername();
            et_userNickName.setText(username);
        } else {
            username = default_username;
            et_userNickName.setHint(username);
        }
        writeProfile_copy.putString(getString(R.string.username_copy_key), username).commit();


        /* String city */
        if (user.getCity() != null && !user.getCity().equals(default_city)) {
            city = user.getCity();
            et_userCity.setText(city);
        } else {
            city = default_city;
            et_userCity.setHint(city);
        }
        writeProfile_copy.putString(getString(R.string.city_copy_key), city).commit();


        /* String bio */
        if (user.getBio() != null && !user.getBio().equals(default_bio)) {
            bio = user.getBio();
            et_userBio.setText(bio);
        } else {
            bio = default_bio;
            et_userBio.setHint(bio);
        }
        writeProfile_copy.putString(getString(R.string.bio_copy_key), bio).commit();

        /* String email */
        if (user.getEmail() != null && !user.getEmail().equals(default_email)) {
            email = user.getEmail();
            et_userEmail.setText(email);
        } else {
            email = default_email;
            et_userEmail.setHint(email);
        }
        writeProfile_copy.putString(getString(R.string.email_copy_key), email).commit();


        /* Edit photo section */
        userPicture = findViewById(R.id.userPicture_edit);

        // Load photo from Firebase using Glide cache
        if (!user.getPicture_timestamp().equals(default_picture_path)) {
            UserInterface.showGlideImage(getApplicationContext(), storageReference.child("images/" + user.getUserID() + ".jpg"), userPicture, Long.valueOf(user.getPicture_timestamp()));
        }

        fab_editPhoto = findViewById(R.id.fab_editPhoto);
        fab_editPhoto.setBackgroundDrawable(AppCompatResources.getDrawable(EditProfileActivity.this, R.drawable.ic_check_black_24dp));

        fab_editPhoto.setOnClickListener(v -> {
            hasPermissions();
            imageUtils.showSelectImageDialog();
        });
    }


    /**
     * onCreateWithBundleNotEmpty method
     */
    private void onCreateWithBundleNotEmpty() {

        //make a copy for rollbacks
        editedProfile_copy = context.getSharedPreferences(getString(R.string.profile_preferences_copy), Context.MODE_PRIVATE);
        writeProfile_copy = editedProfile_copy.edit();

        //get view button
        save_button = findViewById(R.id.fab_save);

        //set hints for views
        et_userFullName.setHint(editedProfile_copy.getString(getString(R.string.fullname_copy_key), default_fullname));
        et_userNickName.setHint(editedProfile_copy.getString(getString(R.string.username_copy_key), default_username));
        et_userCity.setHint(editedProfile_copy.getString(getString(R.string.city_copy_key), default_city));
        String actualBio = editedProfile_copy.getString(getString(R.string.bio_copy_key), default_bio);

        if (actualBio.equals(default_bio))
            et_userBio.setHint(actualBio);
        else {
            et_userBio.setText(actualBio);
            et_userBio.setTextColor(Color.GRAY);
        }

        et_userEmail.setHint(editedProfile_copy.getString(getString(R.string.email_copy_key), default_email));

        /* Edit photo section */
        userPicture = findViewById(R.id.userPicture_edit);

        // Load photo from local if modified or from Firebase using Glide cache
        String chosenPicture = editedProfile_copy.getString(getString(R.string.userPicture_copy_key), default_picture_path);
        if (editedProfile_copy.getBoolean(getString(R.string.changed_photo_flag_key), false)) {
            userPicture.setImageURI(Uri.parse(chosenPicture));
        } else if (!user.getPicture_timestamp().equals(default_picture_path)) {
            UserInterface.showGlideImage(getApplicationContext(), storageReference.child("images/" + user.getUserID() + ".jpg"), userPicture, Long.valueOf(user.getPicture_timestamp()));
        }

        fab_editPhoto = findViewById(R.id.fab_editPhoto);
        fab_editPhoto.setOnClickListener(v -> {
            hasPermissions();
            imageUtils.showSelectImageDialog();
        });
    }


    /**
     * hasPermissions method
     */
    private void hasPermissions() {

        int result;

        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String permission : permissions) {
            result = ContextCompat.checkSelfPermission(EditProfileActivity.this, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
        }
    }


    /**
     * onRequestPermissionsResult method
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permissions granted.
                    Toast.makeText(getApplicationContext(), getString(R.string.permissions_granted), Toast.LENGTH_LONG).show();
                } else {

                    final List<String> neededPermissions = new ArrayList<>();
                    for (String permission : permissions) {

                        if (ActivityCompat.shouldShowRequestPermissionRationale(EditProfileActivity.this, permission)) {
                            neededPermissions.add(permission);
                        }
                    }

                    if (!neededPermissions.isEmpty()) {

                        AlertDialog.Builder newPermissionRequest = new AlertDialog.Builder(EditProfileActivity.this); //give a context to Dialog
                        newPermissionRequest.setTitle(R.string.new_permission_request_title);
                        newPermissionRequest.setMessage(R.string.permissions_rationale);
                        newPermissionRequest.setPositiveButton(android.R.string.ok, (dialog, which) -> ActivityCompat.requestPermissions(EditProfileActivity.this, neededPermissions.toArray(new String[neededPermissions.size()]), MULTIPLE_PERMISSIONS)
                        ).setNegativeButton(android.R.string.cancel,
                                (dialog, which) -> finish()
                        );

                        newPermissionRequest.show();
                    }

                }
            }
        }
    }


    /**
     * onActivityResult method
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_CAMERA) {

                imageUtils.dispatchCropCurrentPhotoIntent(ImageUtils.ASPECT_RATIO_CIRCLE);

            } else if (requestCode == REQUEST_GALLERY) {

                imageUtils.dispatchCropPhotoIntent(data.getData(), ImageUtils.ASPECT_RATIO_CIRCLE);

            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

                Uri resultUri = CropImage.getActivityResult(data).getUri();

                try {
                    Uri resizedPhotoUri = ImageUtils.resizeJpegPhoto(this, resultUri, 600, 0);

                    userPicture.setImageURI(resizedPhotoUri);

                    writeProfile_copy.putString(getString(R.string.userPicture_copy_key), resizedPhotoUri.toString()).commit();
                    writeProfile_copy.putBoolean(getString(R.string.changed_photo_flag_key), true).commit();

                } catch (IOException e) {
                    Log.d("error", "IOException when retrieving resized image Uri");
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * onResume method
     */
    @Override
    protected void onResume() {
        super.onResume();

        /*               edit text             */
        setEditTextListeners(et_userFullName, R.string.fullname_key, R.string.fullname_copy_key);
        setEditTextListeners(et_userNickName, R.string.username_key, R.string.username_copy_key);
        setEditTextListeners(et_userCity, R.string.city_key, R.string.city_copy_key);
        setEditTextListeners(et_userBio, R.string.bio_key, R.string.bio_copy_key);
        setEditTextListeners(et_userEmail, R.string.email_key, R.string.email_copy_key);

        //save on click
        save_button.setOnClickListener(view -> {

            if (!validateForm())
                return;

            firebaseSaveProfile();

            /*if(NetworkUtilities.isConnected()) {

                if (!validateForm())
                    return;

                firebaseSaveProfile();
            } else {
                AlertDialog.Builder internetRequest = new AlertDialog.Builder(EditProfileActivity.this);
                internetRequest.setTitle(R.string.no_internet_connection);
                internetRequest.setMessage(R.string.network_alert);
                internetRequest.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        Intent settingsIntent = new Intent(Settings.ACTION_SETTINGS);
                        startActivityForResult(settingsIntent, 9003);
                        }
                ).setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> dialog.dismiss()
                );

                internetRequest.show();
            }*/
        });

    }


    private void firebaseSaveProfile() {

        Map<String, Object> userData = new HashMap<>();
        if (et_userFullName.getText().length() != 0 && !et_userFullName.getText().equals(default_fullname)) {
            userData.put(getString(R.string.fullname_key), et_userFullName.getText().toString());
            user.setFullname(et_userFullName.getText().toString());
        }
        if (et_userNickName.getText().length() != 0 && !et_userNickName.getText().equals(default_username)) {
            userData.put(getString(R.string.username_key), et_userNickName.getText().toString());
            user.setUsername(et_userNickName.getText().toString());
        }
        if (et_userEmail.getText().length() != 0 && !et_userEmail.getText().equals(default_email)) {
            userData.put(getString(R.string.email_key), et_userEmail.getText().toString());
            user.setEmail(et_userEmail.getText().toString());
        }
        if (et_userCity.getText().length() != 0 && !et_userCity.getText().equals(default_city)) {
            userData.put(getString(R.string.city_key), et_userCity.getText().toString());
            user.setCity(et_userCity.getText().toString());
        }
        if (et_userBio.getText().length() != 0 && !et_userBio.getText().equals(default_bio)) {
            userData.put(getString(R.string.bio_key), et_userBio.getText().toString());
            user.setBio(et_userBio.getText().toString());
        }


        dbReference.child(getString(R.string.profile_key)).updateChildren(userData, (databaseError, databaseReference) -> {

            if (databaseError == null) {

                /*
                 * Check if profile picture has been changed or not
                 */
                if (editedProfile_copy.getBoolean(getString(R.string.changed_photo_flag_key), false)) {

                    Uri picturePath = Uri.parse(editedProfile_copy.getString(getString(R.string.userPicture_copy_key), default_picture_path));
                    uploadFile(picturePath);

                } else {
                    /*
                     * The user has not changed the profile picture
                     */

                    Toast.makeText(getApplicationContext(), getString(R.string.profile_saved), Toast.LENGTH_LONG).show();

                    Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                    i.putExtra(getString(R.string.user_profile_data_key), user);

                    if (getCallingActivity() != null) {  //if it was a StartActivityForResult then -> null
                        setResult(RESULT_OK, i);
                    } else {
                        startActivity(i);
                    }
                    writeProfile_copy.clear().commit();
                    finish();

                }

            }

        });

    }


    /**
     * validateForm method
     *
     * @return
     */
    private boolean validateForm() {
        boolean result = true;


        if (!(et_userFullName.getText().toString().equals(fullname)) && !(et_userFullName.getText().toString().isEmpty())) {
            if ((et_userFullName.getText().toString().length()) < 5) {
                et_userFullName.setError(getString(R.string.name_length_format_rationale));
                result = false;

            } else if (!fullname_regex.matcher(et_userFullName.getText().toString()).matches()) {
                et_userFullName.setError(getString(R.string.name_bad_format_rationale));
                result = false;
            } else {
                et_userFullName.setError(null);
            }

        } else if (et_userFullName.getText().toString().isEmpty()) {
            et_userFullName.setError(getString(R.string.required_field));
            result = false;
        }

        if (!(et_userNickName.getText().toString().equals(username)) && !(et_userNickName.getText().toString().isEmpty())) {

            if ((TextUtils.getTrimmedLength(et_userNickName.getText().toString())) < 2) {
                et_userNickName.setError(getString(R.string.username_bad_lenght_rationale));
                result = false;
            } else if (!(et_userNickName.getText().toString().startsWith("@"))) {
                et_userNickName.setError(getString(R.string.username_bad_start_rationale));
                result = false;
            } else {
                et_userNickName.setError(null);
            }

        } else if (et_userNickName.getText().toString().isEmpty()) {
            et_userNickName.setError(getString(R.string.required_field));
            result = false;
        }

        if (!(et_userEmail.getText().toString().equals(email)) && !(et_userEmail.getText().toString().isEmpty())) { //the mail has been changed
            Log.d("Email:", et_userEmail.getText().toString());

            if (!RFC822_email_regex.matcher(et_userEmail.getText().toString()).matches()) {
                et_userEmail.setError(getString(R.string.bad_email));
                result = false;

            } else {
                et_userEmail.setError(null);
            }
        } else if (TextUtils.isEmpty(et_userEmail.getText().toString())) {
            et_userEmail.setError(getString(R.string.required_field));
            result = false;
        }

        if (!(et_userCity.getText().toString().equals(city)) && !(et_userCity.getText().toString().isEmpty())) {

            if ((TextUtils.getTrimmedLength(et_userCity.getText().toString())) < 2) {
                et_userCity.setError(getString(R.string.city_bad_lenght_rationale));
                result = false;
            } else if (!(city_regex.matcher(et_userCity.getText().toString()).matches())) {
                et_userCity.setError(getString(R.string.city_bad_format_rationale));
                result = false;
            } else {
                et_userCity.setError(null);
            }


        } else if (TextUtils.isEmpty(et_userCity.getText().toString())) {
            et_userCity.setError(getString(R.string.required_field));
            result = false;
        }

        if (TextUtils.isEmpty(et_userBio.getText().toString())) {
            et_userBio.setError(getString(R.string.required_field));
            result = false;
        }

        return result;
    }


    /**
     * setEditTextListeners method
     *
     * @param et
     * @param key
     * @param keycopy
     */
    private void setEditTextListeners(final EditText et, final int key, final int keycopy) {

        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (et.equals(et_userBio))
                    et.setTextColor(Color.DKGRAY);

                writeProfile_copy.putString(getString(keycopy), et.getText().toString()).commit();
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle save) {
        super.onSaveInstanceState(save);
        save.putString("Flag_changes", "true");
        save.putString("currentPhotoUri", imageUtils.getCurrentPhotoUri().toString());
    }


    /**
     * onBackPressed method
     */
    @Override
    public void onBackPressed() {

        AlertDialog.Builder exitRequest = new AlertDialog.Builder(EditProfileActivity.this); //give a context to Dialog
        exitRequest.setTitle(R.string.exit_request_title);
        exitRequest.setMessage(R.string.exit_rationale);
        exitRequest.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    writeProfile_copy.clear().commit();
                    finish();

                }
        ).setNegativeButton(android.R.string.cancel,
                (dialog, which) -> dialog.dismiss()
        );

        exitRequest.show();
    }


    /**
     * getViewsAndSetTypography method
     */
    private void getViewsAndSetTypography() {

        //get views
        tv_userFullNameHeading_edit = findViewById(R.id.tv_userFullNameHeading_edit);
        tv_userNameHeading_edit = findViewById(R.id.tv_userNameHeading_edit);
        tv_userCityHeading_edit = findViewById(R.id.tv_userCityHeading_edit);
        tv_userBioHeading_edit = findViewById(R.id.tv_userBioHeading_edit);
        tv_userEmailHeading_edit = findViewById(R.id.tv_userEmailHeading_edit);

        et_userFullName = findViewById(R.id.et_fullNameContent_edit);
        et_userNickName = findViewById(R.id.et_userNameContent_edit);
        et_userCity = findViewById(R.id.et_userCityContent_edit);
        et_userBio = findViewById(R.id.et_userBioContent_edit);
        et_userEmail = findViewById(R.id.et_emailContent_edit);

        // Retrieve fonts
        Typeface robotoBold = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");
        Typeface robotoLight = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

        /*
         * set views font and view text
         */

        //headings
        tv_userFullNameHeading_edit.setTypeface(robotoBold);
        tv_userNameHeading_edit.setTypeface(robotoBold);
        tv_userCityHeading_edit.setTypeface(robotoBold);
        tv_userBioHeading_edit.setTypeface(robotoBold);
        tv_userEmailHeading_edit.setTypeface(robotoBold);

        //edit texts
        et_userFullName.setTypeface(robotoLight);
        et_userNickName.setTypeface(robotoLight);
        et_userCity.setTypeface(robotoLight);
        et_userBio.setTypeface(robotoLight);
        et_userEmail.setTypeface(robotoLight);
    }


    /**
     * This method uploads a File on firebase
     */
    private void uploadFile(Uri file) {

        if (file != null) {
            // Displaying a progress dialog while upload is going on
            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle(getString(R.string.loading_dialog));
            progressDialog.show();

            StorageReference imageReference = storageReference.child("images/" + user.getUserID() + ".jpg");
            imageReference.putFile(file)
                    .addOnSuccessListener(taskSnapshot -> { //if the upload is successfull

                        progressDialog.dismiss(); //hiding the progress dialog

                        writeProfile_copy.clear().commit();

                        Toast.makeText(getApplicationContext(), getString(R.string.profile_saved), Toast.LENGTH_LONG).show();


                        user.setPicture_timestamp(String.valueOf(taskSnapshot.getMetadata().getCreationTimeMillis()));
                        //user.setPicture_uri(taskSnapshot.getDownloadUrl()); //save the download URL

                        Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                        i.putExtra(getString(R.string.user_profile_data_key), user);
                        if (getCallingActivity() != null) {  //if it was a StartActivityForResult then -> null
                            setResult(RESULT_OK, i);
                        } else {
                            startActivity(i);
                        }
                        finish();

                    })
                    .addOnFailureListener(exception -> { //if the upload is not successfull
                        //hiding the progress dialog
                        progressDialog.dismiss();

                        writeProfile_copy.clear().commit();

                        //and displaying error message
                        Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                    })
                    .addOnProgressListener(taskSnapshot -> {
                        //calculating progress percentage
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                        //displaying percentage in progress dialog
                        progressDialog.setMessage(getString(R.string.upload_progress) + ((int) progress) + "%...");
                    });
        }
        //if there is not any file
        else {
            writeProfile_copy.clear().commit();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}