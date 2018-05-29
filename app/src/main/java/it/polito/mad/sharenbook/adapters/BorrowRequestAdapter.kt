package it.polito.mad.sharenbook.adapters

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.v4.app.FragmentManager
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import it.polito.mad.sharenbook.App
import it.polito.mad.sharenbook.R
import it.polito.mad.sharenbook.fragments.GenericAlertDialog
import it.polito.mad.sharenbook.model.BorrowRequest
import java.util.HashMap

class BorrowRequestAdapter(fragManager : FragmentManager) : RecyclerView.Adapter<BorrowRequestAdapter.BorrowReqHolder>()  {

    private val requests : ArrayList<BorrowRequest> = ArrayList()
    val fragmentManager = fragManager
    lateinit var currSelectedRequest : BorrowRequest
    val context = App.getContext()

    val userPreferences = context.getSharedPreferences(context.resources.getString(R.string.username_preferences), Context.MODE_PRIVATE)
    val username = userPreferences.getString(context.resources.getString(R.string.username_copy_key), "void")

    override fun onBindViewHolder(holder: BorrowReqHolder, position: Int) {
        val itemRequest = requests[position]
        holder.bindReview(itemRequest)
    }

    override fun getItemCount(): Int {
        return requests.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) : BorrowReqHolder {
        val inflatedView = parent.inflate(R.layout.request_item, false)
        return BorrowReqHolder(inflatedView)
    }

    fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
        return LayoutInflater.from(App.getContext()).inflate(layoutRes, this, attachToRoot)
    }

    fun addRequest(req : BorrowRequest){
            requests.add(req)
    }

    fun clearRequests(){
        requests.clear()
    }

    fun doPositiveClick() {

        val usernamesDb = FirebaseDatabase.getInstance().getReference(App.getContext().resources.getString(R.string.usernames_key))

        // Create transaction Map
        val transaction = HashMap<String, Any?>()
        transaction.put(username + "/" + context.resources.getString(R.string.borrow_requests_key) + "/" + currSelectedRequest.bookId, null)
        transaction.put(currSelectedRequest.owner + "/" + context.resources.getString(R.string.pending_requests_key) + "/" + currSelectedRequest.bookId + "/" + username, null)

        usernamesDb.updateChildren(transaction, { databaseError, databaseReference ->
            if (databaseError == null) {

                val pos = requests.indexOf(currSelectedRequest)
                requests.remove(currSelectedRequest)
                this.notifyItemRemoved(pos)
                Toast.makeText(App.getContext(), R.string.borrow_request_undone, Toast.LENGTH_LONG).show()

            } else {
                Toast.makeText(App.getContext(), R.string.borrow_request_undone_fail, Toast.LENGTH_LONG).show()
            }
        })


    }

    inner class BorrowReqHolder(v: View) : RecyclerView.ViewHolder(v) {

        private var view : View = v
        private var request : BorrowRequest? = null

        fun bindReview(req : BorrowRequest) {
            this.request = req
            view.findViewById<TextView>(R.id.book_title).text = req.title
            view.findViewById<TextView>(R.id.book_authors).text = req.authors
            view.findViewById<TextView>(R.id.book_creationTime).text = req.creationTime
            view.findViewById<Button>(R.id.undo_breq_button).visibility = View.VISIBLE

            val photoRef = FirebaseStorage.getInstance().getReference("book_images").child(req.bookId + "/" + req.thumbName)
            Glide.with(App.getContext()).load(photoRef).into(view.findViewById<ImageView>(R.id.book_photo))

            view.findViewById<Button>(R.id.undo_breq_button).setOnClickListener({
                currSelectedRequest = req
                showDialog()

            })

            view.findViewById<CardView>(R.id.book_item).setOnClickListener({

                Log.d("CardView Event", "Cardview Pressed")
            })

        }

        internal fun showDialog() {
            val newFragment = GenericAlertDialog.newInstance(
                    R.string.undo_borrow_book, App.getContext().resources.getString(R.string.undo_borrow_book_msg))

            newFragment.show(fragmentManager, "undo_borrow_dialog")
        }

    }

}

