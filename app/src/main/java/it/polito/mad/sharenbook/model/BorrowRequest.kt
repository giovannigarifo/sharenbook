package it.polito.mad.sharenbook.model

class BorrowRequest(var requestUsers : HashMap<String, Long>?, var bookId : String, var title : String, var authors: String,
                    var creationTime : String, var requests : Int, var thumbName : String, var owner : String, var bookShared : Boolean){

}