package it.polito.mad.sharenbook;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.BottomNavigationView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.IOException;
import java.net.URL;

public class ShareBookActivity extends Activity {

    private BottomNavigationView navBar;
    private ImageView bookCover;
    private TextView tvTitle;
    private TextView tvDescription;
    private Button btnScan;

    private IntentIntegrator qrScan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_book);

        // Set strict mode
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // View objects
        navBar = findViewById(R.id.navigation);
        bookCover = findViewById(R.id.image_book_cover);
        tvTitle = findViewById(R.id.text_sba_title);
        tvDescription = findViewById(R.id.text_sba_description);
        btnScan = findViewById(R.id.button_scan_isbn);

        // Initializing scan object
        qrScan = new IntentIntegrator(this);
        qrScan.setOrientationLocked(false);
        qrScan.setPrompt("Scan an ISBN Barcode");

        // Attach listeners
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrScan.initiateScan();
            }
        });

        /**
         * navBar
         */

        //set navigation_profile as selected item
        navBar.setSelectedItemId(R.id.navigation_shareBook);

        //set the listener for the navigation bar items
        navBar.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_showcase:
                    Toast.makeText(getApplicationContext(), "Selected Showcase!", Toast.LENGTH_SHORT).show();
                    break;

                case R.id.navigation_profile:
                    Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                    startActivity(i);
                    finish();
                    break;

                case R.id.navigation_shareBook:
                    break;
            }
            return true;
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if(result != null)
        {
            if(result.getContents() == null) tvTitle.setText("There was an error! Try Again.");
            else
            {
                BookDetails bookInfo = new BookDetails(result.getContents());

                if (bookInfo.getTotalItems() > 0)
                {
                    String bookTitle = bookInfo.getBookList().get(0).getTitle();
                    String description = bookInfo.getBookList().get(0).getDescription();
                    Uri thumbnail = bookInfo.getBookList().get(0).getThumbnail();

                    tvTitle.setText(bookTitle);
                    if (description != null)
                        tvDescription.setText(description);

                    if (thumbnail != null)
                    {
                        try {
                            bookCover.setImageBitmap(BitmapFactory.decodeStream(new URL(thumbnail.toString()).openConnection().getInputStream()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                else
                    tvTitle.setText("ISBN not valid");
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
