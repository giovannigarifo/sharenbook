package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Toolbar;

import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.core.GeoHash;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import it.polito.mad.sharenbook.model.UserProfile;

public class WriteReviewActivity extends AppCompatActivity {

    //context
    Context context;

    //views
    TextView writereview_givenloaned;
    ImageView writereview_bookPhoto;
    TextView writereview_bookTitle;
    TextView writereview_archivingDate;

    EditText writereview_et_reviewTitle;
    EditText writereview_et_reviewBody;
    SeekBar writereview_sb_reviewVote;

    FloatingActionButton writereview_fab_save;

    //attributes
    private String userNickName;
    private boolean isGiven;

    //user profile
    private UserProfile user;

    //firebase
    private FirebaseUser firebaseUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);
        context = App.getContext();

        //get views
        getViews();
        setupViews();

        if(savedInstanceState == null)
            startedFromIntent();
        else startedFromSavedState();
    }

    /**
     * Activity has been started by an intent
     */
    private void startedFromIntent() {

        //retrieve data from intent
        Bundle bundle = getIntent().getExtras();

        if (bundle != null) {

            //is given or loaned? update textview
            this.userNickName = bundle.getString("userNickName");
            this.isGiven = bundle.getBoolean("isGiven");

            if(isGiven == true)
                this.writereview_givenloaned.setText(getResources().getString(R.string.writereview_review_given) + " " + this.userNickName);
            else this.writereview_givenloaned.setText(this.userNickName + " " + getResources().getString(R.string.writereview_review_loaned));
        }
    }

    /**
     * Activity is restoring from a preivous instance state, e.g. rotation happened
     */
    private void startedFromSavedState() {

    }


    /**
     * get views from layout
     */
    private void getViews(){


        this.writereview_givenloaned = findViewById(R.id.writereview_givenloaned);
        this.writereview_bookPhoto = findViewById(R.id.writereview_bookPhoto);
        this.writereview_bookTitle = findViewById(R.id.writereview_bookTitle);
        this.writereview_archivingDate = findViewById(R.id.writereview_archivingDate);
        this.writereview_et_reviewTitle = findViewById(R.id.writereview_et_reviewTitle);
        this.writereview_et_reviewBody = findViewById(R.id.writereview_et_reviewBody);
        this.writereview_sb_reviewVote = findViewById(R.id.writereview_sb_reviewVote);
        this.writereview_fab_save = findViewById(R.id.writereview_fab_save);
    }


    /**
     * Setup common parameters of views
     */
    private void setupViews(){

        //fab
        this.writereview_fab_save.setOnClickListener( v -> saveReviewToFirebase() );

    }


    /**
     * get all reviews data from the user input and create the hashmap ready to be pushed to firebase
     */
    private HashMap<String,Object> retrieveDataAndCreateReview() {

        HashMap<String, Object> reviewData = new HashMap<>();

        //title
        String title = this.writereview_et_reviewTitle.getText().toString().trim().replace("\"\'\\", "");
        reviewData.put("reviewTitle", title);

        //body
        String body = this.writereview_et_reviewBody.getText().toString().trim().replace("\"\'\\", "");
        reviewData.put("reviewBody", body);

        //score
        Integer score = this.writereview_sb_reviewVote.getProgress();
        reviewData.put("reviewScore", score);

        //creation time
        reviewData.put("creationTime", ServerValue.TIMESTAMP);

        //the userId of who writes the review
        SharedPreferences userData = context.getSharedPreferences(context.getString(R.string.userData_preferences), Context.MODE_PRIVATE);
        String username = userData.getString(context.getString(R.string.username_pref), "void");

        if(!username.equals("void"))
            reviewData.put("username", username);
        else Toast.makeText(context, "error in username", Toast.LENGTH_LONG).show();
        //TODO: to be changed

        return reviewData;
    }

    /**
     * save the review into firebase
     */
    void saveReviewToFirebase(){

        //retrieve data from view and create review object
        HashMap<String, Object> reviewData = retrieveDataAndCreateReview();

        //reference to usernames of firebase
        DatabaseReference usernamesRef = FirebaseDatabase.getInstance().getReference("usernames");

        //get new review key
        String reviewKey = usernamesRef.child(userNickName)
                .child(getString(R.string.reviews_key))
                .push().getKey();

        //create transaction map
        Map<String, Object> transaction = new HashMap<>();

        transaction.put(reviewKey, reviewData);

        // Push the review to firebase
        usernamesRef.updateChildren(transaction, (databaseError, databaseReference) -> {
            if (databaseError != null) {
                Toast.makeText(getApplicationContext(), "An error occurred, try later.", Toast.LENGTH_LONG).show();
            }
        });
    }

}
