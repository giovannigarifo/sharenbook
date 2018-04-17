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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ShareBookActivity extends AppCompatActivity {

    private EditText editIsbn;
    private Button btnSearchIsbn;
    private CardView btnScan;
    private CardView btnManual;
    private BottomNavigationView navBar;

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
        editIsbn = findViewById(R.id.sba_edit_text_isbn);
        btnSearchIsbn = findViewById(R.id.sba_search_isbn_button);
        btnScan = findViewById(R.id.sba_scan_button);
        btnManual = findViewById(R.id.sba_manual_button);

        // Initializing scan object
        qrScan = new IntentIntegrator(this);
        qrScan.setOrientationLocked(false);
        qrScan.setPrompt("Scan an ISBN Barcode");

        // Attach listeners
        btnSearchIsbn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String insertedText = editIsbn.getText().toString();

                if (insertedText.length() != 10 && insertedText.length() != 13) {
                    editIsbn.setError(getText(R.string.sba_isbn_not_valid));
                    return;
                }

                BookDetails bookInfo = new BookDetails(insertedText);
                if (bookInfo.getTotalItems() == 0) {
                    Toast.makeText(getApplicationContext(), "No books found", Toast.LENGTH_SHORT).show();
                }
                else {
                    Intent i = new Intent(getApplicationContext(), EditBookActivity.class);
                    i.putExtra("book", bookInfo.getBookList().get(0));
                    startActivity(i);
                }
            }
        });
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                qrScan.initiateScan();
            }
        });
        btnManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(), EditBookActivity.class);
                i.putExtra("book", new Book());
                startActivity(i);
            }
        });

        // Restore previous state
        if (savedInstanceState != null) {
            editIsbn.setText(savedInstanceState.getString("isbn"));
        }
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
                    // Fire EditBookActivity
                    Intent i = new Intent(getApplicationContext(), EditBookActivity.class);
                    i.putExtra("book", bookInfo.getBookList().get(0));
                    startActivity(i);
                }
                else
                    Toast.makeText(getApplicationContext(), "Invalid ISBN", Toast.LENGTH_SHORT).show();
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

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current ISBN value
        savedInstanceState.putString("isbn", editIsbn.getText().toString());
        super.onSaveInstanceState(savedInstanceState);
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
