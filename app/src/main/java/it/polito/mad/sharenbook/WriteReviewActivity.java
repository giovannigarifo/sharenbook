package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.PersistableBundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import it.polito.mad.sharenbook.model.UserProfile;
import it.polito.mad.sharenbook.utils.GlideApp;

public class WriteReviewActivity extends AppCompatActivity {

    //context
    Context context;

    //views
    TextView writereview_givenloaned;
    ImageView writereview_bookPhoto;
    TextView writereview_bookTitle;
    TextView writereview_creationTime;

    TextView writereview_tv_reviewHeadingMessage;
    EditText writereview_et_reviewTitle;
    EditText writereview_et_reviewBody;
    RatingBar writereview_rb_reviewVote;

    FloatingActionButton writereview_fab_save;

    //attributes
    private String bookId;
    private String exchangeId;
    private String bookPhoto;
    private String bookTitle;
    private Long creationTime;
    private String userNickName;
    private boolean isGiven;


    //user profile
    private UserProfile user;

    //firebase
    private FirebaseUser firebaseUser;
    private StorageReference mBookImagesStorage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);
        context = App.getContext();

        //retrieve firebase storage
        mBookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key));

        if (savedInstanceState == null)
            startedFromIntent();
        else startedFromSavedState(savedInstanceState);

        //get views
        getViews();
        setupViews();
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

            //book title
            this.bookTitle = bundle.getString("bookTitle");

            //book creation time
            this.creationTime = bundle.getLong("creationTime");

            //book id
            this.bookId = bundle.getString("bookId");

            //book photo
            this.bookPhoto = bundle.getString("bookPhoto");

            //exchangeid
            this.exchangeId = bundle.getString("exchangeId");
        }
    }

    /**
     * Activity is restoring from a preivous instance state, e.g. rotation happened
     */
    private void startedFromSavedState(Bundle savedState) {

        this.userNickName = savedState.getString("userNickName");
        this.isGiven = savedState.getBoolean("isGiven");
        this.bookTitle = savedState.getString("bookTitle");
        this.creationTime = savedState.getLong("creationTime");
        this.bookId = savedState.getString("bookId");
        this.bookPhoto = savedState.getString("bookPhoto");
        this.exchangeId = savedState.getString("exchangeId");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putString("userNickName", this.userNickName);
        outState.putBoolean("isGiven", this.isGiven);
        outState.putString("bookTitle", this.bookTitle);
        outState.putLong("creationTime", this.creationTime);
        outState.putString("bookId", this.bookId);
        outState.putString("bookPhoto", this.bookPhoto);
        outState.putString("exchangeId", this.exchangeId);
    }

    /**
     * get views from layout
     */
    private void getViews() {

        this.writereview_givenloaned = findViewById(R.id.writereview_givenloaned);
        this.writereview_bookPhoto = findViewById(R.id.writereview_bookPhoto);
        this.writereview_bookTitle = findViewById(R.id.writereview_bookTitle);
        this.writereview_creationTime = findViewById(R.id.writereview_creationTime);
        this.writereview_et_reviewTitle = findViewById(R.id.writereview_et_reviewTitle);
        this.writereview_et_reviewBody = findViewById(R.id.writereview_et_reviewBody);
        this.writereview_rb_reviewVote = findViewById(R.id.writereview_rb_reviewVote);
        this.writereview_fab_save = findViewById(R.id.writereview_fab_save);
        this.writereview_tv_reviewHeadingMessage = findViewById(R.id.writereview_tv_reviewHeadingMessage);
    }


    /**
     * Setup common parameters of views
     */
    private void setupViews() {

        //book title
        this.writereview_bookTitle.setText(this.bookTitle);

        //given or loaned: heading
        if (isGiven == true)
            this.writereview_givenloaned.setText(getResources().getString(R.string.writereview_review_given) + " " + this.userNickName + ":");
        else
            this.writereview_givenloaned.setText(this.userNickName + " " + getResources().getString(R.string.writereview_review_loaned) + ":");

        //book creation time
        if (this.creationTime != null)
            this.writereview_creationTime.setText(DateUtils.formatDateTime(context, this.creationTime,
                    DateUtils.FORMAT_SHOW_DATE
                            | DateUtils.FORMAT_NUMERIC_DATE
                            | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR));

        //book photo
        StorageReference photoRef = mBookImagesStorage.child(this.bookId).child(this.bookPhoto);

        // Load book photo
        GlideApp.with(App.getContext())
                .load(photoRef)
                .placeholder(R.drawable.book_cover_portrait)
                .into(this.writereview_bookPhoto);

        // review heading message
        this.writereview_tv_reviewHeadingMessage.setText(getResources().getString(R.string.writereview_review_headingMessage) + " " + userNickName);

        //rating bar
        this.writereview_rb_reviewVote.setOnRatingBarChangeListener(this::onRatingChanged);

        //fab
        this.writereview_fab_save.setOnClickListener(v -> saveReviewToFirebase());

    }


    /**
     * get all reviews data from the user input and create the hashmap ready to be pushed to firebase
     */
    private HashMap<String, Object> retrieveDataAndCreateReview() {

        HashMap<String, Object> reviewData = new HashMap<>();

        //book id associated with the review
        reviewData.put("bookId", this.bookId);

        //title
        String title = this.writereview_et_reviewTitle.getText().toString().trim().replace("\"\'\\", "");
        reviewData.put("rTitle", title);

        //body
        String body = this.writereview_et_reviewBody.getText().toString().trim().replace("\"\'\\", "");
        reviewData.put("rText", body);

        //score
        Integer score = this.writereview_rb_reviewVote.getProgress();
        reviewData.put("rating", score);

        //creation time of the review
        reviewData.put("date", ServerValue.TIMESTAMP);

        //isGiven
        reviewData.put("given", this.isGiven);

        //the userId of who writes the review
        SharedPreferences userData = context.getSharedPreferences(context.getString(R.string.userData_preferences), Context.MODE_PRIVATE);
        String username = userData.getString(context.getString(R.string.username_pref), "void");

        if (!username.equals("void"))
            reviewData.put("creator", username);
        else Toast.makeText(context, "error in username", Toast.LENGTH_LONG).show();
        //TODO: to be changed

        return reviewData;
    }

    /**
     * save the review into firebase
     */
    void saveReviewToFirebase() {

        //retrieve data from view and create review object
        HashMap<String, Object> reviewData = retrieveDataAndCreateReview();

        //reference to the reviews of the users in firebase
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();

        //get new review key
        String reviewKey = rootRef
                .child("usernames/" + userNickName + "/" + getString(R.string.reviews_key))
                .push().getKey();

        //archive path of the user who released the review
        String archivePath = "shared_books/" + user.getUsername() + "/archive_books/" + this.exchangeId + "/reviewed";

        //review path
        String reviewPath = "usernames/" + userNickName + "/reviews/" + reviewKey;

        //create transaction map
        Map<String, Object> transaction = new HashMap<>();

        //put paths and datas into transaction
        transaction.put(reviewPath, reviewData);
        transaction.put(archivePath, true);

        // Push the review to firebase
        rootRef.updateChildren(transaction, (databaseError, databaseReference) -> {

            if (databaseError != null) {

                Toast.makeText(getApplicationContext(), R.string.review_error, Toast.LENGTH_LONG).show();

            } else {

                Toast.makeText(getApplicationContext(), R.string.review_correctly_submitted, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }


    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromTouch) {

        final int numStars = ratingBar.getNumStars();

        this.writereview_rb_reviewVote.setRating(rating);
    }

}
