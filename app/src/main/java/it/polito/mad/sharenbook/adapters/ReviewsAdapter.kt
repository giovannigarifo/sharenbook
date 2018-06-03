package it.polito.mad.sharenbook.adapters

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import it.polito.mad.sharenbook.App
import it.polito.mad.sharenbook.R
import it.polito.mad.sharenbook.model.Review
import it.polito.mad.sharenbook.utils.GlideApp
import it.polito.mad.sharenbook.utils.UserInterface


class ReviewsAdapter : RecyclerView.Adapter<ReviewsHolder>()  {

    private val review : ArrayList<Review> = ArrayList()

    override fun onBindViewHolder(holder: ReviewsHolder, position: Int) {
        val itemReview = review[position]
        holder.bindReview(itemReview)
    }

    override fun getItemCount(): Int {
        return review.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : ReviewsHolder{
        val inflatedView = parent.inflate(R.layout.review_item, false)
        return ReviewsHolder(inflatedView)
    }

    fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
        return LayoutInflater.from(App.getContext()).inflate(layoutRes, this, attachToRoot)
    }

    fun addReview(r : Review){
        review.add(r)
        notifyItemInserted(itemCount-1)
    }

}


class ReviewsHolder(v: View) : RecyclerView.ViewHolder(v) {

    private var view: View = v
    private var review: Review? = null

    fun bindReview(r: Review) {
        this.review = r
        view.findViewById<TextView>(R.id.review_title).text = r.getrTitle()
        view.findViewById<TextView>(R.id.review_text).text = r.getrText()
        view.findViewById<TextView>(R.id.review_creation).text = DateUtils.formatDateTime(App.getContext(), r.date,
                DateUtils.FORMAT_SHOW_DATE
                        or DateUtils.FORMAT_NUMERIC_DATE
                        or DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_24HOUR)
        view.findViewById<RatingBar>(R.id.ratingBar).rating = (r.rating)/2.0f
        view.findViewById<TextView>(R.id.text_user).setText(r.creator)
        view.findViewById<ImageView>(R.id.img_popup_menu).visibility = View.INVISIBLE
        val imgView = view.findViewById<ImageView>(R.id.img)

        val recipientPicSignature = FirebaseDatabase.getInstance().getReference("usernames").child(r.creator).child("picSignature")
        recipientPicSignature.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val picSignature = dataSnapshot.value as Long
                    UserInterface.showGlideImage(App.getContext(),
                            FirebaseStorage.getInstance().reference.child("/images").child("/${r.creator}.jpg"),
                            imgView,
                            picSignature)
                } else {
                    GlideApp.with(App.getContext()).load(App.getContext().getResources().getDrawable(R.drawable.ic_profile)).into(imgView)
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {

            }
        })

    }

}