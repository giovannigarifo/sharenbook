package it.polito.mad.sharenbook;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
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

import it.polito.mad.sharenbook.Utils.InputValidate;

public class ShareBookActivity extends AppCompatActivity {

    // View objects
    private EditText editIsbn;
    private Button btnSearchIsbn;
    private CardView btnScan;
    private CardView btnManual;
    private BottomNavigationView navBar;

    // Barcode scanner object
    private IntentIntegrator qrScan;

    // ProgressDialog
    ProgressDialog progressDialog;

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

        // Setup progress dialog
        progressDialog = new ProgressDialog(ShareBookActivity.this, ProgressDialog.STYLE_SPINNER);

        // Get view objects
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
                // Validate isbn value
                if (InputValidate.wrongIsbn(editIsbn)) {
                    editIsbn.setError(getText(R.string.isbn_bad_format));
                    return;
                }

                // Retrieve book details on a separate thread
                String insertedIsbn = editIsbn.getText().toString();
                new GetBookDetailsTask().execute(insertedIsbn);
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

        if(result != null)
        {
            if (result.getContents() != null) {
                // Retrieve book details on a separate thread
                new GetBookDetailsTask().execute(result.getContents());
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
        // Terminate activity (actionbar left arrow pressed)
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

        // Set navigation_shareBook as selected item
        navBar.setSelectedItemId(R.id.navigation_shareBook);

        // Set the listeners for the navigation bar items
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

    private class GetBookDetailsTask extends AsyncTask<String, Integer, BookDetails> {

        // Runs in UI before background thread is called
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // Do something like display a progress bar
            progressDialog.setMessage(getText(R.string.sba_getting_book_details));
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        // This is run in a background thread
        @Override
        protected BookDetails doInBackground(String... params) {
            // Get the isbn string from params, which is an array
            String readIsbn = params[0];
            return new BookDetails(readIsbn);
        }

        // This runs in UI when background thread finishes
        @Override
        protected void onPostExecute(BookDetails result) {
            super.onPostExecute(result);

            // Do things like hide the progress bar or change a TextView
            progressDialog.dismiss();

            if (result.getTotalItems() == 0) {
                Toast.makeText(getApplicationContext(), R.string.sba_no_books_found, Toast.LENGTH_LONG).show();
            }
            else if (result.getTotalItems() == -1) {
                Toast.makeText(getApplicationContext(), R.string.sba_no_connection, Toast.LENGTH_LONG).show();
            }
            else {
                Intent i = new Intent(getApplicationContext(), EditBookActivity.class);
                i.putExtra("book", result.getBookList().get(0));
                startActivity(i);
            }
        }
    }
}
