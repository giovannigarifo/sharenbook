package it.polito.mad.sharenbook;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.onesignal.OneSignal;

import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.BookDetails;
import it.polito.mad.sharenbook.utils.InputValidator;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.PermissionsHandler;
import it.polito.mad.sharenbook.utils.UserInterface;

public class ShareBookActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private Activity mActivity;

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

    //drawer
    private NavigationView navigationView;
    private DrawerLayout drawer;
    private CircularImageView drawer_userPicture;
    private TextView drawer_fullname;
    private TextView drawer_email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_book);

        // Get activity instance
        this.mActivity = this;

        // Setup navigation tools
        setupNavigationTools();

        // Set strict mode
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

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
        qrScan.setPrompt(getText(R.string.sba_scan_your_barcode).toString());

        // Attach listeners
        btnSearchIsbn.setOnClickListener(v -> {
            // Validate isbn value
            if (InputValidator.isWrongIsbn(editIsbn)) {
                editIsbn.setError(getText(R.string.isbn_bad_format));
                return;
            }

            // Retrieve book details on a separate thread
            String insertedIsbn = editIsbn.getText().toString();
            new GetBookDetailsTask().execute(insertedIsbn);
        });

        btnScan.setOnClickListener(v -> PermissionsHandler.check(mActivity, () -> qrScan.initiateScan()));

        btnManual.setOnClickListener(v -> {
            Intent i = new Intent(getApplicationContext(), EditBookActivity.class);
            i.putExtra("book", new Book());
            startActivity(i);
            finish();
        });

        // Restore previous state
        if (savedInstanceState != null) {
            editIsbn.setText(savedInstanceState.getString("isbn"));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (result != null) {
            if (result.getContents() != null) {
                // Retrieve book details on a separate thread
                new GetBookDetailsTask().execute(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onBackPressed() {

        DrawerLayout drawer = findViewById(R.id.share_book_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        return NavigationDrawerManager.onNavigationItemSelected(this,null,item,getApplicationContext(),drawer,R.id.drawer_navigation_shareBook);


    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        // Save the user's current ISBN value
        savedInstanceState.putString("isbn", editIsbn.getText().toString());
        super.onSaveInstanceState(savedInstanceState);
    }

    private void setupNavigationTools() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.share_book_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.sba_title);
        }

        // Setup navigation drawer
        drawer = findViewById(R.id.share_book_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.share_book_nav_view);
        navigationView.setCheckedItem(R.id.drawer_navigation_shareBook);
        navigationView.setNavigationItemSelectedListener(this);

        // Update drawer with user info
        View nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        drawer_email = nav.findViewById(R.id.drawer_user_email);



        // Setup bottom navbar
        UserInterface.setupNavigationBar(this, R.id.navigation_myBook, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        NavigationDrawerManager.setDrawerViews(getApplicationContext(), getWindowManager(), drawer_fullname,
                drawer_email, drawer_userPicture, NavigationDrawerManager.getNavigationDrawerProfile());
        navigationView.setCheckedItem(R.id.drawer_navigation_shareBook);
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
            return new BookDetails(mActivity, readIsbn);
        }

        // This runs in UI when background thread finishes
        @Override
        protected void onPostExecute(BookDetails result) {

            super.onPostExecute(result);

            // Do things like hide the progress bar or change a TextView
            progressDialog.dismiss();

            if (result.getTotalItems() == 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.sba_no_books_found), Toast.LENGTH_LONG).show();
            } else if (result.getTotalItems() == -1) {
                Toast.makeText(getApplicationContext(), getString(R.string.sba_no_connection), Toast.LENGTH_LONG).show();
            } else {
                Intent i = new Intent(getApplicationContext(), EditBookActivity.class);
                i.putExtra("book", result.getBookList().get(0));
                startActivity(i);
                finish();
            }
        }
    }
}
