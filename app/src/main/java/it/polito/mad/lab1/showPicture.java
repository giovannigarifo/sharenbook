package it.polito.mad.lab1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.ImageView;

public class showPicture extends Activity {

    private ImageView fullSizePicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_picture);

        fullSizePicture = (ImageView) findViewById(R.id.fullSize_Picture);
        Intent i = getIntent();

        String picturePath = i.getStringExtra("PicturePath");

        Uri pictureUri = Uri.parse(picturePath);
/*
        Bitmap resultBMP;



        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(picturePath, options);
        final int REQUIRED_SIZE = 200;
        int scale = 1;
        while (options.outWidth / scale / 2 >= REQUIRED_SIZE
                && options.outHeight / scale / 2 >= REQUIRED_SIZE)
            scale *= 2;
        //options.inSampleSize = scale;
        options.inJustDecodeBounds = false;
        DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
        options.inScreenDensity = metrics.densityDpi;
        options.inTargetDensity =  metrics.densityDpi;
        options.inDensity = DisplayMetrics.DENSITY_DEFAULT;

        resultBMP = BitmapFactory.decodeFile(picturePath, options);

        fullSizePicture.setImageBitmap(resultBMP);
        */
        fullSizePicture.setImageURI(pictureUri);

        Log.d("Path:",picturePath.toString());


    }

    @Override
    public void onBackPressed(){

        finish();


    }


}
