package it.polito.mad.sharenbook;

import android.content.Context;
import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.mikhaellopez.circularimageview.CircularImageView;

import it.polito.mad.sharenbook.fragments.ProfileReviewsFragment;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.UserInterface;

public class ShowOthersProfile extends AppCompatActivity {

    private Context context;
    private String ownerUsername;

    private CircularImageView iv_profile;
    private TextView tv_username;
    private ImageView back_btn;
    private FloatingActionButton open_chat_fab;
    private RatingBar ratingBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_others_profile);

        context = getApplicationContext();

        ownerUsername = getIntent().getStringExtra("username");

        back_btn = findViewById(R.id.back_button);
        open_chat_fab = findViewById(R.id.openChat);
        iv_profile = findViewById(R.id.userPicture);
        tv_username = findViewById(R.id.tv_userNickName);
        ratingBar = findViewById(R.id.ratingBar);
        tv_username.setText(ownerUsername);

        DatabaseReference recipientPicSignature = FirebaseDatabase.getInstance().getReference("usernames").child(ownerUsername).child("picSignature");
        recipientPicSignature.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    long picSignature = (long) dataSnapshot.getValue();
                    UserInterface.showGlideImage(context,
                            FirebaseStorage.getInstance().getReference().child("/images").child("/" + ownerUsername + ".jpg"),
                            iv_profile,
                            picSignature);

                    iv_profile.setOnClickListener(v -> {
                        Intent i = new Intent(getApplicationContext(), ShowPictureActivity.class);
                        i.putExtra("PictureSignature", String.valueOf(picSignature));
                        i.putExtra("pathPortion", ownerUsername);
                        startActivity(i);
                    });

                } else {
                    GlideApp.with(context).load(context.getResources().getDrawable(R.drawable.ic_profile)).into(iv_profile);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        back_btn.setOnClickListener(v -> finish());

        open_chat_fab.setOnClickListener(v -> {
            Intent chatActivity = new Intent(context, ChatActivity.class);
            chatActivity.putExtra("recipientUsername",ownerUsername);
            context.startActivity(chatActivity);
        });

        if (savedInstanceState == null) {
            showProfileReviewsFragment();
        }
    }

    public void setRating(float floatRating){
        ratingBar.setRating(floatRating);
    }

    private void showProfileReviewsFragment(){

        ProfileReviewsFragment frag = new ProfileReviewsFragment();
        Bundle bundle = new Bundle();
        bundle.putString("username", ownerUsername);
        frag.setArguments(bundle);

        getSupportFragmentManager().beginTransaction()
                .add(R.id.reviewsContainer, frag, "profileReviews")
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit();
    }

}
