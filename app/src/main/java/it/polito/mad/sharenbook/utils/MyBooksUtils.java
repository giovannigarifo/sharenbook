package it.polito.mad.sharenbook.utils;

import java.util.ArrayList;

import it.polito.mad.sharenbook.model.Book;

public class MyBooksUtils {
    private static ArrayList<Book> myBooks;

    public static void setMyBooks(ArrayList<Book> myBooks) {
        MyBooksUtils.myBooks = myBooks;
    }
    public static ArrayList<Book> getMyBooks(){
        return MyBooksUtils.myBooks;
    }
}
