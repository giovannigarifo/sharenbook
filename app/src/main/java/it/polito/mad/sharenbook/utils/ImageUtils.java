package it.polito.mad.sharenbook.utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import it.polito.mad.sharenbook.R;

import static android.os.Environment.getExternalStoragePublicDirectory;

public class ImageUtils {

    // ImageFolder constants
    public static final int EXTERNAL_PICTURES = 1;          // Android/data/it.polito.mad.sharenbook/files/Pictures
    public static final int EXTERNAL_PUBLIC_PICTURES = 2;   // Pictures/Share'nBook
    public static final int EXTERNAL_CACHE = 3;             // Android/data/it.polito.mad.sharenbook/cache
    // Intent request code
    public static final int REQUEST_CAMERA = 1;
    public static final int REQUEST_GALLERY = 2;
    // Aspect Ratio constant
    public static final int ASPECT_RATIO_FREE = 0;
    public static final int ASPECT_RATIO_PHOTO_PORT = 1;
    public static final int ASPECT_RATIO_PHOTO_LAND = 2;
    public static final int ASPECT_RATIO_SQUARE = 3;
    public static final int ASPECT_RATIO_CIRCLE = 4;

    private Context mContext;
    private Activity mActivity;
    private Uri currentPhotoUri;

    /**
     * Constructor
     *
     * @param activity : Activity object caller
     */
    public ImageUtils(Activity activity) {
        this.mContext = activity.getApplicationContext();
        this.mActivity = activity;
        this.currentPhotoUri = Uri.parse("");
    }

    public Uri getCurrentPhotoUri() {
        return currentPhotoUri;
    }

    public void setCurrentPhotoUri(Uri currentPhotoUri) {
        this.currentPhotoUri = currentPhotoUri;
    }

    /**
     * Dispatch an intent to take a photo saving it on storage
     *
     * @param imageFolder : constant representing folder where to save
     */
    public void dispatchTakePhotoIntent(int imageFolder) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(mContext.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile(mActivity, imageFolder);
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d("DEBUG", "Can't take picture now: error during tempfile generation.");
                ex.printStackTrace();
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                // Get new created file uri
                currentPhotoUri = getUriForFile(mActivity, photoFile);

                // Give Uri Permission to camera (solve camera crash on older android version < 5)
                List<ResolveInfo> resolvedIntentActivities = mContext.getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolvedIntentInfo : resolvedIntentActivities) {
                    String packageName = resolvedIntentInfo.activityInfo.packageName;
                    mContext.grantUriPermission(packageName, currentPhotoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                // Dispatch camera intent
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri);
                mActivity.startActivityForResult(takePictureIntent, REQUEST_CAMERA);
            }
        }
    }

    /**
     * Dispatch an intent to crop passed photo
     *
     * @param photoUri : uri of passed photo
     */
    public void dispatchCropPhotoIntent(Uri photoUri, int aspectRatio) {
        CropImage.ActivityBuilder cropActivity = CropImage.activity(photoUri)
                .setAllowRotation(true)
                .setFixAspectRatio(true)
                .setAutoZoomEnabled(true);

        switch (aspectRatio) {
            case ImageUtils.ASPECT_RATIO_FREE:
                cropActivity.setFixAspectRatio(false);
            case ImageUtils.ASPECT_RATIO_PHOTO_PORT:
                cropActivity.setAspectRatio(10, 15);
                break;
            case ImageUtils.ASPECT_RATIO_PHOTO_LAND:
                cropActivity.setAspectRatio(15, 10);
                break;
            case ImageUtils.ASPECT_RATIO_SQUARE:
                cropActivity.setAspectRatio(1, 1);
                break;
            case ImageUtils.ASPECT_RATIO_CIRCLE:
                cropActivity.setAspectRatio(1, 1);
                cropActivity.setCropShape(CropImageView.CropShape.OVAL);
                break;
            default:
                cropActivity.setFixAspectRatio(false);
        }

        cropActivity.start(mActivity);
    }

    /**
     * Overload for dispatchCropPhotoIntent (using current photo Uri)
     */
    public void dispatchCropCurrentPhotoIntent(int aspectRatio) {
        dispatchCropPhotoIntent(currentPhotoUri, aspectRatio);
    }

    /**
     * Dispatch an intent to open gallery dialog
     */
    private void dispatchGetImageFromGalleryIntent() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (gallery.resolveActivity(mContext.getPackageManager()) != null) {
            gallery.setType("image/*");
            mActivity.startActivityForResult(Intent.createChooser(gallery, mContext.getString(R.string.photo_dialog_select_gallery_method_title)), REQUEST_GALLERY);
        }
    }

    /**
     * Show an alert dialog for choosing image source;
     * it automatically dispatch corresponding intent
     */
    public void showSelectImageDialog() {
        final CharSequence items[] = {
                mContext.getString(R.string.photo_dialog_item_camera),
                mContext.getString(R.string.photo_dialog_item_gallery),
                mContext.getString(android.R.string.cancel)
        };
        final AlertDialog.Builder selectDialog = new AlertDialog.Builder(mActivity);

        // configure select dialog
        selectDialog.setTitle(mContext.getString(R.string.photo_dialog_title));
        selectDialog.setItems(items, (dialogInterface, i) -> {
            if (items[i].equals(mContext.getString(R.string.photo_dialog_item_camera))) {
                dispatchTakePhotoIntent(EXTERNAL_PUBLIC_PICTURES);
            } else if (items[i].equals(mContext.getString(R.string.photo_dialog_item_gallery))) {
                dispatchGetImageFromGalleryIntent();
            } else if (items[i].equals(mContext.getString(android.R.string.cancel))) {
                dialogInterface.dismiss();
            }
        });
        selectDialog.show();
    }

    /**
     * Revoke uri permission given to camera intents due to compatibility reason with android versions < 5
     */
    public void revokeCurrentPhotoUriPermission() {
        mContext.revokeUriPermission(currentPhotoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
    }

    /**
     * Resize passed photo to given dimension (maintain aspect ratio if one of dimension is 0)
     *
     * @param activity  : activity object caller
     * @param photoUri  : uri of image to be resized
     * @param newWidth  : width dimension in pixel (can be 0 if newHeight is given)
     * @param newHeight : height dimension in pixel (can be 0 if newWidth is given)
     * @param forceCopy : force photo copy even if original dimension is lower than requested
     * @return : uri of resized photo
     * @throws IOException
     */
    public static Uri resizeJpegPhoto(Activity activity, int imageFolder, Uri photoUri, int newWidth, int newHeight, boolean forceCopy) throws IOException {
        Bitmap photo = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), photoUri);

        if (newWidth == 0 && newHeight == 0) {
            return photoUri;
        } else if (newHeight == 0) {
            if (photo.getWidth() > newWidth)
                newHeight = (newWidth * photo.getHeight()) / photo.getWidth();
            else if (forceCopy)
                newHeight = photo.getHeight();
            else return photoUri;
        } else if (newWidth == 0) {
            if (photo.getHeight() > newHeight)
                newWidth = (newHeight * photo.getWidth()) / photo.getHeight();
            else if (forceCopy)
                newWidth = photo.getWidth();
            else return photoUri;
        }

        Bitmap resizedPhoto = Bitmap.createScaledBitmap(photo, newWidth, newHeight, false);

        // Compress bitmap into jpeg
        File outputFile = createImageFile(activity, imageFolder);
        FileOutputStream outputFileStream = new FileOutputStream(outputFile);
        resizedPhoto.compress(Bitmap.CompressFormat.JPEG, 95, outputFileStream);

        // Clean resources
        photo.recycle();
        resizedPhoto.recycle();
        outputFileStream.close();

        return getUriForFile(activity, outputFile);
    }


    /**
     * Overload -> Resize passed photo to given width dimension (maintaining aspect ratio)
     *
     * @param activity    : activity object caller
     * @param imageFolder : constant representing folder where to save
     * @param photoUri    : uri of image to be resized
     * @param newWidth    : width dimension in pixel
     * @param forceCopy : force photo copy even if original dimension is lower than requested
     * @return : uri of resized photo
     * @throws IOException
     */
    public static Uri resizeJpegPhoto(Activity activity, int imageFolder, Uri photoUri, int newWidth, boolean forceCopy) throws IOException {
        return resizeJpegPhoto(activity, imageFolder, photoUri, newWidth, 0, forceCopy);
    }

    /**
     * Overload -> Resize passed photo to given width dimension (maintaining aspect ratio) without forcing copy
     *
     * @param activity    : activity object caller
     * @param imageFolder : constant representing folder where to save
     * @param photoUri    : uri of image to be resized
     * @param newWidth    : width dimension in pixel
     * @return : uri of resized photo
     * @throws IOException
     */
    public static Uri resizeJpegPhoto(Activity activity, int imageFolder, Uri photoUri, int newWidth) throws IOException {
        return resizeJpegPhoto(activity, imageFolder, photoUri, newWidth, 0, false);
    }


    /**
     * Stretch passed photo to given aspect ratio
     *
     * @param activity    : activity object caller
     * @param imageFolder : constant representing folder where to save
     * @param photoUri    : uri of image to be stretched
     * @param aspectRatio : constant representing aspectRatio
     * @return : uri of resized photo
     * @throws IOException
     */
    public static Uri stretchJpegPhoto(Activity activity, int imageFolder, Uri photoUri, int aspectRatio) throws IOException {
        Bitmap photo = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), photoUri);

        int newWidth, newHeight;
        int pWidth = photo.getWidth();
        int pHeight = photo.getHeight();

        switch (aspectRatio) {
            case ImageUtils.ASPECT_RATIO_PHOTO_PORT:
                if (pHeight / pWidth == 1.5) {
                    newWidth = pWidth;
                    newHeight = pHeight;
                }
                else if (pHeight / pWidth < 1.5) {
                    newWidth = Math.round((float) pHeight / 15 * 10);
                    newHeight = pHeight;
                } else {
                    newWidth = pWidth;
                    newHeight = Math.round((float) pWidth / 10 * 15);
                }
                break;
            default:
                newWidth = pWidth;
                newHeight = pHeight;
                break;
        }

        Bitmap resizedPhoto = Bitmap.createScaledBitmap(photo, newWidth, newHeight, false);

        // Compress bitmap into jpeg
        File outputFile = createImageFile(activity, imageFolder);
        FileOutputStream outputFileStream = new FileOutputStream(outputFile);
        resizedPhoto.compress(Bitmap.CompressFormat.JPEG, 95, outputFileStream);

        // Clean resources
        photo.recycle();
        resizedPhoto.recycle();
        outputFileStream.close();

        return getUriForFile(activity, outputFile);
    }


    /**
     * Resize passed bitmap to given dimension (maintain aspect ratio if one of dimension is 0)
     *
     * @param photo     : bitmap image to be resized
     * @param newWidth  : width dimension in pixel (can be 0 if newHeight is given)
     * @param newHeight : height dimension in pixel (can be 0 if newWidth is given)
     * @return : resized bitmap image
     */
    public static Bitmap resizeBitmapPhoto(Bitmap photo, int newWidth, int newHeight) {
        if (newWidth == 0 && newHeight == 0) {
            return photo;
        } else if (newHeight == 0) {
            if (photo.getWidth() > newWidth)
                newHeight = (newWidth * photo.getHeight()) / photo.getWidth();
            else return photo;
        } else if (newWidth == 0) {
            if (photo.getHeight() > newHeight)
                newWidth = (newHeight * photo.getWidth()) / photo.getHeight();
            else return photo;
        }

        Bitmap resizedPhoto = Bitmap.createScaledBitmap(photo, newWidth, newHeight, false);
        photo.recycle();

        return resizedPhoto;
    }

    /**
     * Create a new file for storing photo
     *
     * @param activity    : activity object caller
     * @param imageFolder : constant representing folder where to save
     * @return : empty file to be filled with content
     * @throws IOException
     */
    public static File createImageFile(Activity activity, int imageFolder) throws IOException {
        // Create an image file name
        String imageFileName = new SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(new Date());
        File storageDir = null;

        // Select chosen folder
        if (imageFolder == EXTERNAL_PICTURES) {
            storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        } else if (imageFolder == EXTERNAL_PUBLIC_PICTURES) {
            File picturesDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            storageDir = new File(picturesDir, "Share'nBook");
        } else if (imageFolder == EXTERNAL_CACHE) {
            storageDir = activity.getExternalCacheDir();
        }

        // Check if exists
        if (!storageDir.exists()) {
            if (!storageDir.mkdirs())
                throw new IOException();
        }

        // Create new file
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }

    /**
     * Compute uri given a file using a FileProvider
     *
     * @param activity : activity object caller
     * @param file     : file of which we need uri
     * @return : corresponding uri
     */
    public static Uri getUriForFile(Activity activity, File file) {
        return FileProvider.getUriForFile(activity,
                "it.polito.mad.sharenbook.fileprovider",
                file);
    }

    /**
     * Download an image into storage returning corresponding uri
     *
     * @param activity    : activity object caller
     * @param url         : url to be downloaded
     * @param imageFolder : constant representing folder where to save
     * @return : return uri of downloaded image
     */
    public static Uri downloadImageToStorage(Activity activity, String url, int imageFolder) throws IOException {
        InputStream srcStream = null;
        OutputStream dstStream = null;
        File imageFile;

        try {
            imageFile = ImageUtils.createImageFile(activity, imageFolder);
            srcStream = new URL(url).openStream();
            dstStream = new FileOutputStream(imageFile);

            Bitmap bmp = BitmapFactory.decodeStream(srcStream);
            bmp.compress(Bitmap.CompressFormat.JPEG, 95, dstStream);
            bmp.recycle();
        } finally {
            try {
                srcStream.close();
                dstStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return getUriForFile(activity, imageFile);
    }

}
