package it.polito.mad.sharenbook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.algolia.search.saas.Client;
import com.algolia.search.saas.Index;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.core.GeoHash;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import it.polito.mad.sharenbook.adapters.MultipleCheckableCheckboxAdapter;
import it.polito.mad.sharenbook.adapters.SingleCheckableCheckboxAdapter;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.ImageUtils;
import it.polito.mad.sharenbook.utils.InputValidator;
import it.polito.mad.sharenbook.utils.PermissionsHandler;
import it.polito.mad.sharenbook.utils.UpdatableFragmentDialog;
import it.polito.mad.sharenbook.utils.UserInterface;
import it.polito.mad.sharenbook.utils.Utils;
import it.polito.mad.sharenbook.views.ExpandableHeightGridView;

public class EditBookActivity extends AppCompatActivity {

    final static int MAX_ALLOWED_BOOK_PHOTO = 5;
    final static int MIN_REQUIRED_BOOK_PHOTO = 1;

    // context of the activity
    private Context context;

    // views
    private EditText editbook_et_isbn, editbook_et_title, editbook_et_subtitle, editbook_et_authors,
            editbook_et_publisher, editbook_et_publishedDate, editbook_et_description, editbook_et_pageCount,
            editbook_et_language, editbook_et_location, editbook_et_tags;

    private ExpandableHeightGridView editbook_ehgv_conditions, editbook_ehgv_categories;

    private ScrollView editbook_scrollview;

    private FloatingActionButton editbook_fab_save;

    private ImageButton editbook_ib_addBookPhoto;

    private Button editbook_btn_addBookPhoto;

    private BottomNavigationView navBar;

    private Toast toast;

    private ImageUtils imageUtils;

    //Recycler View
    private RecyclerView editbook_rv_bookPhotos;
    private BookPhotoAdapter rvAdapter;
    private RecyclerView.LayoutManager rvLayoutManager;
    private SingleCheckableCheckboxAdapter conditionAdapter;
    private MultipleCheckableCheckboxAdapter categoryAdapter;

    //information of the book to be shared
    private Book book;
    private String bookKey;
    private ArrayList<String> removedPhotos;

    // FireBase objects
    private FirebaseUser firebaseUser;
    private StorageReference bookImagesStorage;

    // ProgressDialog
    private int photoLoaded;

    // Boolean value to check if a new book should be added
    private boolean isNewBook;

    private String username;

    /**
     * onCreate callback
     *
     * @param savedInstanceState : bundle that contains activity state information (null or the book)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_book);
        context = this.getApplicationContext();
        getViews(); //retrieve references to views objects and change default fonts

        SharedPreferences userData = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userData.getString(getString(R.string.username_copy_key), "");

        // Setup FireBase
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        bookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));

        // Setup image utils class
        imageUtils = new ImageUtils(this);

        //retrieve book info
        if (savedInstanceState == null) {

            removedPhotos = new ArrayList<>();
            Bundle bundle = getIntent().getExtras();

            if (bundle == null) {

                Log.d("debug", "[EditBookActivity] no book received from ShareBookActivity");

            } else {

                book = bundle.getParcelable("book"); //retrieve book from intent
            }

        } else {
            // retrieve book info from saveInstanceState
            book = savedInstanceState.getParcelable("book");

            // retrieve currentPhotoUri for imageUtils class
            Uri currentPhotoUri = Uri.parse(savedInstanceState.getString("currentPhotoUri"));
            imageUtils.setCurrentPhotoUri(currentPhotoUri);

            // retirve removedPhotos list
            removedPhotos = savedInstanceState.getStringArrayList("removedPhotos");
        }

        /*
         * Recycle View
         */
        editbook_rv_bookPhotos.setHasFixedSize(true); //improves performance

        // attach a Layout Manager to the RecyclerView, it's in charge of injecting views into the Recycler
        rvLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        editbook_rv_bookPhotos.setLayoutManager(rvLayoutManager);

        // set an adapter for the RecyclerView, it's in charge of managing the ViewHolder objects
        rvAdapter = new BookPhotoAdapter(book, this, removedPhotos);
        editbook_rv_bookPhotos.setAdapter(rvAdapter);

        // Book Condition expandable height grid view
        String[] book_conditions = getResources().getStringArray(R.array.book_conditions);
        conditionAdapter = new SingleCheckableCheckboxAdapter(EditBookActivity.this, R.layout.item_checkbox, book_conditions);
        editbook_ehgv_conditions.setAdapter(conditionAdapter);
        editbook_ehgv_conditions.setNumColumns(2);
        editbook_ehgv_conditions.setExpanded(true);
        editbook_ehgv_conditions.setVerticalScrollBarEnabled(false);

        // Book categories expandable height grid view
        String[] book_categories = getResources().getStringArray(R.array.book_categories);
        categoryAdapter = new MultipleCheckableCheckboxAdapter(EditBookActivity.this, R.layout.item_checkbox, book_categories);
        editbook_ehgv_categories.setAdapter(categoryAdapter);
        editbook_ehgv_categories.setNumColumns(2);
        editbook_ehgv_categories.setExpanded(true);

        //Populate the view with all the information retrieved from Google Books API
        loadViewWithBookData();

        /*
         * register callbacks for buttons
         */

        editbook_fab_save.setOnClickListener((v) -> {
            Log.d("debug", "editbook_fab_save onClickListener fired");
            if (validateInputFields()) {

                updateBookWithUserInfo();
                firebaseSaveBook();
                algoliaIndexBook();
            }
        });

        editbook_ib_addBookPhoto.setOnClickListener(v -> addBookPhoto());
        editbook_btn_addBookPhoto.setOnClickListener(v -> addBookPhoto());

        // Setup bottom navbar
        UserInterface.setupNavigationBar(this, R.id.navigation_myBook);
    }


    /*
     * Update the Book object with the final information inserted by the user in the edittexts
     */
    private void updateBookWithUserInfo() {

        book.setIsbn(editbook_et_isbn.getText().toString());
        book.setTitle(editbook_et_title.getText().toString());
        book.setSubtitle(editbook_et_subtitle.getText().toString());
        book.setAuthors(Utils.commaStringToList(editbook_et_authors.getText().toString()));
        book.setPublisher(editbook_et_publisher.getText().toString());
        book.setPublishedDate(editbook_et_publishedDate.getText().toString());
        book.setDescription(editbook_et_description.getText().toString());

        String pageCount = editbook_et_pageCount.getText().toString();

        if (pageCount.equals(""))
            book.setPageCount(0);
        else
            book.setPageCount(Integer.parseInt(pageCount));

        //retrieve categories
        ArrayList<String> selectedCategories = categoryAdapter.getSelectedStrings();
        ArrayList<Integer> selectedCategoriesAsInt = new ArrayList<>();

        if (selectedCategories.size() > 0) {
            String[] bookCategories = getResources().getStringArray(R.array.book_categories); //retrieve the array of all available conditions
            for (int i = 0; i < selectedCategories.size(); i++)
                selectedCategoriesAsInt.add(Arrays.asList(bookCategories).indexOf(selectedCategories.get(i))); //retrieve index of the condition
        }
        book.setCategories(selectedCategoriesAsInt);

        //retrieve conditions
        book.setBookConditions(conditionAdapter.getSelectedPosition());//get the condition selected by the user

        book.setLanguage(editbook_et_language.getText().toString());
        book.setTags(Utils.commaStringToList(editbook_et_tags.getText().toString()));
    }


    /*
     * Add Book photo button listener
     */
    private void addBookPhoto() {

        if (book.getPhotosName().size() + book.getBookPhotosUri().size() >= MAX_ALLOWED_BOOK_PHOTO) {

            showToast(getString(R.string.max_allowed_book_photo));

        } else {
            // Check if permissions are granted and if yes open selectImageDialog
            PermissionsHandler.check(this, () -> imageUtils.showSelectImageDialog());
        }
    }



    /**
     * Saves the state of the activity when dealing with system wide events (e.g. rotation)
     *
     * @param outState : the bundle object that contains all the serialized information to be saved
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("book", book);
        outState.putString("currentPhotoUri", imageUtils.getCurrentPhotoUri().toString());
        outState.putStringArrayList("removedPhotos", removedPhotos);
    }


    /**
     * Loads all the info obtained from Google Books API into the view
     */
    @SuppressLint("SetTextI18n")
    private void loadViewWithBookData() {

        isNewBook = book.getBookId().equals("");

        editbook_et_isbn.setText(book.getIsbn());
        editbook_et_title.setText(book.getTitle());
        editbook_et_subtitle.setText(book.getSubtitle());
        editbook_et_publisher.setText(book.getPublisher());
        editbook_et_publishedDate.setText(book.getPublishedDate());
        editbook_et_description.setText(book.getDescription());
        editbook_et_language.setText(book.getLanguage());

        if (!isNewBook) {
            List<Address> places = new ArrayList<>();

            try {
                Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                places.addAll(geocoder.getFromLocation(book.getLocation_lat(), book.getLocation_long(), 1));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (places.isEmpty()) {
                editbook_et_location.setText(R.string.unknown_place);
            } else {
                String bookLocation = places.get(0).getLocality() + ", " + places.get(0).getCountryName();
                editbook_et_location.setText(bookLocation);
            }

            //set previously selected book condition
            conditionAdapter.setSelectedPosition(book.getBookConditions());
            conditionAdapter.notifyDataSetChanged();

            //set previously selected book categories
            categoryAdapter.setAlreadyCheckedCheckboxes(book.getCategories());
            categoryAdapter.notifyDataSetChanged();

        }

        if (book.getPageCount() != -1)
            editbook_et_pageCount.setText(Integer.valueOf(book.getPageCount()).toString());

        //authors to comma separated string
        String authors = Utils.listToCommaString(book.getAuthors());
        editbook_et_authors.setText(authors);

        //tags to comma separated string
        String tags = Utils.listToCommaString(book.getTags());
        editbook_et_tags.setText(tags);



    }


    /*
     * Toast method
     */
    private void showToast(String message) {

        if (toast == null || toast.getView().getWindowVisibility() != View.VISIBLE) {

            runOnUiThread(() -> {
                toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG);
                toast.show();
            });
        }
    }


    /**
     * onActivityResult method, callback fired each time an intent returns his isValid
     *
     * @param requestCode : the kind of intent requested
     * @param resultCode  : isValid code of the intet
     * @param data        : data returned by the intnet
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == ImageUtils.REQUEST_CAMERA) {

                imageUtils.dispatchCropCurrentPhotoIntent(ImageUtils.ASPECT_RATIO_PHOTO_PORT);
                imageUtils.revokeCurrentPhotoUriPermission();

            } else if (requestCode == ImageUtils.REQUEST_GALLERY) {

                imageUtils.dispatchCropPhotoIntent(data.getData(), ImageUtils.ASPECT_RATIO_PHOTO_PORT);

            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

                Uri resultUri = CropImage.getActivityResult(data).getUri();

                try {
                    Uri resizedPhotoUri = ImageUtils.resizeJpegPhoto(this, ImageUtils.EXTERNAL_CACHE, resultUri, 540, true);
                    book.getBookPhotosUri().add(resizedPhotoUri);

                    //notify update of the collection to Recycle View adapter
                    int lastPosition = book.getPhotosName().size() + book.getBookPhotosUri().size() - 1;
                    rvAdapter.notifyItemInserted(lastPosition);
                    rvLayoutManager.scrollToPosition(lastPosition);

                } catch (IOException e) {
                    Log.d("error", "IOException when retrieving resized image Uri");
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * onBackPressed method
     */
    @Override
    public void onBackPressed() {

        showExitRequestDialog();
    }

    /**
     * Show an alert to the user, asking if he really want to exit. on affermative response, finish the activity.
     */
    public void showExitRequestDialog(){

        AlertDialog.Builder exitRequest = new AlertDialog.Builder(EditBookActivity.this); //give a context to Dialog
        exitRequest.setTitle(R.string.exit_request_title);
        exitRequest.setMessage(R.string.exit_rationale);
        exitRequest.setPositiveButton(android.R.string.ok, (dialog, which) -> finish()
        ).setNegativeButton(android.R.string.cancel,
                (dialog, which) -> dialog.dismiss()
        );

        exitRequest.show();
    }

    /**
     * Save all data on FireBase
     */
    private void firebaseSaveBook() {

        // Show ProgressDialog
        UpdatableFragmentDialog.show(this, null, getString(R.string.default_saving_on_firebase));

        // Get Firebase root node reference
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        // Get book key
        bookKey = isNewBook ? rootRef.child(getString(R.string.books_key)).push().getKey() : book.getBookId();

        // Get Map containing book data
        Map<String, Object> bookData = createHashMapFromBook();

        // Create Map containing book location data
        GeoLocation location = new GeoLocation(book.getLocation_lat(), book.getLocation_long());
        GeoHash geoHash = new GeoHash(location);
        Map<String, Object> bookLocation = new HashMap<>();
        bookLocation.put("g", geoHash.getGeoHashString());
        bookLocation.put("l", Arrays.asList(location.latitude, location.longitude));

        // Create transaction Map
        Map<String, Object> transaction = new HashMap<>();
        transaction.put(getString(R.string.books_key) + "/" + bookKey, bookData);
        transaction.put(getString(R.string.books_locations) + "/" + bookKey, bookLocation);
        if (isNewBook)
            transaction.put(getString(R.string.users_key) + "/" + firebaseUser.getUid() + "/" + getString(R.string.user_books_key) + "/" + bookKey, bookKey);

        // Push newBook or edited book on firebase
        rootRef.updateChildren(transaction, (databaseError, databaseReference) -> {
            if (databaseError == null) {

                // Load new photos on firebase
                firebaseSavePhotos(bookKey);
                // Delete removed photos from firebase
                firebaseDeletePhotos(bookKey);

            } else {
                Toast.makeText(getApplicationContext(), "An error occurred, try later.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Create firebase HashMap containing data to save
     */
    private HashMap<String, Object> createHashMapFromBook() {

        // Retrieve all book data
        HashMap<String, Object> bookData = new HashMap<>();
        bookData.put("owner_uid", firebaseUser.getUid());
        bookData.put("owner_username", username);
        bookData.put("isbn", book.getIsbn());
        bookData.put("title", book.getTitle());
        bookData.put("subtitle", book.getSubtitle());
        bookData.put("authors", book.getAuthors());
        bookData.put("publisher", book.getPublisher());
        bookData.put("publishedDate", book.getPublishedDate());
        bookData.put("description", book.getDescription());
        bookData.put("pageCount", book.getPageCount());
        bookData.put("categories", book.getCategories());
        bookData.put("language", book.getLanguage());
        bookData.put("bookConditions", book.getBookConditions());
        bookData.put("tags", book.getTags());
        bookData.put("numPhotos", book.getBookPhotosUri().size());
        bookData.put("shared", book.isShared());

        if (book.getCreationTime() == 0) {

            bookData.put("creationTime", ServerValue.TIMESTAMP); //get timestamp from firebase
            book.setCreationTime(System.currentTimeMillis()); //get timestamp from local device

        } else
            bookData.put("creationTime", book.getCreationTime());

        bookData.put("location_lat", book.getLocation_lat());
        bookData.put("location_long", book.getLocation_long());

        // Get photo names
        for (Uri uri : book.getBookPhotosUri()) {
            String fileName = uri.getLastPathSegment();
            book.getPhotosName().add(fileName);
        }
        bookData.put("photosName", book.getPhotosName());

        // Set thumbnail value (only if still present)
        if (isNewBook && book.getBookPhotosUri().get(0).toString().equals(book.getThumbnail())) {
            book.setThumbnail(book.getPhotosName().get(0));
        }
        bookData.put("thumbnail", book.getThumbnail());

        return bookData;
    }


    /**
     * Save all photos on firebase
     */
    private void firebaseSavePhotos(String bookKey) {
        // Get list of photos
        List<Uri> photosUri = book.getBookPhotosUri();
        List<String> photosName = book.getPhotosName();
        int photosNameInitSize = photosName.size() - photosUri.size();

        List<Task<UploadTask.TaskSnapshot>> taskList = new ArrayList<>();
        photoLoaded = 0;
        int numPhotos = photosUri.size();

        // Show message "uploading books"
        updateProgressDialogMessage(++photoLoaded, numPhotos);

        // Launch a task for every photo that should be updated
        for (int i = 0; i < photosUri.size(); i++) {
            final int num = i;
            String fileName = photosName.get(photosNameInitSize + i);

            // Generate new file on firebase and write it
            StorageReference newFile = bookImagesStorage.child(bookKey + "/" + fileName);
            Task<UploadTask.TaskSnapshot> newTask = newFile.putFile(photosUri.get(i))
                    .addOnSuccessListener(taskSnapshot -> {
                        // Handle successful uploads
                        Log.d("Debug", "Photo n. " + num + " uploaded!");
                        updateProgressDialogMessage(++photoLoaded, numPhotos);
                    })
                    .addOnFailureListener(exception -> {
                        // Handle unsuccessful uploads
                        Log.d("Debug", "Error during upload of photo n. " + num);
                    });

            taskList.add(newTask);
        }

        Tasks.whenAllComplete(taskList).addOnCompleteListener(task -> {
            UpdatableFragmentDialog.dismiss();
            Toast.makeText(getApplicationContext(), R.string.default_book_saved, Toast.LENGTH_LONG).show();
            Intent i = new Intent(getApplicationContext(), ShowCaseActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
            finish();
        });
    }

    /**
     * Delete removed photos from firebase
     */
    private void firebaseDeletePhotos(String bookKey) {

        for (String fileName : removedPhotos) {
            StorageReference photoRef = bookImagesStorage.child(bookKey + "/" + fileName);
            photoRef.delete()
                    .addOnSuccessListener(aVoid -> Log.d("DEBUG", "Photo removed from firebase -> " + photoRef.toString()))
                    .addOnFailureListener(e -> Log.d("DEBUG", "Error during photo removal from firebase -> " + photoRef.toString()));
        }
    }


    /**
     * Add the book to the Algolia Index so that it can be searched
     */
    private void algoliaIndexBook() {

        try {

            // Setup Algolia
            // Client algoliaClient = new Client("4DWHVL57AK", "03391b3ea81e4a5c37651a677670bcb8"); <- RELEASE CONFIG
            Client algoliaClient = new Client("K7HV32WVKQ", "80c98eabf83684293f3b8b330ca2486e");
            Index index = algoliaClient.getIndex("books");

            JSONObject bookData = new JSONObject()
                    .put("bookId", bookKey)
                    .put("owner_uid", firebaseUser.getUid())
                    .put("owner_username", username)
                    .put("isbn", book.getIsbn())
                    .put("title", book.getTitle())
                    .put("subtitle", book.getSubtitle())
                    .put("authors", new JSONArray(book.getAuthors()))
                    .put("publisher", book.getPublisher())
                    .put("publishedDate", book.getPublishedDate())
                    .put("description", book.getDescription())
                    .put("pageCount", book.getPageCount())
                    .put("categories", new JSONArray(book.getCategories()))
                    .put("language", book.getLanguage())
                    .put("bookConditions", book.getBookConditions())
                    .put("tags", new JSONArray(book.getTags()))
                    .put("thumbnail", book.getThumbnail())
                    .put("numPhotos", book.getBookPhotosUri().size())
                    .put("creationTime", book.getCreationTime()) //setted in firebaseSaveBook()
                    .put("location_lat", book.getLocation_lat())
                    .put("location_long", book.getLocation_long())
                    .put("photosName", new JSONArray(book.getPhotosName()));

            JSONObject ob = new JSONObject()
                    .put("bookData", bookData)
                    .put("shared", book.isShared())
                    .put("objectID", bookKey);

            if (isNewBook) {
                index.addObjectAsync(ob, (jsonObject, e) -> Log.d("DEBUG", "Algolia ADD request completed."));
            } else {
                index.saveObjectAsync(ob, bookKey, (jsonObject, e) -> Log.d("DEBUG", "Algolia UPDATE request completed."));
            }

        } catch (JSONException e) {
            e.printStackTrace();
            Log.e("error", "Unable to update Algolia index.");
        }
    }

    /**
     * Validate user inputs
     */
    private boolean validateInputFields() {
        boolean isValid = true;
        boolean alreadyFocused = false;

        if (InputValidator.isWrongIsbn(editbook_et_isbn)) {
            editbook_et_isbn.setError(getText(R.string.isbn_bad_format));
            isValid = false;
            editbook_et_isbn.requestFocus();
            alreadyFocused = true;
        }
        if (editbook_et_title.getText().toString().isEmpty()) {
            editbook_et_title.setError(getText(R.string.field_required));
            isValid = false;
            if (!alreadyFocused) {
                editbook_et_title.requestFocus();
                alreadyFocused = true;
            }
        }
        if (editbook_et_authors.getText().toString().isEmpty()) {
            editbook_et_authors.setError(getText(R.string.field_required));
            isValid = false;
            if (!alreadyFocused) {
                editbook_et_authors.requestFocus();
                alreadyFocused = true;
            }
        }

        if (editbook_et_location.getText().toString().isEmpty()) {
            editbook_et_location.setError(getText(R.string.field_required));
            isValid = false;
            if (!alreadyFocused) {
                editbook_et_location.requestFocus();
                alreadyFocused = true;
            }
        } else {
            boolean error = false;
            String location = editbook_et_location.getText().toString();

            Geocoder geocoder = new Geocoder(context, Locale.getDefault());
            List<Address> place = new ArrayList<>();
            try {
                place.addAll(geocoder.getFromLocationName(location, 1));
                if (place.size() == 0) {
                    error = true;
                }
            } catch (IOException e) { //if it was not possible to recognize location
                error = true;
            }

            if (error) {
                editbook_et_location.setError(getText(R.string.unknown_place));
                isValid = false;
                if (!alreadyFocused) {
                    editbook_et_location.requestFocus();
                    alreadyFocused = true;
                }
            } else {
                // Set book's location attributes
                book.setLocation_lat(place.get(0).getLatitude());
                book.setLocation_long(place.get(0).getLongitude());
            }
        }

        //book conditions validations
        if (conditionAdapter.getSelectedPosition() == -1) {
            showToast(getText(R.string.field_required).toString());
            showToast("you must select one condition");
            isValid = false;
        }

        //book category validations
        if (categoryAdapter.getSelectedStrings().size() < 1 || categoryAdapter.getSelectedStrings().size() > 3) {
            showToast("max three categories are allowed");
            isValid = false;
        }

        //there must be at least one photo (excluding the thumbnail)
        int thumbnailIsPresent;

        if (book.getThumbnail().equals(""))
            thumbnailIsPresent = 0;
        else thumbnailIsPresent = 1; //thumbnail is present

        if (book.getBookPhotosUri().size() + book.getPhotosName().size() - thumbnailIsPresent < MIN_REQUIRED_BOOK_PHOTO) {
            isValid = false;
            if (!alreadyFocused) {
                UserInterface.scrollToViewTop(editbook_scrollview, editbook_rv_bookPhotos);
                showToast(getString(R.string.min_photo_required));
            }
        }

        return isValid;
    }

    /**
     * Update progressDialog message with new upload photo state
     */
    private void updateProgressDialogMessage(int num, int den) {
        if (num <= den) {
            String msg = getString(R.string.default_saving_photo) + num + getString(R.string.of_preposition) + den + getString(R.string.default_ongoing);
            UpdatableFragmentDialog.updateMessage(msg);
        }
    }

    /**
     * getViews method
     */
    private void getViews() {

        //get scrollview
        editbook_scrollview = findViewById(R.id.editbook_scrollview);

        //get navbar
        navBar = findViewById(R.id.navigation);

        //get buttons
        editbook_fab_save = findViewById(R.id.editbook_fab_save);
        editbook_ib_addBookPhoto = findViewById(R.id.editbook_ib_addBookPhoto);
        editbook_btn_addBookPhoto = findViewById(R.id.editbook_tv_addBookPhoto);

        //get recycle views
        editbook_rv_bookPhotos = findViewById(R.id.editbook_rv_bookPhotos);

        editbook_ehgv_conditions = findViewById(R.id.editbook_ehgv_conditions);
        editbook_ehgv_categories = findViewById(R.id.editbook_ehgv_categories);

        //get the rest of views
        editbook_et_isbn = findViewById(R.id.editbook_et_isbn);
        editbook_et_title = findViewById(R.id.editbook_et_title);
        editbook_et_subtitle = findViewById(R.id.editbook_et_subtitle);
        editbook_et_authors = findViewById(R.id.editbook_et_authors);
        editbook_et_publisher = findViewById(R.id.editbook_et_publisher);
        editbook_et_publishedDate = findViewById(R.id.editbook_et_publishedDate);
        editbook_et_description = findViewById(R.id.editbook_et_description);
        editbook_et_pageCount = findViewById(R.id.editbook_et_pageCount);
        editbook_et_language = findViewById(R.id.editbook_et_language);
        editbook_et_location = findViewById(R.id.editbook_et_location);
        editbook_et_tags = findViewById(R.id.editbook_et_tags);
    }

    /**
     * BookPhotoAdapter class
     */
    private class BookPhotoAdapter extends RecyclerView.Adapter<BookPhotoAdapter.BookPhotoViewHolder> {

        private Book mBook;
        private List<Uri> mBookPhotosUri;
        private List<String> mPhotosName;
        private Activity mActivity;
        private ContentResolver mContentResolver;
        private List<String> mRemovedPhotos;
        private StorageReference bookImagesStorage;

        //Inner Class that provides a reference to the views for each data item of the collection
        class BookPhotoViewHolder extends RecyclerView.ViewHolder {

            private ImageView iv;
            private ImageButton ib;

            BookPhotoViewHolder(RelativeLayout rl) {
                super(rl);
                this.iv = rl.findViewById(R.id.itembookphoto_iv_bookphoto);
                this.ib = rl.findViewById(R.id.itembookphoto_ib_deletePhoto);
            }
        }

        //constructor
        BookPhotoAdapter(Book book, Activity activity, List<String> removedPhotos) {
            this.mBook = book;
            this.mBookPhotosUri = book.getBookPhotosUri();
            this.mPhotosName = book.getPhotosName();
            this.mActivity = activity;
            this.mContentResolver = activity.getContentResolver();
            this.mRemovedPhotos = removedPhotos;
            this.bookImagesStorage = FirebaseStorage.getInstance().getReference(activity.getString(R.string.book_images_key));
        }

        /**
         * Create new ViewHolder objects (invoked by the layout manager) and set the view to use to
         * display it's content
         *
         * @param parent   :
         * @param viewType :
         * @return BookPhotoViewHolder :
         */
        @NonNull
        @Override
        public BookPhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            LayoutInflater li = LayoutInflater.from(parent.getContext());
            RelativeLayout rl = (RelativeLayout) li.inflate(R.layout.item_book_photo, parent, false);

            return new BookPhotoAdapter.BookPhotoViewHolder(rl);
        }

        /**
         * Replace the contents of a ViewHolder (invoked by the layout manager)
         *
         * @param holder   :
         * @param position :
         */
        @Override
        public void onBindViewHolder(@NonNull BookPhotoViewHolder holder, int position) {

            if (mPhotosName.size() > 0 && position < mPhotosName.size()) {
                String fileName = mPhotosName.get(position);
                StorageReference photoRef = bookImagesStorage.child(mBook.getBookId() + "/" + fileName);

                GlideApp.with(mActivity)
                        .load(photoRef)
                        .into(holder.iv);
                holder.ib.setTag(fileName);

                //onClick listener for the delete photo button
                holder.ib.setOnClickListener((v) -> {

                    if (mPhotosName.size() > 0) {

                        String tag = (String) v.getTag();
                        int ib_position = mPhotosName.indexOf(tag);

                        //check if thumbnail must be removed
                        if (tag.equals(mBook.getThumbnail()))
                            mBook.setThumbnail("");

                        mPhotosName.remove(ib_position);
                        notifyItemRemoved(ib_position);
                        mRemovedPhotos.add(tag);
                    }
                });
            } else {

                int relPosition = position - mPhotosName.size();

                Uri photoUri = this.mBookPhotosUri.get(relPosition);
                Bitmap bmPhoto;

                try {
                    bmPhoto = MediaStore.Images.Media.getBitmap(mContentResolver, photoUri);

                    holder.iv.setImageBitmap(bmPhoto);
                    holder.ib.setTag(photoUri);

                    //onClick listener for the delete photo button
                    holder.ib.setOnClickListener((v) -> {

                        if (mBookPhotosUri.size() > 0) {

                            Uri tag = (Uri) v.getTag();
                            int ib_position = mBookPhotosUri.indexOf(tag);

                            //check if thumbnail must be removed
                            if (tag.toString().equals(mBook.getThumbnail()))
                                mBook.setThumbnail("");

                            mBookPhotosUri.remove(ib_position);
                            notifyItemRemoved(ib_position + mPhotosName.size());
                        }
                    });
                } catch (IOException e) {
                    Log.d("error", "IOException when retrieving image Bitmap");
                    e.printStackTrace();
                }
            }
        }


        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return this.mPhotosName.size() + this.mBookPhotosUri.size();
        }
    }
}
