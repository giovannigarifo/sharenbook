package it.polito.mad.lab1;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

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
            editbook_tv_pageCount, editbook_tv_categories, editbook_tv_language;

    private EditText editbook_et_isbn, editbook_et_title, editbook_et_subtitle, editbook_et_authors,
            editbook_et_publisher, editbook_et_publishedDate, editbook_et_description,
            editbook_et_pageCount, editbook_et_categories, editbook_et_language;

    private ImageView editbook_iv_thumbnail;

    private FloatingActionButton editbook_fab_save;

    private ImageButton editbook_ib_addBookPhoto;

    //book to share
    Book book;


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

                book = (Book) bundle.getParcelable("book"); //retrieve book from intent
            }

        } else {

            book = (Book) savedInstanceState.getParcelable("book"); //retrieve book info from saveInstanceState
        }

        //Populate the view with all the information retrieved from Google Books API
        loadViewWithBookData(book);


        /**
         * register callbacks for buttons
         */

        editbook_fab_save.setOnClickListener((v) -> {
            Log.d("debug", "editbook_fab_save onClickListener fired");
        });

        editbook_ib_addBookPhoto.setOnClickListener((v) -> {

            Log.d("debug", "editbook_ib_addBookPhoto onClickListener fired");
            showSelectImageDialog();
        });
    }


    /**
     * Saves the state of the activity when dealing with system wide eents (e.g. rotation)
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
     *
     * @param book : the Book object that encapsulate all the book infos
     */
    private void loadViewWithBookData(Book book) {

        editbook_et_isbn.setText(book.getIsbn());
        editbook_et_title.setText(book.getTitle());
        editbook_et_subtitle.setText(book.getSubTitle());
        editbook_et_publisher.setText(book.getPublisher());
        editbook_et_publishedDate.setText(book.getPublishedDate());
        editbook_et_description.setText(book.getDescription());
        editbook_et_language.setText(book.getLanguage());
        editbook_et_pageCount.setText(Integer.valueOf(book.getPageCount()).toString());

        //authors to comma separated string
        String[] a_arr = book.getAuthors();

        if (a_arr.length == 1) {

            editbook_et_authors.setText(book.getAuthors()[0]);

        } else if (a_arr.length > 1) {

            StringBuilder sb = new StringBuilder();

            for (String s : a_arr) {
                sb.append(s).append(", ");
            }

            editbook_et_authors.setText(sb.deleteCharAt(sb.length() - 1).toString());
        }

        //categories to comma separated string
        String[] c_arr = book.getCategories();

        if (c_arr.length == 1) {

            editbook_et_categories.setText(book.getCategories()[0]);

        } else if (a_arr.length > 1) {

            StringBuilder sb = new StringBuilder();

            for (String s : c_arr) {
                sb.append(s).append(", ");
            }

            editbook_et_categories.setText(sb.deleteCharAt(sb.length() - 1).toString());
        }

        //load book cover from thumbnail
        editbook_iv_thumbnail = (ImageView) findViewById(R.id.editbook_iv_thumbnail);
        Glide.with(this).load(book.getThumbnail()).into(editbook_iv_thumbnail);

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
                Intent selfie = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if (selfie.resolveActivity(getPackageManager()) != null) {

                    startActivityForResult(selfie, REQUEST_CAMERA);
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

            } else if (requestCode == REQUEST_CROP) {

                Bundle extras = data.getExtras();
                Bitmap croppedPhoto;

                try {

                    croppedPhoto = extras.getParcelable("data");

                    //add photo to collection and display it in view
                    book.addBookPhoto(croppedPhoto);
                    editbook_iv_thumbnail.setImageBitmap(croppedPhoto);

                } catch (NullPointerException exc) {

                    Log.d("exception", "NullPointerException when getting cropped image from parcel");
                    exc.printStackTrace();
                }

            }
        }
    }


    /**
     * saveAndCropCameraPhoto method
     *
     * @param data
     */
    private void saveAndCropCameraPhoto(Intent data) {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_" + book.getTitle() + ".jpg";
        Bundle extras = data.getExtras();

        if (extras != null) {

            Bitmap bitmap = (Bitmap) extras.get("data");

            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
            String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), bitmap, imageFileName, null);

            cropPhoto(Uri.parse(path));
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

        try {

            Intent cropIntent = new Intent("com.android.camera.action.CROP");

            cropIntent.setDataAndType(photoUri, "image/*");
            cropIntent.putExtra("crop", "true");
            cropIntent.putExtra("aspectX", 10);
            cropIntent.putExtra("aspectY", 15);
            cropIntent.putExtra("outputX", 100);
            cropIntent.putExtra("outputY", 150);
            cropIntent.putExtra("return-data", true);

            startActivityForResult(cropIntent, REQUEST_CROP);

        } catch (ActivityNotFoundException exc) { //device doesn't support crop functionality

            Log.d("debug", "Unable to crop the image, activity not found.");
        }
    }


    /**
     * getViewsAndSetTypography method
     */
    private void getViewsAndSetTypography() {

        //get buttons
        editbook_fab_save = (FloatingActionButton) findViewById(R.id.editbook_fab_save);
        editbook_ib_addBookPhoto = (ImageButton) findViewById(R.id.editbook_ib_addBookPhoto);

        //get the rest of views
        editbook_tv_isbn = (TextView) findViewById(R.id.editbook_tv_isbn);
        editbook_tv_title = (TextView) findViewById(R.id.editbook_tv_title);
        editbook_tv_subtitle = (TextView) findViewById(R.id.editbook_tv_subtitle);
        editbook_tv_authors = (TextView) findViewById(R.id.editbook_tv_authors);
        editbook_tv_publisher = (TextView) findViewById(R.id.editbook_tv_publisher);
        editbook_tv_publishedDate = (TextView) findViewById(R.id.editbook_tv_publishedDate);
        editbook_tv_description = (TextView) findViewById(R.id.editbook_tv_description);
        editbook_tv_pageCount = (TextView) findViewById(R.id.editbook_tv_pageCount);
        editbook_tv_categories = (TextView) findViewById(R.id.editbook_tv_categories);
        editbook_tv_language = (TextView) findViewById(R.id.editbook_tv_language);

        editbook_et_isbn = (EditText) findViewById(R.id.editbook_et_isbn);
        editbook_et_title = (EditText) findViewById(R.id.editbook_et_title);
        editbook_et_subtitle = (EditText) findViewById(R.id.editbook_et_subtitle);
        editbook_et_authors = (EditText) findViewById(R.id.editbook_et_authors);
        editbook_et_publisher = (EditText) findViewById(R.id.editbook_et_publisher);
        editbook_et_publishedDate = (EditText) findViewById(R.id.editbook_et_publishedDate);
        editbook_et_description = (EditText) findViewById(R.id.editbook_et_description);
        editbook_et_pageCount = (EditText) findViewById(R.id.editbook_et_pageCount);
        editbook_et_categories = (EditText) findViewById(R.id.editbook_et_categories);
        editbook_et_language = (EditText) findViewById(R.id.editbook_et_language);

        /**
         * set typography
         */

        //retrieve fonts
        Typeface robotoBold = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Bold.ttf");
        Typeface robotoLight = Typeface.createFromAsset(getAssets(), "fonts/Roboto-Light.ttf");

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

    }
}
