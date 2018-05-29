package it.polito.mad.sharenbook.model

class BorrowRequest(var requestUsers : ArrayList<String>?, var bookId : String, var title : String, var authors: String,
                    var creationTime : String, var requests : Int, var thumbName : String){

}