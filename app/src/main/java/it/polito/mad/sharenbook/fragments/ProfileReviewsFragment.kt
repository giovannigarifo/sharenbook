package it.polito.mad.sharenbook.fragments

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import it.polito.mad.sharenbook.App
import it.polito.mad.sharenbook.R
import it.polito.mad.sharenbook.adapters.ReviewsAdapter
import it.polito.mad.sharenbook.model.Review

class ProfileReviewsFragment : Fragment() {

    private val reviewAdapter : ReviewsAdapter = ReviewsAdapter()
    private lateinit var linearLayoutManager: LinearLayoutManager

    companion object {

        fun newInstance(): ProfileReviewsFragment {
            return ProfileReviewsFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val reviewList: View = inflater.inflate(R.layout.fragment_show_user_reviews, container, false)
        linearLayoutManager = LinearLayoutManager(App.getContext())

        val recyclerView = reviewList.findViewById<RecyclerView>(R.id.reviews) as RecyclerView
        recyclerView.layoutManager = linearLayoutManager

        recyclerView.adapter = reviewAdapter

        val r = Review(2, "Tutto bene con lo scambio", "il libro faceva schifo ma tutto ok. Poteva andare peggio",
                "Daviiid", "27/05/2018, 20:00")
        reviewAdapter.addReview(r)
        reviewAdapter.notifyDataSetChanged()

        return reviewList
    }

}


