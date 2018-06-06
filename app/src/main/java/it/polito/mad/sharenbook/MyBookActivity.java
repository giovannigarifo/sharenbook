package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
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
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.adapters.AnnouncementAdapter;
import it.polito.mad.sharenbook.fragments.ExchangesFragment;
import it.polito.mad.sharenbook.fragments.RequestsFragment;
import it.polito.mad.sharenbook.fragments.ShowMyAnnouncementsFragment;
import it.polito.mad.sharenbook.utils.NavigationDrawerManager;
import it.polito.mad.sharenbook.utils.UserInterface;

public class MyBookActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener{

    private BottomNavigationView navBar;

    /** new announcement button */
    private AnnouncementAdapter adapter;
    private LinearLayoutManager llm;
    private RecyclerView rv;
    private NavigationView navigationView;
    private TabLayout tabLayout;

    /* TabView vars*/
    public MyBookActivity.SectionsPagerAdapter mSectionsPagerAdapter;
    public ViewPager mViewPager;

    private String username;

    private DrawerLayout drawer;
    private CircularImageView drawer_userPicture;
    private TextView drawer_fullname;
    private TextView drawer_email;


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

        SharedPreferences userPreferences = getSharedPreferences(getString(R.string.username_preferences), Context.MODE_PRIVATE);
        username = userPreferences.getString(getString(R.string.username_copy_key), "void");

        if(getIntent().getBooleanExtra("openedFromNotification", false)){
            mViewPager.setCurrentItem(getIntent().getIntExtra("showPageNum", 0));
        }
        if(getIntent().getBooleanExtra("openedFromDrawer",false)){
            mViewPager.setCurrentItem(getIntent().getIntExtra("showPageNumFromDrawer", 0));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        //navBar.setSelectedItemId(R.id.navigation_myBook);
        NavigationDrawerManager.setDrawerViews(getApplicationContext(), getWindowManager(), drawer_fullname,
                drawer_email, drawer_userPicture, NavigationDrawerManager.getNavigationDrawerProfile());
        UserInterface.setupNavigationBar(this, R.id.navigation_myBook);

        navigationView.setCheckedItem(R.id.drawer_navigation_none);

        if(mViewPager.getCurrentItem()==1)
            navigationView.setCheckedItem(R.id.drawer_navigation_myBookPendingRequest);
        else if(mViewPager.getCurrentItem()==2)
            navigationView.setCheckedItem(R.id.drawer_navigation_myBookExchanges);


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
        drawer = findViewById(R.id.my_book_drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.my_book_nav_view);
        navigationView.setNavigationItemSelectedListener(MyBookActivity.this);
        if(mViewPager.getCurrentItem()==1)
            navigationView.setCheckedItem(R.id.drawer_navigation_myBookPendingRequest);
        else if(mViewPager.getCurrentItem()==2)
            navigationView.setCheckedItem(R.id.drawer_navigation_myBookExchanges);
        else
            navigationView.setCheckedItem(R.id.drawer_navigation_none);

        // Update drawer with user info
        View nav = getLayoutInflater().inflate(R.layout.nav_header_main, navigationView);
        drawer_userPicture = nav.findViewById(R.id.drawer_userPicture);
        drawer_fullname = nav.findViewById(R.id.drawer_user_fullname);
        drawer_email = nav.findViewById(R.id.drawer_user_email);



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


    /**
     * Navigation Drawer Listeners
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int fragmentSelected = mViewPager.getCurrentItem();

        int id;

        if(fragmentSelected == 1)
            return NavigationDrawerManager.onNavigationItemSelected(this,mViewPager,item,getApplicationContext(),drawer,R.id.drawer_navigation_myBookPendingRequest);
        else if(fragmentSelected == 2)
            return NavigationDrawerManager.onNavigationItemSelected(this,mViewPager,item,getApplicationContext(),drawer,R.id.drawer_navigation_myBookExchanges);
        else
            return NavigationDrawerManager.onNavigationItemSelected(this,mViewPager,item,getApplicationContext(),drawer,0);

    }
    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.my_book_drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }

        navigationView.setCheckedItem(R.id.drawer_navigation_none);

        if(mViewPager.getCurrentItem()==1)
            navigationView.setCheckedItem(R.id.drawer_navigation_myBookPendingRequest);
        else if(mViewPager.getCurrentItem()==2)
            navigationView.setCheckedItem(R.id.drawer_navigation_myBookExchanges);


    }


    public class SectionsPagerAdapter extends FragmentStatePagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        Fragment fragment = null;

        @Override
        public Fragment getItem(int position) {

            Bundle bundle = new Bundle();
            bundle.putString(getString(R.string.username_key), username);

            switch (position) {
                case 0:
                    fragment = new ShowMyAnnouncementsFragment();
                    break;
                case 1:
                    fragment = new RequestsFragment();
                    fragment.setArguments(bundle);
                    break;
                case 2:
                    fragment = new ExchangesFragment();
                    break;
            }
            return fragment;
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            if (getCurrentFragment() != object) {
                fragment = ((Fragment) object);
            }
            super.setPrimaryItem(container, position, object);
        }

        public Fragment getCurrentFragment() {
            return fragment;
        }
    }
}
