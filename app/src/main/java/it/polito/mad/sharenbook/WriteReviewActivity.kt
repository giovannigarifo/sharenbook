package it.polito.mad.sharenbook

import android.content.Context
import android.content.SharedPreferences
import android.location.Address
import android.location.Geocoder
import android.os.PersistableBundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap
import java.util.Locale

import it.polito.mad.sharenbook.model.UserProfile
import it.polito.mad.sharenbook.utils.GlideApp
import it.polito.mad.sharenbook.utils.InputValidator
import it.polito.mad.sharenbook.utils.UserInterface

import it.polito.mad.sharenbook.EditBookActivity.MIN_REQUIRED_BOOK_PHOTO

class WriteReviewActivity : AppCompatActivity() {

    //context
    internal var context: Context? = null

    //views
    private var writereview_givenloaned: TextView? = null
    internal var writereview_bookPhoto: ImageView? = null
    internal var writereview_bookTitle: TextView? = null
    internal var writereview_creationTime: TextView? = null

    internal var writereview_tv_reviewHeadingMessage: TextView? = null
    internal var writereview_et_reviewTitle: EditText? = null
    internal var writereview_et_reviewBody: EditText? = null
    internal var writereview_rb_reviewVote: RatingBar? = null

    internal var writereview_fab_save: FloatingActionButton? = null

    //back button
    internal var writereview_back_button: ImageView? = null

    //attributes
    private var bookId: String? = null
    private var exchangeId: String? = null
    private var bookPhoto: String? = null
    private var bookTitle: String? = null
    private var creationTime: Long? = null
    private var userNickName: String? = null
    private var isGiven: Boolean = false

    //the username of the user who write the review
    internal var username: String? = null

    //firebase
    private val firebaseUser: FirebaseUser? = null
    private var mBookImagesStorage: StorageReference? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_write_review)
        context = App.getContext()

        //retrieve firebase storage
        mBookImagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.book_images_key))

        if (savedInstanceState == null)
            startedFromIntent()
        else
            startedFromSavedState(savedInstanceState)

        //get views
        getViews()
        setupViews()
    }

    /**
     * Activity has been started by an intent
     */
    private fun startedFromIntent() {

        //retrieve data from intent
        val bundle = intent.extras

        if (bundle != null) {

            //is given or loaned? update textview
            this.userNickName = bundle.getString("userNickName")
            this.isGiven = bundle.getBoolean("isGiven")

            //book title
            this.bookTitle = bundle.getString("bookTitle")

            //book creation time
            this.creationTime = bundle.getLong("creationTime")

            //book id
            this.bookId = bundle.getString("bookId")

            //book photo
            this.bookPhoto = bundle.getString("bookPhoto")

            //exchangeid
            this.exchangeId = bundle.getString("exchangeId")
        }
    }

    /**
     * Activity is restoring from a preivous instance state, e.g. rotation happened
     */
    private fun startedFromSavedState(savedState: Bundle) {

        this.userNickName = savedState.getString("userNickName")
        this.isGiven = savedState.getBoolean("isGiven")
        this.bookTitle = savedState.getString("bookTitle")
        this.creationTime = savedState.getLong("creationTime")
        this.bookId = savedState.getString("bookId")
        this.bookPhoto = savedState.getString("bookPhoto")
        this.exchangeId = savedState.getString("exchangeId")
    }

    public override fun onSaveInstanceState(outState: Bundle) {

        super.onSaveInstanceState(outState)

        outState.putString("userNickName", this.userNickName)
        outState.putBoolean("isGiven", this.isGiven)
        outState.putString("bookTitle", this.bookTitle)
        outState.putLong("creationTime", this.creationTime!!)
        outState.putString("bookId", this.bookId)
        outState.putString("bookPhoto", this.bookPhoto)
        outState.putString("exchangeId", this.exchangeId)
    }

    /**
     * get views from layout
     */
    private fun getViews() {

        this.writereview_givenloaned = findViewById(R.id.writereview_givenloaned)
        this.writereview_bookPhoto = findViewById(R.id.writereview_bookPhoto)
        this.writereview_bookTitle = findViewById(R.id.writereview_bookTitle)
        this.writereview_creationTime = findViewById(R.id.writereview_creationTime)
        this.writereview_et_reviewTitle = findViewById(R.id.writereview_et_reviewTitle)
        this.writereview_et_reviewBody = findViewById(R.id.writereview_et_reviewBody)
        this.writereview_rb_reviewVote = findViewById(R.id.writereview_rb_reviewVote)
        this.writereview_fab_save = findViewById(R.id.writereview_fab_save)
        this.writereview_tv_reviewHeadingMessage = findViewById(R.id.writereview_tv_reviewHeadingMessage)
        this.writereview_back_button = findViewById(R.id.writereview_back_button)
    }


    /**
     * Setup common parameters of views
     */
    private fun setupViews() {

        //book title
        this.writereview_bookTitle!!.text = this.bookTitle

        //given or loaned: heading
        if (isGiven == true)
            this.writereview_givenloaned!!.text = resources.getString(R.string.writereview_review_given) + " " + this.userNickName
        else
            this.writereview_givenloaned!!.text = this.userNickName + " " + resources.getString(R.string.writereview_review_loaned)

        //book creation time
        if (this.creationTime != null)
            this.writereview_creationTime!!.text = resources.getString(R.string.writereview_creationTime_incipit) + " " + DateUtils.formatDateTime(context, this.creationTime!!,
                    DateUtils.FORMAT_SHOW_DATE
                            or DateUtils.FORMAT_NUMERIC_DATE
                            or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_24HOUR)

        //book photo
        val photoRef = mBookImagesStorage!!.child(this.bookId!!).child(this.bookPhoto!!)

        // Load book photo
        GlideApp.with(App.getContext())
                .load(photoRef)
                .placeholder(R.drawable.book_cover_portrait)
                .into(this.writereview_bookPhoto!!)

        // review heading message
        this.writereview_tv_reviewHeadingMessage!!.text = resources.getString(R.string.writereview_review_headingMessage) + " " + userNickName

        //back button
        this.writereview_back_button!!.setOnClickListener { v -> onBackPressed() }

        //fab
        this.writereview_fab_save!!.setOnClickListener { v -> if (validateInputFields()) saveReviewToFirebase() }

    }


    /**
     * get all reviews data from the user input and create the hashmap ready to be pushed to firebase
     */
    private fun retrieveDataAndCreateReview(): HashMap<String, Any> {

        val reviewData = HashMap<String, Any>()

        //book id associated with the review
        reviewData["bookId"] = this.bookId.toString()

        //title
        val title = this.writereview_et_reviewTitle!!.text.toString().trim { it <= ' ' }.replace("\"\'\\", "")
        reviewData["rTitle"] = title

        //body
        val body = this.writereview_et_reviewBody!!.text.toString().trim { it <= ' ' }.replace("\"\'\\", "")
        reviewData["rText"] = body

        //score
        val score = this.writereview_rb_reviewVote!!.progress
        reviewData["rating"] = score

        //creation time of the review
        reviewData["date"] = ServerValue.TIMESTAMP

        //isGiven
        reviewData["given"] = this.isGiven

        //the userId of who writes the review
        val userData = context!!.getSharedPreferences(context!!.getString(R.string.userData_preferences), Context.MODE_PRIVATE)
        username = userData.getString(context!!.getString(R.string.username_pref), "void")

        if (username != "void")
            reviewData["creator"] = username.toString()
        else
            Toast.makeText(context, "error in username", Toast.LENGTH_LONG).show()
        //TODO: to be changed

        return reviewData
    }

    /**
     * save the review into firebase
     */
    internal fun saveReviewToFirebase() {

        //retrieve data from view and create review object
        val reviewData = retrieveDataAndCreateReview()

        //reference to the reviews of the users in firebase
        val rootRef = FirebaseDatabase.getInstance().reference

        //get new review key
        val reviewKey = rootRef
                .child("usernames/" + userNickName + "/" + getString(R.string.reviews_key))
                .push().key

        //archive path of the user who released the review
        val archivePath = "shared_books/" + username + "/archive_books/" + this.exchangeId + "/reviewed"

        //review path
        val reviewPath = "usernames/$userNickName/reviews/$reviewKey"

        //create transaction map
        val transaction = HashMap<String, Any>()

        //put paths and datas into transaction
        transaction[reviewPath] = reviewData
        transaction[archivePath] = true

        // Push the review to firebase
        rootRef.updateChildren(transaction) { databaseError, databaseReference ->

            if (databaseError != null) {

                Toast.makeText(applicationContext, R.string.review_error, Toast.LENGTH_LONG).show()

            } else {

                Toast.makeText(applicationContext, R.string.review_correctly_submitted, Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }


    /**
     * Validate user inputs
     */
    private fun validateInputFields(): Boolean {

        var isValid = true
        var alreadyFocused = false

        //test title
        if (writereview_et_reviewTitle!!.text.toString().isEmpty()) {
            writereview_et_reviewTitle!!.error = getText(R.string.field_required)
            isValid = false
            if (!alreadyFocused) {
                writereview_et_reviewTitle!!.requestFocus()
                alreadyFocused = true
            }
        }

        //test body
        if (writereview_et_reviewBody!!.text.toString().isEmpty()) {
            writereview_et_reviewBody!!.error = getText(R.string.field_required)
            isValid = false
            if (!alreadyFocused) {
                writereview_et_reviewBody!!.requestFocus()
                alreadyFocused = true
            }
        }

        return isValid
    }

    /**
     * onBackPressed method
     */
    override fun onBackPressed() {

        val exitRequest = AlertDialog.Builder(this@WriteReviewActivity) //give a context to Dialog
        exitRequest.setTitle(R.string.exit_request_title)
        exitRequest.setMessage(R.string.exit_rationale)
        exitRequest.setPositiveButton(android.R.string.ok
        ) { dialog, which -> finish() }.setNegativeButton(android.R.string.cancel
        ) { dialog, which -> dialog.dismiss() }

        exitRequest.show()
    }


}
