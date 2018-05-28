package it.polito.mad.sharenbook;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


import com.firebase.ui.auth.AuthUI;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.ArrayList;

import it.polito.mad.sharenbook.fragments.ProfileReviewsFragment;
import it.polito.mad.sharenbook.fragments.ShowMyAnnouncements;
import it.polito.mad.sharenbook.model.Book;
import it.polito.mad.sharenbook.utils.MyBooksUtils;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.UserInterface;

public class MyBookActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private BottomNavigationView navBar;

    /** new announcement button */
    private FloatingActionButton newAnnoucementFab;
    private AnnouncementAdapter adapter;
    private LinearLayoutManager llm;
    private RecyclerView rv;
    private NavigationView navigationView;
    private TabLayout tabLayout;

    /* TabView vars*/
    private MyBookActivity.SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_book);

        mViewPager = findViewById(R.id.container);
        tabLayout = findViewById(R.id.tabs);

        //Adapter for viewPager
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));
        tabLayout.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(mViewPager));
        tabLayout.setTabTextColors(getResources().getColor(R.color.secondaryText), getResources().getColor(R.color.white));

        setupNavigationTools();
        findAndSetNewAnnouncementFab();


        // Setup toolbar
        /*
        Toolbar sbaToolbar = findViewById(R.id.sba_toolbar);
        setSupportActionBar(sbaToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.mba_title);




        // Setup navbar
        setupNavbar();
        */
    }

    @Override
    protected void onResume() {
        super.onResume();
        navBar.setSelectedItemId(R.id.navigation_myBook);
    }

    private void setupNavigationTools() {
        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.sba_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.mba_title);
        }

        // Setup navigation drawer
        DrawerLayout drawer = findViewById(R.id.my_book_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.my_book_nav_view);
        navigationView.setNavigationItemSelectedListener(MyBookActivity.this);
        navigationView.setCheckedItem(R.id.drawer_navigation_myBook);

        // Update drawer with user info
        View nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        CircularImageView drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        TextView drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        TextView drawer_email = nav.findViewById(R.id.drawer_user_email);

        NavigationDrawerManager.setDrawerViews(getApplicationContext(), getWindowManager(), drawer_fullname,
                drawer_email, drawer_userPicture, NavigationDrawerManager.getNavigationDrawerProfile());

        // Setup bottom navbar
        UserInterface.setupNavigationBar(this, R.id.navigation_myBook);
        navBar = findViewById(R.id.navigation);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);


    }
/*
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("books",books);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d("debug","I am in onRestoreInstanceState");
        books = savedInstanceState.getParcelableArrayList("books");
        setRecyclerView(books);
    }*/

    private void findAndSetNewAnnouncementFab(){
        newAnnoucementFab = findViewById(R.id.fab_addBook);

        newAnnoucementFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),ShareBookActivity.class);
                startActivity(i);
            }
        });
    }




    /**
     * Navigation Drawer Listeners
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if(id == R.id.drawer_navigation_profile){
            Intent i = new Intent(getApplicationContext(), TabbedShowProfileActivity.class);
            i.putExtra(getString(R.string.user_profile_data_key), NavigationDrawerManager.getUserParcelable(getApplicationContext()));
            startActivity(i);

        } else if (id == R.id.drawer_navigation_shareBook) {
            Intent i = new Intent(getApplicationContext(), ShareBookActivity.class);
            startActivity(i);

        } else if (id == R.id.drawer_navigation_logout) {
            AuthUI.getInstance()
                    .signOut(this)
                    .addOnCompleteListener(task -> {
                        Intent i = new Intent(getApplicationContext(), SplashScreenActivity.class);
                        startActivity(i);
                        Toast.makeText(getApplicationContext(), getString(R.string.log_out), Toast.LENGTH_SHORT).show();
                        finish();
                    });
        }

        DrawerLayout drawer = findViewById(R.id.my_book_drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.my_book_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
        navigationView.setCheckedItem(R.id.drawer_navigation_myBook);
    }


    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        private ShowMyAnnouncements announcementsFragment;
        private ProfileReviewsFragment revFragment;

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {

            switch (position) {
                case 0:
                    announcementsFragment = new ShowMyAnnouncements();
                    return announcementsFragment;
                case 1:
                    //bundle = new Bundle();
                    //bundle.putParcelable("userData", user);
                    revFragment = new ProfileReviewsFragment();
                    //revFragment.setArguments(bundle);
                    return revFragment;
                case 2:
                    revFragment = new ProfileReviewsFragment();
                    //revFragment.setArguments(bundle);
                    return revFragment;
                default:
                    return null;
            }

        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return 2;
        }
    }
}
