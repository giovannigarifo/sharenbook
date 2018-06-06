package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.onesignal.OneSignal;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import it.polito.mad.sharenbook.adapters.MultipleCheckableCheckboxAdapter;
import it.polito.mad.sharenbook.model.UserProfile;
import it.polito.mad.sharenbook.utils.ImageUtils;
import it.polito.mad.sharenbook.utils.InputValidator;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.PermissionsHandler;
import it.polito.mad.sharenbook.utils.UpdatableFragmentDialog;
import it.polito.mad.sharenbook.utils.UserInterface;
import it.polito.mad.sharenbook.views.ExpandableHeightGridView;


public class EditProfileActivity extends AppCompatActivity {

    //views
    private EditText et_userFullName, et_userNickName, et_userCity, et_userBio, et_userEmail;
    private TextView tv_userNickName, tv_categories;
    private FloatingActionButton save_button;
    private FloatingActionButton fab_editPhoto;

    /* user photo views */
    private com.mikhaellopez.circularimageview.CircularImageView userPicture;

    //Shared Preferences
    private SharedPreferences editedProfile_copy, usernamePref;
    private SharedPreferences.Editor writeProfile_copy, writeUsernamePref;
    private SharedPreferences userProfileData;
    private SharedPreferences.Editor write_userProfileData;

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

    //Regex for input validation
    private Pattern fullname_regex = Pattern.compile("([A-Z]([a-z])*[\\s][A-Za-z]([a-z])*[\\s]?)([A-Za-z]([a-z])*[\\s]?)*");
    private Pattern city_regex = Pattern.compile("([A-Z][a-z]*)[\\s]?[,]?[\\s]?([A-Z][a-z]*)?[\\s]?");

    //User profile data
    private UserProfile user;

    //Firebase References
    private DatabaseReference usersReference;
    private DatabaseReference usernamesReference;
    private StorageReference storageReference;

    private ExpandableHeightGridView fragment_sf_ehgv_categories;
    private MultipleCheckableCheckboxAdapter categoryAdapter;

    // ImageUtils for image handling
    private ImageUtils imageUtils;

    boolean isValid;
    boolean resultFlag = false;
    private boolean creatingProfile = false;

    /**
     * onCreate callback
     *
     * @param savedInstanceState :
     */
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        //modify default typography
        getViews();

        if (getCallingActivity() == null && savedInstanceState == null) {   //Profile creation
            AlertDialog.Builder completeProfileAlert = new AlertDialog.Builder(EditProfileActivity.this); //give a context to Dialog
            completeProfileAlert.setTitle(R.string.complete_profile_title);
            completeProfileAlert.setMessage(R.string.complete_profile_rational);
            completeProfileAlert.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        dialog.dismiss();
                    }
            );

            creatingProfile = true;
            completeProfileAlert.show();

        } else {    //User already created the profile
            et_userNickName.setVisibility(View.GONE);
            tv_userNickName.setVisibility(View.GONE);
        }


        //retrieve the default values if the profile is not yet edited
        default_city = getString(R.string.default_city);
        default_bio = getString(R.string.default_bio);
        default_email = getString(R.string.default_email);
        default_fullname = getString(R.string.default_fullname);
        default_username = getString(R.string.default_username);
        default_picture_path = getString(R.string.default_picture_path);

        // Create a storage reference from our app
        FirebaseStorage storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        /* Get user Data from the Intent Extras*/
        Bundle data = getIntent().getExtras();
        user = data.getParcelable(getString(R.string.user_profile_data_key));

        // Get the database reference to this User Data
        usersReference = FirebaseDatabase.getInstance().getReference(getString(R.string.users_key)).child(user.getUserID());
        usernamesReference = FirebaseDatabase.getInstance().getReference(getString(R.string.usernames_key));

        // Initialize image class
        imageUtils = new ImageUtils(this);

        // Book categories expandable height grid view
        String[] book_categories = getResources().getStringArray(R.array.book_categories);
        categoryAdapter = new MultipleCheckableCheckboxAdapter(EditProfileActivity.this, R.layout.item_checkbox, book_categories);
        fragment_sf_ehgv_categories.setAdapter(categoryAdapter);
        fragment_sf_ehgv_categories.setNumColumns(2);
        fragment_sf_ehgv_categories.setExpanded(true);

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
        editedProfile_copy = getSharedPreferences(getString(R.string.profile_preferences_copy), Context.MODE_PRIVATE);
        writeProfile_copy = editedProfile_copy.edit();

        usernamePref = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        writeUsernamePref = usernamePref.edit();

        userProfileData = getSharedPreferences(getString(R.string.userData_preferences), Context.MODE_PRIVATE);
        write_userProfileData = userProfileData.edit();

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


        String[] bookCategories = getResources().getStringArray(R.array.book_categories);
        if(user.getCategories() != null && user.getCategories().size() != 0) {

            for(Integer catNumber : user.getCategories()){
                categoryAdapter.setCheckboxCheck(Arrays.asList(bookCategories).get(catNumber));
            }

            writeProfile_copy.putString(getString(R.string.categories_copy_key), user.getCategoriesAsString(bookCategories)).commit();
        }

        /* Edit photo section */
        userPicture = findViewById(R.id.userPicture_edit);

        // Load photo from Firebase using Glide cache
        if (!user.getPicture_timestamp().equals(default_picture_path)) {
            UserInterface.showGlideImage(getApplicationContext(), storageReference.child("images/" + user.getUsername() + ".jpg"), userPicture, Long.valueOf(user.getPicture_timestamp()));
        }

        fab_editPhoto = findViewById(R.id.fab_editPhoto);
        fab_editPhoto.setBackgroundDrawable(AppCompatResources.getDrawable(EditProfileActivity.this, R.drawable.ic_check_black_24dp));

        fab_editPhoto.setOnClickListener(v -> {
            PermissionsHandler.check(this, () -> imageUtils.showSelectImageDialog());
        });
    }


    /**
     * onCreateWithBundleNotEmpty method
     */
    private void onCreateWithBundleNotEmpty() {

        //make a copy for rollbacks
        editedProfile_copy = getSharedPreferences(getString(R.string.profile_preferences_copy), Context.MODE_PRIVATE);
        writeProfile_copy = editedProfile_copy.edit();

        usernamePref = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        writeUsernamePref = usernamePref.edit();

        userProfileData = getSharedPreferences(getString(R.string.userData_preferences), Context.MODE_PRIVATE);
        write_userProfileData = userProfileData.edit();

        //get view button
        save_button = findViewById(R.id.fab_save);

        //set hints for views
        et_userFullName.setHint(editedProfile_copy.getString(getString(R.string.fullname_copy_key), default_fullname));
        if(creatingProfile)
            et_userNickName.setHint(editedProfile_copy.getString(getString(R.string.username_copy_key), default_username));

        et_userCity.setHint(editedProfile_copy.getString(getString(R.string.city_copy_key), default_city));
        String actualBio = editedProfile_copy.getString(getString(R.string.bio_copy_key), default_bio);

        if (actualBio.equals(default_bio))
            et_userBio.setHint(actualBio);
        else {
            et_userBio.setText(actualBio);
            et_userBio.setTextColor(Color.GRAY);
        }

        /* Restore categories selection from shared preferences */
        String[] bookCategories = getResources().getStringArray(R.array.book_categories);
        String pref_categories = editedProfile_copy.getString(getString(R.string.categories_copy_key), "void");
        if(!pref_categories.equals("void")) {

            String[] categories = pref_categories.split(", ");
            ArrayList<String> selectedCategories = new ArrayList<String>(Arrays.asList(categories));

            String[] book_categories = getApplicationContext().getResources().getStringArray(R.array.book_categories);

            for (int i = 0; i < selectedCategories.size(); i++)
                categoryAdapter.setCheckboxCheck(Arrays.asList(bookCategories).get(Arrays.asList(book_categories).indexOf(selectedCategories.get(i))));

        }

        et_userEmail.setHint(editedProfile_copy.getString(getString(R.string.email_copy_key), default_email));

        /* Edit photo section */
        userPicture = findViewById(R.id.userPicture_edit);

        // Load photo from local if modified or from Firebase using Glide cache
        String chosenPicture = editedProfile_copy.getString(getString(R.string.userPicture_copy_key), default_picture_path);
        if (editedProfile_copy.getBoolean(getString(R.string.changed_photo_flag_key), false)) {
            userPicture.setImageURI(Uri.parse(chosenPicture));
        } else if (!user.getPicture_timestamp().equals(default_picture_path)) {
            UserInterface.showGlideImage(getApplicationContext(), storageReference.child("images/" + user.getUsername() + ".jpg"), userPicture, Long.valueOf(user.getPicture_timestamp()));
        }

        fab_editPhoto = findViewById(R.id.fab_editPhoto);
        fab_editPhoto.setOnClickListener(v -> {
            PermissionsHandler.check(this, () -> imageUtils.showSelectImageDialog());
        });
    }


    /**
     * onActivityResult method
     *
     * @param requestCode :
     * @param resultCode  :
     * @param data        :
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == ImageUtils.REQUEST_CAMERA) {

                imageUtils.dispatchCropCurrentPhotoIntent(ImageUtils.ASPECT_RATIO_CIRCLE);
                imageUtils.revokeCurrentPhotoUriPermission();

            } else if (requestCode == ImageUtils.REQUEST_GALLERY) {

                imageUtils.dispatchCropPhotoIntent(data.getData(), ImageUtils.ASPECT_RATIO_CIRCLE);

            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

                Uri resultUri = CropImage.getActivityResult(data).getUri();

                try {
                    Uri resizedPhotoUri = ImageUtils.resizeJpegPhoto(this, ImageUtils.EXTERNAL_CACHE, resultUri, 600);

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

            if(creatingProfile){

            /*check username uniqueness*/
                ValueEventListener eventListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if(dataSnapshot.hasChild(et_userNickName.getText().toString())){
                            et_userNickName.setError(getString(R.string.username_already_exists));
                        } else {
                            firebaseSaveProfile();
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                };
                usernamesReference.addListenerForSingleValueEvent(eventListener);

            } else {
                firebaseSaveProfile();
            }

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

        /* get preferred categories */
        ArrayList<String> selectedCategories = categoryAdapter.getSelectedStrings();
        ArrayList<Integer> selectedCategoriesAsInt = new ArrayList<>();

        String[] bookCategories = getResources().getStringArray(R.array.book_categories); //retrieve the array of all available categories
        for (int i = 0; i < selectedCategories.size(); i++)
            selectedCategoriesAsInt.add(Arrays.asList(bookCategories).indexOf(selectedCategories.get(i))); //retrieve index of the categories

        userData.put(getString(R.string.categories_key), selectedCategoriesAsInt);
        user.setCategories(selectedCategoriesAsInt);

        if(creatingProfile) {

            HashMap<String, Object> unameEntry = new HashMap<>();
            unameEntry.put("messages", "placeholder");

            usernamesReference.child(user.getUsername()).updateChildren(unameEntry, ((databaseError, databaseReference) -> {

                if (databaseError == null) {

                    usersReference.child(getString(R.string.profile_key)).updateChildren(userData, (databaseError2, databaseReference2) -> {

                        if (databaseError2 == null) {
                            checkProfileImageUpdate();
                        }

                    });

                }

            }));

        } else {
            usersReference.child(getString(R.string.profile_key)).updateChildren(userData, (databaseError, databaseReference2) -> {

                if (databaseError == null) {
                    checkProfileImageUpdate();
                }

            });
        }

    }


    /**
     * validateForm method
     *
     * @return :
     */
    private boolean validateForm() {
        isValid = true;

        if (!(et_userFullName.getText().toString().equals(fullname)) && !(et_userFullName.getText().toString().isEmpty())) {
            if ((et_userFullName.getText().toString().length()) < 5) {
                et_userFullName.setError(getString(R.string.name_length_format_rationale));
                isValid = false;

            } else if (!fullname_regex.matcher(et_userFullName.getText().toString()).matches()) {
                et_userFullName.setError(getString(R.string.name_bad_format_rationale));
                isValid = false;
            } else {
                et_userFullName.setError(null);
            }

        } else if (et_userFullName.getText().toString().isEmpty()) {
            et_userFullName.setError(getString(R.string.required_field));
            isValid = false;
        }

        if (!(et_userEmail.getText().toString().equals(email)) && !(et_userEmail.getText().toString().isEmpty())) { //the mail has been changed
            Log.d("Email:", et_userEmail.getText().toString());

            if (InputValidator.isWrongEmailAddress(et_userEmail)) {
                et_userEmail.setError(getString(R.string.bad_email));
                isValid = false;

            } else {
                et_userEmail.setError(null);
            }
        } else if (TextUtils.isEmpty(et_userEmail.getText().toString())) {
            et_userEmail.setError(getString(R.string.required_field));
            isValid = false;
        }

        if (!(et_userCity.getText().toString().equals(city)) && !(et_userCity.getText().toString().isEmpty())) {

            if ((TextUtils.getTrimmedLength(et_userCity.getText().toString())) < 2) {
                et_userCity.setError(getString(R.string.city_bad_lenght_rationale));
                isValid = false;
            } else if (!(city_regex.matcher(et_userCity.getText().toString()).matches())) {
                et_userCity.setError(getString(R.string.city_bad_format_rationale));
                isValid = false;
            } else {

                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());

                List<Address> place = new ArrayList<>();
                String location = et_userCity.getText().toString();
                Boolean location_error = false;

                try {

                    place.clear();
                    place.addAll(geocoder.getFromLocationName(location, 1));
                    if (place.size() == 0)
                        location_error = true;

                } catch (IOException e) { //if it was not possible to recognize location
                    location_error = true;
                }

                if (location_error) {
                    isValid = false;
                    et_userCity.setError(getString(R.string.unknown_place));
                } else {
                    et_userCity.setError(null);
                }
            }


        } else if (TextUtils.isEmpty(et_userCity.getText().toString())) {
            et_userCity.setError(getString(R.string.required_field));
            isValid = false;
        }

        if (TextUtils.isEmpty(et_userBio.getText().toString())) {
            et_userBio.setError(getString(R.string.required_field));
            isValid = false;
        }

        if (creatingProfile && !(et_userNickName.getText().toString().equals(username)) && !(et_userNickName.getText().toString().isEmpty())) {

            if ((TextUtils.getTrimmedLength(et_userNickName.getText().toString())) < 3 || (TextUtils.getTrimmedLength(et_userNickName.getText().toString())) > 12) {
                et_userNickName.setError(getString(R.string.username_bad_lenght_rationale));
                isValid = false;
            }

        } else if (creatingProfile && et_userNickName.getText().toString().isEmpty()) {
            et_userNickName.setError(getString(R.string.required_field));
            isValid = false;
        } else {
            et_userNickName.setError(null);
        }

        /* Validate categories */
        Log.d("num cat", "here: " + categoryAdapter.getSelectedStrings().size());
        if (categoryAdapter.getSelectedStrings().size() == 0 || categoryAdapter.getSelectedStrings().size()>5) {
            tv_categories.requestFocus();
            if(categoryAdapter.getSelectedStrings().size() == 0)
                tv_categories.setError(getString(R.string.select_one_category));
            else
                tv_categories.setError(getString(R.string.select_max_category));
            isValid = false;
        } else {
            tv_categories.setError(null);
        }


        return isValid;
    }


    private void checkProfileImageUpdate(){
        /*
         * Check if profile picture has been changed or not
         */
        writeUsernamePref.putString(getString(R.string.username_copy_key), user.getUsername()).commit();
        OneSignal.sendTag("User_ID", user.getUsername());

        if (editedProfile_copy.getBoolean(getString(R.string.changed_photo_flag_key), false)) {

            Uri picturePath = Uri.parse(editedProfile_copy.getString(getString(R.string.userPicture_copy_key), default_picture_path));
            uploadFile(picturePath);

        } else {
        /*
         * The user has not changed the profile picture
         */

            Toast.makeText(getApplicationContext(), getString(R.string.profile_saved), Toast.LENGTH_LONG).show();

            Intent i = new Intent(getApplicationContext(), TabbedShowProfileActivity.class);
            i.putExtra(getString(R.string.user_profile_data_key), user);

            /* Update local data */
            write_userProfileData.putString(getString(R.string.username_pref), user.getUsername()).commit();
            write_userProfileData.putString(getString(R.string.uid_pref), user.getUserID()).commit();
            write_userProfileData.putString(getString(R.string.bio_pref), user.getBio()).commit();
            write_userProfileData.putString(getString(R.string.city_pref), user.getCity()).commit();
            write_userProfileData.putString(getString(R.string.email_pref), user.getEmail()).commit();
            write_userProfileData.putString(getString(R.string.fullname_pref), user.getFullname()).commit();
            write_userProfileData.putString(getString(R.string.picture_pref), user.getPicture_timestamp()).commit();
            String[] bookCategories = getResources().getStringArray(R.array.book_categories);
            write_userProfileData.putString(getString(R.string.categories_pref), user.getCategoriesAsString(bookCategories)).commit();

            if (getCallingActivity() != null) {  //if it was a StartActivityForResult then -> null
                setResult(RESULT_OK, i);
            } else {
                startActivity(i);
            }
            writeProfile_copy.clear().commit();

            /* save the new Data for NavigationDrawerProfile */
            NavigationDrawerManager.setNavigationDrawerProfileByUser(user);

            finish();
        }

    }


    /**
     * setEditTextListeners method
     *
     * @param et      :
     * @param key     :
     * @param keycopy :
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
     * getViews method
     */
    private void getViews() {

        //get views
        et_userFullName = findViewById(R.id.et_fullNameContent_edit);
        et_userNickName = findViewById(R.id.et_userNameContent_edit);
        et_userCity = findViewById(R.id.et_userCityContent_edit);
        et_userBio = findViewById(R.id.et_userBioContent_edit);
        et_userEmail = findViewById(R.id.et_emailContent_edit);
        tv_categories = findViewById(R.id.tv_categories);
        tv_userNickName = findViewById(R.id.tv_userNameHeading_edit);
        fragment_sf_ehgv_categories = findViewById(R.id.ehgv_categories);

    }


    /**
     * This method uploads a File on firebase
     */
    private void uploadFile(Uri file) {

        if (file != null) {
            // Displaying a progress dialog while upload is going on
            UpdatableFragmentDialog.show(this, getString(R.string.loading_dialog), null);

            StorageReference imageReference = storageReference.child("images/" + user.getUsername() + ".jpg");
            DatabaseReference usernameRef = FirebaseDatabase.getInstance().getReference("usernames").child(user.getUsername());
            imageReference.putFile(file)
                    .addOnSuccessListener(taskSnapshot -> { //if the upload is successfull

                        UpdatableFragmentDialog.dismiss(); //hiding the progress dialog

                        writeProfile_copy.clear().commit();

                        Toast.makeText(getApplicationContext(), getString(R.string.profile_saved), Toast.LENGTH_LONG).show();


                        long picSignature = taskSnapshot.getMetadata().getCreationTimeMillis();

                        //Add profile pic signature on DB
                        Map<String, Object> signature = new HashMap<>();
                        signature.put("picSignature", picSignature);
                        usernameRef.updateChildren(signature);

                        user.setPicture_timestamp(String.valueOf(picSignature));

                        /* Update local data */
                        write_userProfileData.putString(getString(R.string.username_pref), user.getUsername()).commit();
                        write_userProfileData.putString(getString(R.string.uid_pref), user.getUserID()).commit();
                        write_userProfileData.putString(getString(R.string.bio_pref), user.getBio()).commit();
                        write_userProfileData.putString(getString(R.string.city_pref), user.getCity()).commit();
                        write_userProfileData.putString(getString(R.string.email_pref), user.getEmail()).commit();
                        write_userProfileData.putString(getString(R.string.fullname_pref), user.getFullname()).commit();
                        write_userProfileData.putString(getString(R.string.picture_pref), user.getPicture_timestamp()).commit();
                        String[] bookCategories = getResources().getStringArray(R.array.book_categories);
                        write_userProfileData.putString(getString(R.string.categories_pref), user.getCategoriesAsString(bookCategories)).commit();

                        Intent i = new Intent(getApplicationContext(), TabbedShowProfileActivity.class);
                        i.putExtra(getString(R.string.user_profile_data_key), user);



                        if (getCallingActivity() != null) {  //if it was a StartActivityForResult then -> null
                            setResult(RESULT_OK, i);
                        } else {
                            startActivity(i);
                        }

                        /* save the new Data for NavigationDrawerProfile */

                        NavigationDrawerManager.setNavigationDrawerProfileByUser(user);


                        finish();

                    })
                    .addOnFailureListener(exception -> { //if the upload is not successfull
                        //hiding the progress dialog
                        UpdatableFragmentDialog.dismiss();

                        writeProfile_copy.clear().commit();

                        //and displaying error message
                        Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                    })
                    .addOnProgressListener(taskSnapshot -> {
                        //calculating progress percentage
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();

                        //displaying percentage in progress dialog
                        UpdatableFragmentDialog.updateMessage(getString(R.string.upload_progress) + ((int) progress) + "%...");
                    });
        }
        //if there is not any file
        else {
            writeProfile_copy.clear().commit();
        }
    }
}