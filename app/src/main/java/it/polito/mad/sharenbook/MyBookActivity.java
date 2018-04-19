package it.polito.mad.sharenbook;

import android.content.Intent;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
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

import java.util.ArrayList;

import it.polito.mad.sharenbook.model.UserProfile;

public class MyBookActivity extends AppCompatActivity {

    /**
     * JUST A TRY MUST BE REMOVED
     */
    class DataItem {
        private String name, address;
        private ArrayList<String> children;

        public DataItem(String name, String address, int n) {
            this.name = name;
            this.address = address;
            children = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                children.add("child " + i);
            }
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        int getChildrenCount() {
            return children.size();
        }

        String getChild(int pos) {
            return children.get(pos);
        }

    }

    private BottomNavigationView navBar;

    /**
     * My books expandable
     */
    private ExpandableListView elv;
    private ArrayList<DataItem> data = new ArrayList<DataItem>();

    /** FireBase objects */
    private FirebaseUser firebaseUser;
    private DatabaseReference booksDb;
    private DatabaseReference userBooksDb;
    private StorageReference bookImagesStorage;
    private ArrayList<String> bookKeys;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_book);

        // Setup FireBase
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        userBooksDb = firebaseDatabase.getReference(getString(R.string.users_key)).child(firebaseUser.getUid()).child(getString(R.string.user_books_key));

        userBooksDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue().equals(getString(R.string.users_books_placeholder))){ /** no announcemnts */
                    AlertDialog.Builder no_books = new AlertDialog.Builder(MyBookActivity.this); //give a context to Dialog
                    no_books.setTitle(R.string.no_books_alert_title);
                    no_books.setMessage(R.string.no_books_suggestion);
                    no_books.setPositiveButton(android.R.string.ok, (dialog, which) -> {

                                Intent i = new Intent (getApplicationContext(), ShareBookActivity.class);
                                startActivity(i);
                                finish();

                            }
                    ).setNegativeButton(android.R.string.cancel,
                            (dialog, which) -> {
                                dialog.dismiss();
                                finish();
                            }
                    );

                    no_books.show();
                }else{ /** there are announcements */


                    bookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));
                    booksDb = firebaseDatabase.getReference(getString(R.string.books_key));

                for(DataSnapshot announce :dataSnapshot.getChildren()){ /** for each announce that book detail */
                    //bookKeys.add((String)announce.getValue());
                    booksDb.child((String)announce.getValue()).addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            Book book = dataSnapshot.getValue(Book.class);
                            Log.d("Book:",book.getIsbn());
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                    }
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        // Setup toolbar
        Toolbar sbaToolbar = findViewById(R.id.sba_toolbar);
        setSupportActionBar(sbaToolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.mba_title);

        //setMyBooksList();

        // Setup navbar
        setupNavbar();
    }

    private void setMyBooksList(){
        elv = (ExpandableListView) findViewById(R.id.expanded_books);

        for (int i = 0; i < 100; i++) {
            DataItem di = new DataItem("name" + i, "address" + i, (int) (Math.sqrt(i)));
            data.add(di);
        }

        BaseExpandableListAdapter bela = new BaseExpandableListAdapter() {
            @Override
            public int getGroupCount() {
                return data.size();
            }

            @Override
            public int getChildrenCount(int groupPosition) {
                return data.get(groupPosition).getChildrenCount();
            }

            @Override
            public Object getGroup(int groupPosition) {
                return data.get(groupPosition);
            }

            @Override
            public Object getChild(int groupPosition, int childPosition) {
                return data.get(groupPosition).getChild(childPosition);
            }

            @Override
            public long getGroupId(int groupPosition) {
                return 0;
            }

            @Override
            public long getChildId(int groupPosition, int childPosition) {
                return 0;
            }

            @Override
            public boolean hasStableIds() {
                return false;
            }

            @Override
            public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
                if (convertView==null) {
                    convertView=getLayoutInflater().inflate(R.layout.mybook_item,parent,false);
                }
                TextView name=(TextView)convertView.findViewById(R.id.name_tv);
                TextView address=(TextView)convertView.findViewById(R.id.address_tv);
                DataItem di=data.get(groupPosition);
                name.setText(di.getName());
                address.setText(di.getAddress());
                return convertView;
            }

            @Override
            public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
                if (convertView==null) convertView=new TextView(MyBookActivity.this);
                TextView tv=(TextView)convertView;
                tv.setText(data.get(groupPosition).getChild(childPosition));
                return tv;
            }

            @Override
            public boolean isChildSelectable(int groupPosition, int childPosition) {
                return false;
            }
        };
        elv.setAdapter(bela);
    }

    private void setupNavbar() {
        navBar = findViewById(R.id.navigation);


        // Set navigation_shareBook as selected item
        navBar.setSelectedItemId(R.id.navigation_myBook);

        // Set the listeners for the navigation bar items
        navBar.setOnNavigationItemSelectedListener(item -> {

            switch (item.getItemId()) {
                case R.id.navigation_logout:
                    AuthUI.getInstance()
                            .signOut(this)
                            .addOnCompleteListener(task -> {
                                Intent i = new Intent(getApplicationContext(), SplashScreenActivity.class);
                                startActivity(i);
                                Toast.makeText(getApplicationContext(), getString(R.string.log_out), Toast.LENGTH_SHORT).show();
                                finish();
                            });
                    break;

                case R.id.navigation_profile:
                    Intent i = new Intent(getApplicationContext(), ShowProfileActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(i);
                    finish();
                    break;

                case R.id.navigation_shareBook:
                    Intent shareBook = new Intent(getApplicationContext(), ShareBookActivity.class);
                    startActivity(shareBook);
                    finish();
                    break;

                case R.id.navigation_myBook:

                    break;
            }

            return true;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        // Terminate activity (actionbar left arrow pressed)
        finish();
        return true;
    }
}
