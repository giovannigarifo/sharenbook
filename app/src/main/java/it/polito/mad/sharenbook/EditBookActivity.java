package it.polito.mad.sharenbook;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
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
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import it.polito.mad.sharenbook.Utils.InputValidator;

public class EditBookActivity extends Activity {

    //context of the activity
    private Context context;

    // request codes to edit user photo
    private static final int REQUEST_CAMERA = 1;
    private static final int REQUEST_GALLERY = 2;
    private static final int MULTIPLE_PERMISSIONS = 3;
    private static final int REQUEST_CROP = 4;

    //views
    private TextView editbook_tv_isbn, editbook_tv_title, editbook_tv_subtitle, editbook_tv_authors,
            editbook_tv_publisher, editbook_tv_publishedDate, editbook_tv_description,
            editbook_tv_pageCount, editbook_tv_categories, editbook_tv_language, editbook_tv_bookConditions,
            editbook_tv_tags;

    private EditText editbook_et_isbn, editbook_et_title, editbook_et_subtitle, editbook_et_authors,
            editbook_et_publisher, editbook_et_publishedDate, editbook_et_description,
            editbook_et_pageCount, editbook_et_categories, editbook_et_language, editbook_et_bookConditions,
            editbook_et_tags;

    private FloatingActionButton editbook_fab_save;

    private ImageButton editbook_ib_addBookPhoto;

    private Button editbook_btn_addBookPhoto;

    private BottomNavigationView navBar;

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
    private DatabaseReference userBooksDb;
    private StorageReference bookImagesStorage;

    // ProgressDialog
    private ProgressDialog progressDialog;

    final static int MAX_ALLOWED_BOOK_PHOTO = 5;


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

        //retrieve book info
        if (savedInstanceState == null) {

            Bundle bundle = getIntent().getExtras();

            if (bundle == null) {

                Log.d("debug", "[EditBookActivity] no book received from ShareBookActivity");

            } else {

                book = bundle.getParcelable("book"); //retrieve book from intent
            }

        } else {

            book = savedInstanceState.getParcelable("book"); //retrieve book info from saveInstanceState
        }

        //Populate the view with all the information retrieved from Google Books API
        loadViewWithBookData();

        /*
         * register callbacks for buttons
         */

        editbook_fab_save.setOnClickListener((v) -> {
            Log.d("debug", "editbook_fab_save onClickListener fired");
            if (validateInputFields()) {
                firebaseSaveBook();
            }
        });

        editbook_ib_addBookPhoto.setOnClickListener((v) -> {

            Log.d("debug", "editbook_ib_addBookPhoto onClickListener fired");

            hasPermissions();//check permissions

            if (book.getBookPhotos().size() >= MAX_ALLOWED_BOOK_PHOTO)
                Toast.makeText(getApplicationContext(), getString(R.string.max_allowed_book_photo), Toast.LENGTH_LONG).show();
            else
                showSelectImageDialog();
        });

        editbook_btn_addBookPhoto.setOnClickListener((v) -> {

            Log.d("debug", "editbook_ib_addBookPhoto onClickListener fired");

            hasPermissions(); //check permissions

            if (book.getBookPhotos().size() >= MAX_ALLOWED_BOOK_PHOTO)
                Toast.makeText(getApplicationContext(), getString(R.string.max_allowed_book_photo), Toast.LENGTH_LONG).show();
            else
                showSelectImageDialog();
        });


        /*
         * Recycle View
         */
        editbook_rv_bookPhotos.setHasFixedSize(true); //improves performance

        // attach a Layout Manager to the RecyclerView, it's in charge of injecting views into the Recycler
        rvLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false);
        editbook_rv_bookPhotos.setLayoutManager(rvLayoutManager);

        // set an adapter for the RecyclerView, it's in charge of managing the ViewHolder objects
        rvAdapter = new BookPhotoAdapter(book.getBookPhotos(), context);
        editbook_rv_bookPhotos.setAdapter(rvAdapter);


        /*
         * navBar
         */

        //set correct navigation as selected item
        navBar.setSelectedItemId(R.id.navigation_shareBook);

        //set the listener for the navigation bar items
        navBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_logout:
                    //Toast.makeText(getApplicationContext(), "Selected Showcase!", Toast.LENGTH_SHORT).show();
                    AuthUI.getInstance()
                            .signOut(this)
                            .addOnCompleteListener(task -> {
                                Intent i = new Intent(getApplicationContext(), SplashScreenActivity.class);
                                startActivity(i);
                                Toast.makeText(getApplicationContext(), getString(R.string.log_out), Toast.LENGTH_SHORT).show();
                                finish();
                            });

                    break;

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

        // Setup progress dialog
        progressDialog = new ProgressDialog(EditBookActivity.this, ProgressDialog.STYLE_SPINNER);
    }

    @Override
    protected void onStart() {
        super.onStart();

        Log.d("debug", "onStart called by EditBookActivity");
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
    }


    /**
     * Loads all the info obtained from Google Books API into the view
     */
    private void loadViewWithBookData() {

        editbook_et_isbn.setText(book.getIsbn());
        editbook_et_title.setText(book.getTitle());
        editbook_et_subtitle.setText(book.getSubTitle());
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
            } else {
                //Toast.makeText(getApplicationContext(), getString(R.string.permission_already_granted), Toast.LENGTH_LONG).show();
            }
        }

        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
        }
    }


    /**
     * selectImage method
     */
    private void showSelectImageDialog() {

        final CharSequence items[] = {
                getString(R.string.photo_dialog_item_camera),
                getString(R.string.photo_dialog_item_gallery),
                getString(android.R.string.cancel)
        };

        final AlertDialog.Builder select = new AlertDialog.Builder(EditBookActivity.this); //give a context to Dialog

        select.setTitle(getString(R.string.photo_dialog_title));

        //set onclick listener, different intents for different items
        select.setItems(items, (dialogInterface, i) -> {

            if (items[i].equals(getString(R.string.photo_dialog_item_camera))) {

                //send intent to camera
                Intent takePhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (takePhoto.resolveActivity(getPackageManager()) != null) {

                    startActivityForResult(takePhoto, REQUEST_CAMERA);
                }

            } else if (items[i].equals(getString(R.string.photo_dialog_item_gallery))) {

                //send intent to gallery
                Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                if (gallery.resolveActivity(getPackageManager()) != null) {

                    gallery.setType("image/*");
                    startActivityForResult(Intent.createChooser(gallery, getString(R.string.photo_dialog_select_gallery_method_title)), REQUEST_GALLERY);
                }

            } else if (items[i].equals(getString(android.R.string.cancel))) {

                //dialog aborted
                dialogInterface.dismiss();
            }
        });

        //show dialog
        select.show();
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

                saveAndCropCameraPhoto(data);

            } else if (requestCode == REQUEST_GALLERY) {

                openAndCropGalleryPhoto(data);

            } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {

                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                Uri resultUri = result.getUri();

                try {

                    Bitmap croppedPhoto = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);

                    book.addBookPhoto(croppedPhoto); //add photo to collection (to position 0)

                    //notify update of the collection to Recycle View adapter
                    rvAdapter.notifyItemInserted(0);
                    rvLayoutManager.scrollToPosition(0);

                } catch (IOException e) {
                    Log.d("error", "IOException when retrieving cropped image from Uri");
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * saveAndCropCameraPhoto method
     *
     * @param data : Intent that contains the Bundle with the bitmap photo
     */
    private void saveAndCropCameraPhoto(Intent data) {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_" + book.getTitle() + ".jpg";

        Bundle extras = data.getExtras();

        if (extras != null) {

            try {

                Bitmap bitmap = (Bitmap) extras.get("data");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();

                if (bitmap != null)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

                String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, imageFileName, null);

                cropPhoto(Uri.parse(path));

            } catch (NullPointerException exc) {

                Toast.makeText(getApplicationContext(), getString(R.string.error_save_camera_photo), Toast.LENGTH_LONG).show();
            }
        }
    }


    /**
     * openAndCropGalleryphoto method
     *
     * @param data : the image selected from gallery
     */
    private void openAndCropGalleryPhoto(Intent data) {

        cropPhoto(data.getData()); //getData returns a Uri
    }


    /**
     * Method that sends a CROP intent for the photo identified by the Uri
     *
     * @param photoUri : uri of the photo to be cropped
     */
    public void cropPhoto(Uri photoUri) {

        CropImage.activity(photoUri)
                .setAllowRotation(true)
                .setAspectRatio(10, 15).setFixAspectRatio(true)
                .setAutoZoomEnabled(true)
                .start(this);
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
     * Validate user inputs
     */
    private boolean validateInputFields() {
        boolean isValid = true;

        if (InputValidator.isWrongIsbn(editbook_et_isbn)) {
            editbook_et_isbn.setError(getText(R.string.isbn_bad_format));
            isValid = false;
        }
        if (editbook_et_title.getText().toString().isEmpty()) {
            editbook_et_title.setError(getText(R.string.field_required));
            isValid = false;
        }
        if (editbook_et_authors.getText().toString().isEmpty()) {
            editbook_et_authors.setError(getText(R.string.field_required));
            isValid = false;
        }
        if (editbook_et_bookConditions.getText().toString().isEmpty()) {
            editbook_et_bookConditions.setError(getText(R.string.field_required));
            isValid = false;
        }

        return isValid;
    }

    /**
     * Save all data on FireBase
     */
    private void firebaseSaveBook() {
        // Retrieve all data
        HashMap<String, Object> bookData = new HashMap<>();
        bookData.put("owner_uid", firebaseUser.getUid());
        bookData.put("isbn", editbook_et_isbn.getText().toString());
        bookData.put("title", editbook_et_title.getText().toString());
        bookData.put("subtitle", editbook_et_subtitle.getText().toString());
        bookData.put("authors", commaStringToList(editbook_et_authors.getText().toString()));
        bookData.put("publisher", editbook_et_publisher.getText().toString());
        bookData.put("publishedDate", editbook_et_publishedDate.getText().toString());
        bookData.put("description", editbook_et_description.getText().toString());
        String pageCount = editbook_et_pageCount.getText().toString();
        if (pageCount.equals(""))
            bookData.put("pageCount", 0);
        else
            bookData.put("pageCount", Integer.parseInt(pageCount));
        bookData.put("categories", commaStringToList(editbook_et_categories.getText().toString()));
        bookData.put("language", editbook_et_language.getText().toString());
        bookData.put("bookConditions", editbook_et_bookConditions.getText().toString());
        bookData.put("tags", commaStringToList(editbook_et_tags.getText().toString()));

        // Show ProgressDialog
        progressDialog.setMessage(getText(R.string.default_saving_on_firebase));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Write on DB
        DatabaseReference newBookRef = booksDb.push();

        // Push newBook on "books" section
        newBookRef.updateChildren(bookData, (databaseError, databaseReference) -> {

            if (databaseError == null) {
                // Push newBook reference on "user_books" section
                userBooksDb.push().setValue(newBookRef.getKey(), (databaseError1, databaseReference1) -> {

                    progressDialog.dismiss();
                    if (databaseError1 == null) {
                        firebaseSavePhotos(newBookRef.getKey());
                        Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                        i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                        startActivity(i);
                        finish();
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
        ArrayList<Bitmap> photos = book.getBookPhotos();

        // Launch a task for every photo that should be updated
        for (int i = 0; i < photos.size(); i++) {
            final int num = i;

            // Compress bitmap into jpeg
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            photos.get(i).compress(Bitmap.CompressFormat.JPEG, 95, output);

            // Generate new file on firebase and write it
            StorageReference newFile = bookImagesStorage.child(bookKey + "/" + i + ".jpg");
            newFile.putBytes(output.toByteArray()).addOnSuccessListener(taskSnapshot -> {
                // Handle successful uploads
                Log.d("Debug","Photo n. " + num + " uploaded!");
            })
            .addOnFailureListener(exception -> {
                // Handle unsuccessful uploads
                Log.d("Debug","Error during upload of photo n. " + num);
            });
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
 *
 * BookPhotoAdapter class
 */

class BookPhotoAdapter extends RecyclerView.Adapter<BookPhotoAdapter.BookPhotoViewHolder> {

    private List<Bitmap> bookPhotos;
    private Context context;

    //constructor
    BookPhotoAdapter(ArrayList<Bitmap> bookPhotos, Context context) {
        this.bookPhotos = bookPhotos;
        this.context = context;
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
     * @param parent :
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
     * @param holder :
     * @param position :
     */
    @Override
    public void onBindViewHolder(@NonNull BookPhotoViewHolder holder, int position) {

        Bitmap bmPhoto = this.bookPhotos.get(position);

        ImageView iv = holder.item_book_photo_iv.findViewById(R.id.itembookphoto_iv_bookphoto);
        iv.setImageBitmap(bmPhoto);

        ImageButton ib = holder.item_book_photo_iv.findViewById(R.id.itembookphoto_ib_deletePhoto);
        ib.setTag(bmPhoto);

        //onClick listener for the delete photo button
        ib.setOnClickListener((v) -> {

            if (bookPhotos.size() > 0) {

                Bitmap bmp = (Bitmap) v.getTag();
                int ib_position = bookPhotos.indexOf(bmp);
                bookPhotos.remove(ib_position);
                notifyItemRemoved(ib_position);
            }
        });
    }


    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.bookPhotos.size();
    }

}
