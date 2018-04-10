package it.polito.mad.lab1;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.IOException;
import java.net.URL;

public class ShareBookActivity extends Activity {

    private ImageView bookCover;
    private TextView tvIsbn;
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
        bookCover = findViewById(R.id.image_book_cover);
        tvIsbn = findViewById(R.id.text_isbn);
        btnScan = findViewById(R.id.button_scan_isbn);

        // Initializing scan object
        qrScan = new IntentIntegrator(this);
        qrScan.setDesiredBarcodeFormats(IntentIntegrator.EAN_13);
        qrScan.setOrientationLocked(false);

        // Attach listeners
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrScan.initiateScan();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if(result != null)
        {
            if(result.getContents() == null) tvIsbn.setText("There was an error! Try Again.");
            else
            {
                BookDetails bookInfo = new BookDetails(result.getContents());

                if (bookInfo.getTotalItems() > 0)
                {
                    String bookTitle = bookInfo.getBookList().get(0).getTitle();
                    Uri thumbnail = bookInfo.getBookList().get(0).getThumbnail();
                    tvIsbn.setText(bookTitle);
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
                    tvIsbn.setText("ISBN not valid");
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
