package it.polito.mad.sharenbook.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.database.*
import it.polito.mad.sharenbook.App
import it.polito.mad.sharenbook.R
import it.polito.mad.sharenbook.adapters.ReviewsAdapter
import it.polito.mad.sharenbook.model.Review
import it.polito.mad.sharenbook.utils.Utils
import java.util.ArrayList

class ProfileReviewsFragment : Fragment() {

    private val reviewAdapter : ReviewsAdapter = ReviewsAdapter()
    private lateinit var linearLayoutManager: LinearLayoutManager

    private val reviewsList = ArrayList<Review>()

    /** FireBase objects  */
    private lateinit var reviewsDb: DatabaseReference

    companion object {

        fun newInstance(username : String): ProfileReviewsFragment {

            val frag = ProfileReviewsFragment()
            val bundle = Bundle()
            bundle.putString("username", username)
            frag.arguments = bundle
            return frag

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val reviewList: View = inflater.inflate(R.layout.fragment_show_user_reviews, container, false)
        linearLayoutManager = LinearLayoutManager(App.getContext())

        val recyclerView = reviewList.findViewById<RecyclerView>(R.id.reviews) as RecyclerView
        recyclerView.layoutManager = linearLayoutManager

        recyclerView.adapter = reviewAdapter

        /*val r = Review(2, "Tutto bene con lo scambio", "il libro faceva schifo ma tutto ok. Poteva andare peggio",
                "Daviiid", "27/05/2018, 20:00")
        reviewAdapter.addReview(r)
        reviewAdapter.notifyDataSetChanged()*/

        loadReviews(arguments!!.getString("username", "void"))

        return reviewList
    }

    private fun loadReviews(username : String) {

        // Setup FireBase
        val firebaseDatabase = FirebaseDatabase.getInstance()
        reviewsDb = firebaseDatabase.getReference(getString(R.string.usernames_key)).child(username).child(getString(R.string.reviews_key))

        reviewsDb.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.value != null) {

                    /*if (dataSnapshot.value == getString(R.string.users_books_placeholder)) {
                        /** no reviews  */

                        //TODO this one
                        Toast.makeText(context, "No Reviews available", Toast.LENGTH_SHORT).show()

                    } else {*/
                    /** there are reviews  */

                    if (reviewsList.isEmpty()) {

                        val reviews = dataSnapshot.children
                        val reviewsReverse = Utils.toReverseList(reviews)

                        for (review in reviewsReverse) {

                            val rev: Review = review.getValue(Review::class.java)!!

                            Log.d("reeeev", "here: " + rev.getrTitle())
                            reviewAdapter.addReview(rev!!)
                        }

                    }
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(App.getContext(), getString(R.string.databaseError), Toast.LENGTH_SHORT).show()

            }
        })
    }

}


