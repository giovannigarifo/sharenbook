package it.polito.mad.sharenbook;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.UserInterface;

public class ShowPictureActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE); //disable title bar
        setContentView(R.layout.activity_show_picture);


        ImageView fullSizePicture = findViewById(R.id.fullSize_Picture);

        /* Get extras */
        Intent i = getIntent();
        String pic_signature = i.getStringExtra("PictureSignature");
        String pathPortion = i.getStringExtra("pathPortion");
        int mode = i.getIntExtra("mode", 1);

        if(mode == 1) {
            /* Show User Image */
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("images/" + pathPortion + ".jpg");
            UserInterface.showGlideImage(getApplicationContext(), storageRef, fullSizePicture, Long.valueOf(pic_signature));
        } else {
            /* Show book image */
            Log.d("pic path", "path: " + "book_images/" + pathPortion + ".jpg");
            StorageReference storageRef = FirebaseStorage.getInstance().getReference().child("book_images/" + pathPortion);
            GlideApp.with(getApplicationContext())
                    .load(storageRef)
                    .into(fullSizePicture);
        }

    }

    @Override
    public void onBackPressed() {
        finish();
    }

}
