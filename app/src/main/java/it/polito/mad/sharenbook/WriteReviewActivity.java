package it.polito.mad.sharenbook;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toolbar;

import com.mikhaellopez.circularimageview.CircularImageView;

public class WriteReviewActivity extends AppCompatActivity {

    //views
    CircularImageView writereview_userPicture;
    TextView writereview_userFullName, writereview_userNickName;
    TextView writereview_givenloaned;
    ImageView writereview_bookPhoto;
    TextView writereview_bookTitle;
    TextView writereview_archivingDate;

    EditText writereview_et_reviewTitle;
    EditText writereview_et_reviewBody;
    SeekBar writereview_sb_reviewVote;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_review);

        //get views
        getViews();

        if(savedInstanceState == null)
            startedFromIntent();
        else startedFromSavedState();
    }

    /**
     * Activity has been started by an intent
     */
    private void startedFromIntent() {

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

        this.writereview_userPicture = findViewById(R.id.writereview_userPicture);
        this.writereview_userFullName = findViewById(R.id.writereview_userFullName);
        this.writereview_userNickName = findViewById(R.id.writereview_userNickName);
        this.writereview_givenloaned = findViewById(R.id.writereview_givenloaned);
        this.writereview_bookPhoto = findViewById(R.id.writereview_bookPhoto);
        this.writereview_bookTitle = findViewById(R.id.writereview_bookTitle);
        this.writereview_archivingDate = findViewById(R.id.writereview_archivingDate);
        this.writereview_et_reviewTitle = findViewById(R.id.writereview_et_reviewTitle);
        this.writereview_et_reviewBody = findViewById(R.id.writereview_et_reviewBody);
        this.writereview_sb_reviewVote = findViewById(R.id.writereview_sb_reviewVote);
    }


    /**
     * Setup common parameters of views
     */
    private void setupViews(){


    }
}
