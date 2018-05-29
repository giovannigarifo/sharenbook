package it.polito.mad.sharenbook.adapters

import android.app.Activity
import android.os.Bundle
import android.support.annotation.LayoutRes
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.storage.FirebaseStorage
import it.polito.mad.sharenbook.App
import it.polito.mad.sharenbook.MyBookActivity
import it.polito.mad.sharenbook.R
import it.polito.mad.sharenbook.fragments.RequestListFragment
import it.polito.mad.sharenbook.model.BorrowRequest


class PendingRequestAdapter(activity : Activity) : RecyclerView.Adapter<PendingRequestAdapter.RequestHolder>()  {

    private val requests : ArrayList<BorrowRequest> = ArrayList()
    private val activity = activity as MyBookActivity
    private val fragManager = this.activity.supportFragmentManager

    override fun onBindViewHolder(holder: RequestHolder, position: Int) {
        val itemRequest = requests[position]
        holder.bindReview(itemRequest)
    }

    override fun getItemCount(): Int {
        return requests.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : RequestHolder {
        val inflatedView = parent.inflate(R.layout.request_item, false)
        return RequestHolder(inflatedView)
    }

    fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
        return LayoutInflater.from(App.getContext()).inflate(layoutRes, this, attachToRoot)
    }

    fun addRequest(req : BorrowRequest, pos : Int){
            requests.add(pos, req)
    }

    fun clearRequests(){
        requests.clear()
    }

    inner class RequestHolder(v: View) : RecyclerView.ViewHolder(v) {

        private var view : View = v
        private var request : BorrowRequest? = null

        fun bindReview(req : BorrowRequest) {
            this.request = req
            view.findViewById<TextView>(R.id.book_title).text = req.title
            view.findViewById<TextView>(R.id.book_authors).text = req.authors
            view.findViewById<TextView>(R.id.book_creationTime).text = req.creationTime
            val reqNumber = App.getContext().resources.getString(R.string.borrow_req_counter, req.requests)
            view.findViewById<TextView>(R.id.req_counter).text = reqNumber

            val photoRef = FirebaseStorage.getInstance().getReference("book_images").child(req.bookId + "/" + req.thumbName)
            Glide.with(App.getContext()).load(photoRef).into(view.findViewById<ImageView>(R.id.book_photo))

            view.findViewById<CardView>(R.id.book_item).setOnClickListener({

                val mapSize = req.requestUsers!!.size
                val requestKeys = ArrayList<String>()
                val requestValues = LongArray(mapSize)

                var i = 0
                for (request in req.requestUsers!!) {
                    requestKeys.add(i, request.key)
                    requestValues[i] = request.value
                    i++
                }

                val bundle = Bundle()
                bundle.putStringArrayList("usernameList", requestKeys)
                bundle.putLongArray("requestTimeArray", requestValues)

                var requestFragment = RequestListFragment()
                requestFragment.arguments = bundle

                fragManager.beginTransaction()
                        .replace(R.id.inner_container, requestFragment, "requestList")
                        .addToBackStack("test")
                        .commit();

                Log.d("CardView Event", "Cardview Pressed")
            })

        }
    }
}