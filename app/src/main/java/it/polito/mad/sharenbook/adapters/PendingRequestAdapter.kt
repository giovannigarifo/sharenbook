package it.polito.mad.sharenbook.adapters

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
import it.polito.mad.sharenbook.R
import it.polito.mad.sharenbook.model.BorrowRequest



class PendingRequestAdapter : RecyclerView.Adapter<PendingReqHolder>()  {

    private val requests : ArrayList<BorrowRequest> = ArrayList()

    override fun onBindViewHolder(holder: PendingReqHolder, position: Int) {
        val itemRequest = requests[position]
        holder.bindReview(itemRequest)
    }

    override fun getItemCount(): Int {
        return requests.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : PendingReqHolder {
        val inflatedView = parent.inflate(R.layout.request_item, false)
        return PendingReqHolder(inflatedView)
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

}

class PendingReqHolder(v: View) : RecyclerView.ViewHolder(v) {

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

            //val announcementsFragment = ShowMyAnnouncements()

            /*(BorrowRequestsFragment.newInstance().activity)!!.supportFragmentManager.beginTransaction()
                    .replace(R.id.container, announcementsFragment)
                    .commit()*/

            Log.d("CardView Event", "Cardview Pressed")
        })

    }

}