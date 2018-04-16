package it.polito.mad.sharenbook;

import android.content.Intent;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ShareBookActivity extends AppCompatActivity {

    private BottomNavigationView navBar;
    private CardView btnScan;
    private CardView btnManual;

    private IntentIntegrator qrScan;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_book);

        // Set strict mode
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        // Setup toolbar
        Toolbar sbaToolbar = findViewById(R.id.sba_toolbar);
        setSupportActionBar(sbaToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.sba_title);

        // Setup navbar
        setupNavbar();

        // View objects
        btnScan = findViewById(R.id.sba_scan_button);
        btnManual = findViewById(R.id.sba_manual_button);

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
        btnManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                Intent i = new Intent(getApplicationContext(), EditBookActivity.class);
                startActivity(i);
                */
                Toast.makeText(getApplicationContext(), "To be implemented", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        BookDetails bookInfo;

        if(result != null)
        {
            if(result.getContents() == null) return;
            else
            {
                bookInfo = new BookDetails(result.getContents());

                if (bookInfo.getTotalItems() > 0)
                {
                    /**
                     * FOR TEST PURPOSE: FIRE EditBookActivity
                     */
                    Intent i = new Intent(getApplicationContext(), EditBookActivity.class);
                    i.putExtra("book", bookInfo.getBookList().get(0));
                    startActivity(i);
                }
                else
                    Toast.makeText(getApplicationContext(), "Ivalid ISBN", Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            super.onActivityResult(requestCode, resultCode, data);
        }

    }

    @Override
    public boolean onSupportNavigateUp()
    {
        finish();
        return true;
    }

    private void setupNavbar()
    {
        navBar = findViewById(R.id.navigation);

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
                    i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                    finish();
                    break;

                case R.id.navigation_shareBook:
                    break;
            }
            return true;
        });
    }
}
