package it.polito.mad.sharenbook.adapters

import android.support.annotation.LayoutRes
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import it.polito.mad.sharenbook.App
import it.polito.mad.sharenbook.R
import it.polito.mad.sharenbook.model.Review


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
    }

}


class ReviewsHolder(v: View) : RecyclerView.ViewHolder(v) {

    private var view: View = v
    private var review: Review? = null

    fun bindReview(r: Review) {
        this.review = r
        view.findViewById<TextView>(R.id.review_title).text = r.rTitle
        view.findViewById<TextView>(R.id.review_text).text = r.rText
        view.findViewById<TextView>(R.id.review_creation).text = r.date
        view.findViewById<RatingBar>(R.id.ratingBar).rating = 2.0f
    }

}