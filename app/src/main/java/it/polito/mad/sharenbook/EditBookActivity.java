package it.polito.mad.sharenbook;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import android.widget.TextView;
import android.widget.Toast;

import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Client;
import com.algolia.search.saas.CompletionHandler;
import com.algolia.search.saas.Index;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.polito.mad.sharenbook.Utils.ImageUtils;
import it.polito.mad.sharenbook.Utils.InputValidator;
import it.polito.mad.sharenbook.Utils.UserInterface;
import it.polito.mad.sharenbook.model.Book;

public class EditBookActivity extends Activity {

    // request codes to edit user photo
    private static final int REQUEST_CAMERA = ImageUtils.REQUEST_CAMERA;
    private static final int REQUEST_GALLERY = ImageUtils.REQUEST_GALLERY;
    private static final int MULTIPLE_PERMISSIONS = 3;

    final static int MAX_ALLOWED_BOOK_PHOTO = 5;
    final static int MIN_REQUIRED_BOOK_PHOTO = 1;

    // context of the activity
    private Context context;

    // views
    private TextView editbook_tv_isbn, editbook_tv_title, editbook_tv_subtitle, editbook_tv_authors,
            editbook_tv_publisher, editbook_tv_publishedDate, editbook_tv_description,
            editbook_tv_pageCount, editbook_tv_categories, editbook_tv_language, editbook_tv_bookConditions,
            editbook_tv_tags;

    private EditText editbook_et_isbn, editbook_et_title, editbook_et_subtitle, editbook_et_authors,
            editbook_et_publisher, editbook_et_publishedDate, editbook_et_description,
            editbook_et_pageCount, editbook_et_categories, editbook_et_language, editbook_et_bookConditions,
            editbook_et_tags;

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

    //permissions needed
    private String[] permissions = new String[]{
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    //information of the book to be shared
    Book book;

    // FireBase objects
    private FirebaseUser firebaseUser;
    private DatabaseReference booksDb;
    private DatabaseReference newBookRef; //the unique key obtained by firebase
    private DatabaseReference userBooksDb;
    private StorageReference bookImagesStorage;

    // Algolia objects
    Client algoliaClient;
    Index algoliaIndex;

    // ProgressDialog
    private ProgressDialog progressDialog;
    int photoLoaded;


    /**
     * onCreate callback
     *
     * @param savedInstanceState : bundle that contains activity state information (null or the book)
     */
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_book);
        context = this.getApplicationContext();
        getViewsAndSetTypography(); //retrieve references to views objects and change default fonts

        // Setup image utils class
        imageUtils = new ImageUtils(this);

        //retrieve book info
        if (savedInstanceState == null) {

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
        }

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

        editbook_ib_addBookPhoto.setOnClickListener(this::addBookPhoto);
        editbook_btn_addBookPhoto.setOnClickListener(this::addBookPhoto);

        /*
         * Recycle View
         */
        editbook_rv_bookPhotos.setHasFixedSize(true); //improves performance

        // attach a Layout Manager to the RecyclerView, it's in charge of injecting views into the Recycler
        rvLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        editbook_rv_bookPhotos.setLayoutManager(rvLayoutManager);

        // set an adapter for the RecyclerView, it's in charge of managing the ViewHolder objects
        rvAdapter = new BookPhotoAdapter(book.getBookPhotosUri(), this.getContentResolver());
        editbook_rv_bookPhotos.setAdapter(rvAdapter);


        /*
         * navBar
         */

        //set correct navigation as selected item
        navBar.setSelectedItemId(R.id.navigation_shareBook);

        //set the listener for the navigation bar items
        navBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_profile:
                    Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                    finish();
                    break;

                case R.id.navigation_shareBook:
                    break;
                case R.id.navigation_myBook:
                    Intent my_books = new Intent(getApplicationContext(), MyBookActivity.class);
                    startActivity(my_books);
                    finish();
                    break;
            }
            return true;
        });

        // Setup FireBase
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        booksDb = firebaseDatabase.getReference(getString(R.string.books_key));

        userBooksDb = firebaseDatabase.getReference(getString(R.string.users_key)).child(firebaseUser.getUid()).child(getString(R.string.user_books_key));
        bookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));

        // Setup Algolia
        algoliaClient = new Client("4DWHVL57AK", "03391b3ea81e4a5c37651a677670bcb8");
        algoliaIndex = algoliaClient.getIndex("books");

        // Setup progress dialog
        progressDialog = new ProgressDialog(EditBookActivity.this, ProgressDialog.STYLE_SPINNER);
    }


    /*
     * Update the Book object with the final information inserted by the user in the edittexts
     */
    private void updateBookWithUserInfo() {

        book.setIsbn(editbook_et_isbn.getText().toString());
        book.setTitle(editbook_et_title.getText().toString());
        book.setSubtitle(editbook_et_subtitle.getText().toString());
        book.setAuthors(commaStringToList(editbook_et_authors.getText().toString()));
        book.setPublisher(editbook_et_publisher.getText().toString());
        book.setPublishedDate(editbook_et_publishedDate.getText().toString());
        book.setDescription(editbook_et_description.getText().toString());

        String pageCount = editbook_et_pageCount.getText().toString();

        if (pageCount.equals(""))
            book.setPageCount(0);
        else
            book.setPageCount(Integer.parseInt(pageCount));

        book.setCategories(commaStringToList(editbook_et_categories.getText().toString()));
        book.setLanguage(editbook_et_language.getText().toString());
        book.setBookConditions(editbook_et_bookConditions.getText().toString());
        book.setTags(commaStringToList(editbook_et_tags.getText().toString()));
    }


    /*
     * Add Book photo button listener
     */
    private void addBookPhoto(View v) {

        Log.d("debug", "editbook_ib_addBookPhoto onClickListener fired");

        if (book.getBookPhotosUri().size() >= MAX_ALLOWED_BOOK_PHOTO) {

            showToast(getString(R.string.max_allowed_book_photo));

        } else {

            hasPermissions();//check permissions
            imageUtils.showSelectImageDialog();
        }
    }


    /**
     * onResume callback
     */
    @Override
    protected void onResume() {
        super.onResume();

        //set correct navigation as selected item
        navBar.setSelectedItemId(R.id.navigation_shareBook);
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
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Loads all the info obtained from Google Books API into the view
     */
    private void loadViewWithBookData() {

        editbook_et_isbn.setText(book.getIsbn());
        editbook_et_title.setText(book.getTitle());
        editbook_et_subtitle.setText(book.getSubtitle());
        editbook_et_publisher.setText(book.getPublisher());
        editbook_et_publishedDate.setText(book.getPublishedDate());
        editbook_et_description.setText(book.getDescription());
        editbook_et_language.setText(book.getLanguage());
        if (book.getPageCount() != -1)
            editbook_et_pageCount.setText(Integer.valueOf(book.getPageCount()).toString());

        //authors to comma separated string
        List<String> a_arr = book.getAuthors();

        if (a_arr.size() == 1) {

            editbook_et_authors.setText(book.getAuthors().get(0));

        } else if (a_arr.size() > 1) {

            StringBuilder sb = new StringBuilder();

            String prefix = "";
            for (String s : a_arr) {
                sb.append(prefix);
                prefix = ", ";
                sb.append(s);
            }

            editbook_et_authors.setText(sb.toString());
        }

        //categories to comma separated string
        List<String> c_arr = book.getCategories();

        if (c_arr.size() == 1) {

            editbook_et_categories.setText(book.getCategories().get(0));

        } else if (c_arr.size() > 1) {

            StringBuilder sb = new StringBuilder();

            String prefix = "";
            for (String s : c_arr) {
                sb.append(prefix);
                prefix = ", ";
                sb.append(s);
            }

            editbook_et_categories.setText(sb.toString());
        }
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
     * hasPermissions method
     */
    private void hasPermissions() {

        int result;

        List<String> listPermissionsNeeded = new ArrayList<>();

        for (String permission : permissions) {
            result = ContextCompat.checkSelfPermission(context, permission);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(permission);
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
        }
    }


    /**
     * onActivityResult method, callback fired each time an intent returns his result
     *
     * @param requestCode : the kind of intent requested
     * @param resultCode  : result code of the intet
     * @param data        : data returned by the intnet
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_CAMERA) {

                imageUtils.dispatchCropCurrentPhotoIntent(ImageUtils.ASPECT_RATIO_PHOTO_PORT);
                imageUtils.revokeCurrentPhotoUriPermission();

            } else if (requestCode == REQUEST_GALLERY) {

                imageUtils.dispatchCropPhotoIntent(data.getData(), ImageUtils.ASPECT_RATIO_PHOTO_PORT);

            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

                Uri resultUri = CropImage.getActivityResult(data).getUri();

                try {
                    Uri resizedPhotoUri = ImageUtils.resizeJpegPhoto(this, ImageUtils.EXTERNAL_CACHE, resultUri, 540);
                    book.addBookPhotoUri(resizedPhotoUri); //add photo uri to collection (to position 0)

                    //notify update of the collection to Recycle View adapter
                    rvAdapter.notifyItemInserted(0);
                    rvLayoutManager.scrollToPosition(0);

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
        // Retrieve all data
        HashMap<String, Object> bookData = new HashMap<>();
        bookData.put("owner_uid", firebaseUser.getUid());
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
        bookData.put("thumbnail", book.getThumbnail());
        bookData.put("numPhotos", book.getBookPhotosUri().size());

        // Show ProgressDialog
        progressDialog.setMessage(getText(R.string.default_saving_on_firebase));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Write on DB
        newBookRef = booksDb.push();

        // Push newBook on "books" section
        newBookRef.updateChildren(bookData, (databaseError, databaseReference) -> {

            if (databaseError == null) {
                // Push newBook reference on "user_books" section
                userBooksDb.push().setValue(newBookRef.getKey(), (databaseError1, databaseReference1) -> {

                    if (databaseError1 == null) {
                        firebaseSavePhotos(newBookRef.getKey());
                    } else {
                        Toast.makeText(getApplicationContext(), "An error occurred, try later.", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "An error occurred, try later.", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Save al photos on firebase
     */
    private void firebaseSavePhotos(String bookKey) {
        // Get list of photos
        List<Uri> photos = book.getBookPhotosUri();
        List<Task<UploadTask>> taskList = new ArrayList<>();
        photoLoaded = 0;
        int numPhotos = photos.size();

        // Show message "uploading books"
        updateProgressDialogMessage(++photoLoaded, numPhotos);

        // Launch a task for every photo that should be updated
        for (int i = 0; i < photos.size(); i++) {
            final int num = i;

            // Generate new file on firebase and write it
            StorageReference newFile = bookImagesStorage.child(bookKey + "/" + i + ".jpg");
            Task newTask = newFile.putFile(photos.get(i)).addOnSuccessListener(taskSnapshot -> {
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
            progressDialog.dismiss();
            Toast.makeText(getApplicationContext(), R.string.default_book_saved, Toast.LENGTH_LONG).show();
            Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(i);
            finish();
        });
    }


    /*
     * Add the book to the Algolia Index so that it can be searched
     */
    private void algoliaIndexBook() {

        try {

            Index index = algoliaClient.getIndex("books");

            JSONObject bookData = new JSONObject()
                    .put("owner_uid", firebaseUser.getUid())
                    .put("isbn", book.getIsbn())
                    .put("title", book.getTitle())
                    .put("subtitle", book.getSubtitle())
                    .put("authors", book.getAuthors())
                    .put("publisher", book.getPublisher())
                    .put("publishedDate", book.getPublishedDate())
                    .put("description", book.getDescription())
                    .put("pageCount", book.getPageCount())
                    .put("categories", book.getCategories())
                    .put("language", book.getLanguage())
                    .put("bookConditions", book.getBookConditions())
                    .put("tags", book.getTags())
                    .put("thumbnail", book.getThumbnail())
                    .put("numPhotos", book.getBookPhotosUri().size());


            JSONObject ob = new JSONObject();
            ob.put(newBookRef.getKey(), bookData);

            index.addObjectAsync( ob ,
                    new CompletionHandler() {
                        @Override
                        public void requestCompleted(JSONObject jsonObject, AlgoliaException e) {

                            Log.d("debug", "Algolia request completed.");
                        }
                    }
            );
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
        if (editbook_et_bookConditions.getText().toString().isEmpty()) {
            editbook_et_bookConditions.setError(getText(R.string.field_required));
            isValid = false;
            if (!alreadyFocused) {
                editbook_et_bookConditions.requestFocus();
                alreadyFocused = true;
            }
        }
        if (book.getBookPhotosUri().size() < MIN_REQUIRED_BOOK_PHOTO) {
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
            progressDialog.setMessage(msg);
        }
    }

    /**
     * Convert a comma separated multiple words string to a string list
     */
    private List<String> commaStringToList(String commaString) {
        List<String> stringList = new ArrayList<>();
        for (String s : commaString.split(",")) {
            stringList.add(s.trim());
        }
        return stringList;
    }

    /**
     * getViewsAndSetTypography method
     */
    private void getViewsAndSetTypography() {

        //get scrollview
        editbook_scrollview = findViewById(R.id.editbook_scrollview);

        //get navbar
        navBar = (BottomNavigationView) findViewById(R.id.navigation);

        //get buttons
        editbook_fab_save = findViewById(R.id.editbook_fab_save);
        editbook_ib_addBookPhoto = findViewById(R.id.editbook_ib_addBookPhoto);
        editbook_btn_addBookPhoto = findViewById(R.id.editbook_tv_addBookPhoto);

        //get recycle views
        editbook_rv_bookPhotos = findViewById(R.id.editbook_rv_bookPhotos);

        //get the rest of views
        editbook_tv_isbn = findViewById(R.id.editbook_tv_isbn);
        editbook_tv_title = findViewById(R.id.editbook_tv_title);
        editbook_tv_subtitle = findViewById(R.id.editbook_tv_subtitle);
        editbook_tv_authors = findViewById(R.id.editbook_tv_authors);
        editbook_tv_publisher = findViewById(R.id.editbook_tv_publisher);
        editbook_tv_publishedDate = findViewById(R.id.editbook_tv_publishedDate);
        editbook_tv_description = findViewById(R.id.editbook_tv_description);
        editbook_tv_pageCount = findViewById(R.id.editbook_tv_pageCount);
        editbook_tv_categories = findViewById(R.id.editbook_tv_categories);
        editbook_tv_language = findViewById(R.id.editbook_tv_language);
        editbook_tv_bookConditions = findViewById(R.id.editbook_tv_bookConditions);
        editbook_tv_tags = findViewById(R.id.editbook_tv_tags);

        editbook_et_isbn = findViewById(R.id.editbook_et_isbn);
        editbook_et_title = findViewById(R.id.editbook_et_title);
        editbook_et_subtitle = findViewById(R.id.editbook_et_subtitle);
        editbook_et_authors = findViewById(R.id.editbook_et_authors);
        editbook_et_publisher = findViewById(R.id.editbook_et_publisher);
        editbook_et_publishedDate = findViewById(R.id.editbook_et_publishedDate);
        editbook_et_description = findViewById(R.id.editbook_et_description);
        editbook_et_pageCount = findViewById(R.id.editbook_et_pageCount);
        editbook_et_categories = findViewById(R.id.editbook_et_categories);
        editbook_et_language = findViewById(R.id.editbook_et_language);
        editbook_et_bookConditions = findViewById(R.id.editbook_et_bookConditions);
        editbook_et_tags = findViewById(R.id.editbook_et_tags);

        /*
         * set typography
         */


        //retrieve fonts
        Typeface robotoBold = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");
        Typeface robotoLight = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

        //buttons
        editbook_btn_addBookPhoto.setTypeface(robotoBold);

        //headings
        editbook_tv_isbn.setTypeface(robotoBold);
        editbook_tv_title.setTypeface(robotoBold);
        editbook_tv_subtitle.setTypeface(robotoBold);
        editbook_tv_authors.setTypeface(robotoBold);
        editbook_tv_publisher.setTypeface(robotoBold);
        editbook_tv_publishedDate.setTypeface(robotoBold);
        editbook_tv_description.setTypeface(robotoBold);
        editbook_tv_pageCount.setTypeface(robotoBold);
        editbook_tv_categories.setTypeface(robotoBold);
        editbook_tv_language.setTypeface(robotoBold);
        editbook_tv_bookConditions.setTypeface(robotoBold);
        editbook_tv_tags.setTypeface(robotoBold);

        //edit texts
        editbook_et_isbn.setTypeface(robotoLight);
        editbook_et_title.setTypeface(robotoLight);
        editbook_et_subtitle.setTypeface(robotoLight);
        editbook_et_authors.setTypeface(robotoLight);
        editbook_et_publisher.setTypeface(robotoLight);
        editbook_et_publishedDate.setTypeface(robotoLight);
        editbook_et_description.setTypeface(robotoLight);
        editbook_et_pageCount.setTypeface(robotoLight);
        editbook_et_categories.setTypeface(robotoLight);
        editbook_et_language.setTypeface(robotoLight);
        editbook_et_bookConditions.setTypeface(robotoLight);
        editbook_et_tags.setTypeface(robotoLight);
    }
}


/**
 * BookPhotoAdapter class
 */

class BookPhotoAdapter extends RecyclerView.Adapter<BookPhotoAdapter.BookPhotoViewHolder> {

    private List<Uri> bookPhotosUri;
    private ContentResolver mContentResolver;

    //constructor
    BookPhotoAdapter(List<Uri> bookPhotosUri, ContentResolver contentResolver) {
        this.bookPhotosUri = bookPhotosUri;
        this.mContentResolver = contentResolver;
    }

    //Inner Class that provides a reference to the views for each data item of the collection
    class BookPhotoViewHolder extends RecyclerView.ViewHolder {

        private RelativeLayout item_book_photo_iv;

        BookPhotoViewHolder(RelativeLayout rl) {
            super(rl);
            this.item_book_photo_iv = rl;
        }
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

        Uri photoUri = this.bookPhotosUri.get(position);
        Bitmap bmPhoto;

        try {
            bmPhoto = MediaStore.Images.Media.getBitmap(mContentResolver, photoUri);

            ImageView iv = holder.item_book_photo_iv.findViewById(R.id.itembookphoto_iv_bookphoto);
            iv.setImageBitmap(bmPhoto);

            ImageButton ib = holder.item_book_photo_iv.findViewById(R.id.itembookphoto_ib_deletePhoto);
            ib.setTag(photoUri);

            //onClick listener for the delete photo button
            ib.setOnClickListener((v) -> {

                if (bookPhotosUri.size() > 0) {

                    Uri uri = (Uri) v.getTag();
                    int ib_position = bookPhotosUri.indexOf(uri);
                    bookPhotosUri.remove(ib_position);
                    notifyItemRemoved(ib_position);
                }
            });
        } catch (IOException e) {
            Log.d("error", "IOException when retrieving image Bitmap");
            e.printStackTrace();
        }
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.bookPhotosUri.size();
    }

}
