package it.polito.mad.sharenbook;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Array;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

class BookDetails {
    private final String GOOGLE_BOOK_WS = "https://www.googleapis.com/books/v1/volumes?q=isbn:";
    private String isbnNumber;
    private JSONObject jsonBook;
    private int totalItems;
    private ArrayList<Book> bookList;

    /**
     * Retrieve book details from GoogleApi Books WS
     *
     * @param isbnNumber
     */
    public BookDetails(String isbnNumber) {
        bookList = new ArrayList<>();
        this.isbnNumber = isbnNumber;

        try {
            this.jsonBook = readJsonFromUrl(GOOGLE_BOOK_WS + isbnNumber);
            totalItems = jsonBook.getInt("totalItems");
            createBookList();
        } catch (IOException | JSONException e) {
            totalItems = 0;
        }
    }

    public int getTotalItems() {
        return totalItems;
    }

    public String getIsbn() {
        return isbnNumber;
    }

    public ArrayList<Book> getBookList() {
        return bookList;
    }

    private void createBookList() throws JSONException {
        JSONArray items = jsonBook.getJSONArray("items");

        for (int i = 0; i < items.length(); i++) {
            JSONObject volumeInfo = items.getJSONObject(i).getJSONObject("volumeInfo");

            // Retrieve book details
            String isbn = isbnNumber;
            String title = retrieveString(volumeInfo, "title");
            String subTitle = retrieveString(volumeInfo, "subtitle");
            String[] authors = retrieveArrayString(volumeInfo, "authors");
            String publisher = retrieveString(volumeInfo, "publisher");
            String publishedDate = retrieveString(volumeInfo, "publishedDate");
            String description = retrieveString(volumeInfo, "description");
            int pageCount = retrieveInteger(volumeInfo, "pageCount");
            String[] categories = retrieveArrayString(volumeInfo, "categories");
            String language = retrieveString(volumeInfo, "language");
            Uri thumbnail = retrieveImageUri(volumeInfo, "thumbnail");
            double averageRating = retrieveDouble(volumeInfo, "averageRating");
            int ratingsCount = retrieveInteger(volumeInfo, "ratingsCount");

            // Create a new Book object
            Book newBook = new Book(isbn, title, subTitle, authors, publisher, publishedDate, description,
                    pageCount, categories, language, thumbnail);
            bookList.add(newBook);
        }
    }

    private String retrieveString(JSONObject volumeInfo, String name) {
        try {
            return volumeInfo.getString(name);
        } catch (JSONException e) {
            return "";
        }
    }

    private String[] retrieveArrayString(JSONObject volumeInfo, String name) {
        JSONArray jsonArray;

        try {
            jsonArray = volumeInfo.getJSONArray(name);
        } catch (JSONException e) {
            return new String[]{};
        }

        String[] array = new String[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                array[i] = jsonArray.getString(i);
            } catch (JSONException e) {
                array[i] = "";
            }
        }

        return array;
    }

    private int retrieveInteger(JSONObject volumeInfo, String name) {
        try {
            return volumeInfo.getInt(name);
        } catch (JSONException e) {
            return -1;
        }
    }

    private double retrieveDouble(JSONObject volumeInfo, String name) {
        try {
            return volumeInfo.getDouble(name);
        } catch (JSONException e) {
            return -1;
        }
    }

    private Uri retrieveImageUri(JSONObject volumeInfo, String name) {
        try {
            JSONObject imageLinks = volumeInfo.getJSONObject("imageLinks");
            return Uri.parse(imageLinks.getString(name));
        } catch (JSONException e) {
            return null;
        }
    }

    /**
     * Return a String containing all data read from a Reader
     */
    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;

        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    /**
     * Return JSONObject from passed URL
     */
    private JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
        InputStream is = new URL(url).openStream();

        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        } finally {
            is.close();
        }
    }


}


/**
 * Book class
 */
class Book implements Parcelable {

    private String isbn;
    private String title;
    private String subTitle;
    private String publisher;
    private String publishedDate;
    private String description;
    private String language;
    private String[] categories;
    private String[] authors;
    private int pageCount;
    private Uri thumbnail;
    private ArrayList<Bitmap> bookPhotos;



    public Book(String isbn, String title, String subTitle, String[] authors, String publisher,
                String publishedDate, String description, int pageCount, String[] categories,
                String language, Uri thumbnail) {
        this.isbn = isbn;
        this.title = title;
        this.subTitle = subTitle;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.description = description;
        this.language = language;
        this.thumbnail = thumbnail;

        if (authors == null)
            this.authors = new String[]{""};
        else
            this.authors = authors;

        this.pageCount = pageCount;

        if (categories == null)
            this.categories = new String[]{""};
        else
            this.categories = categories;

        this.bookPhotos= new ArrayList<>();
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public String[] getAuthors() {
        return authors;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

    public String getDescription() {
        return description;
    }

    public int getPageCount() {
        return pageCount;
    }

    public String[] getCategories() {
        return categories;
    }

    public String getLanguage() {
        return language;
    }

    public Uri getThumbnail() {
        return thumbnail;
    }

    public ArrayList<Bitmap> getBookPhotos() {
        return bookPhotos;
    }

    public void setBookPhotos(ArrayList<Bitmap> bookPhotos) {
        this.bookPhotos = bookPhotos;
    }

    public void addBookPhoto(Bitmap photo){
        this.bookPhotos.add(photo);
    }


    /*******************************
     * Parcelizable implementation
     *
     */

    /**
     * constructor used to create a Book object from parcelized data, the data must be retrieved
     * in the same order as in writeToParcel method
     *
     * @param in : the Parcel object
     */
    public Book(Parcel in) {


        this.isbn = in.readString();
        this.title = in.readString();
        this.subTitle = in.readString();
        this.publisher = in.readString();
        this.publishedDate = in.readString();
        this.description = in.readString();
        this.language = in.readString();
        this.thumbnail = Uri.parse(in.readString());

        int num_authors = in.readInt();
        String[] a = new String[num_authors];
        in.readStringArray(a);
        this.authors = a;

        this.pageCount = in.readInt();

        int num_categories = in.readInt();
        String[] c = new String[num_categories];
        in.readStringArray(c);
        this.categories = c;
    }

    /**
     * method that parcelize a Book object
     *
     * @param dest  : the Parcel object in which the Book must be parcelized
     * @param flags : optional flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(getIsbn());
        dest.writeString(getTitle());
        dest.writeString(getSubTitle());
        dest.writeString(getPublisher());
        dest.writeString(getPublishedDate());
        dest.writeString(getDescription());
        dest.writeString(getLanguage());
        dest.writeString(getThumbnail().toString());

        dest.writeInt(getAuthors().length);
        dest.writeStringArray(getAuthors());

        dest.writeInt(getPageCount());

        dest.writeInt(getCategories().length);
        dest.writeStringArray(getCategories());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

}