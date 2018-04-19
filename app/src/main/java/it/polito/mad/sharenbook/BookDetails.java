package it.polito.mad.sharenbook;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
        } catch (IOException e) {
            totalItems = -1;
        } catch (JSONException e) {
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
            List<String> authors = retrieveArrayList(volumeInfo, "authors");
            String publisher = retrieveString(volumeInfo, "publisher");
            String publishedDate = retrieveString(volumeInfo, "publishedDate");
            String description = retrieveString(volumeInfo, "description");
            int pageCount = retrieveInteger(volumeInfo, "pageCount");
            List<String> categories = retrieveArrayList(volumeInfo, "categories");
            String language = retrieveString(volumeInfo, "language");
            String thumbnail = retrieveImageLink(volumeInfo, "thumbnail");

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

    private List<String> retrieveArrayList(JSONObject volumeInfo, String name) {
        JSONArray jsonArray;

        try {
            jsonArray = volumeInfo.getJSONArray(name);
        } catch (JSONException e) {
            return new ArrayList<>();
        }

        List<String> arrayList = new ArrayList<>(jsonArray.length());

        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                arrayList.add(jsonArray.getString(i));
            } catch (JSONException e) {
                arrayList.add("");
            }
        }

        return arrayList;
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

    private String retrieveImageLink(JSONObject volumeInfo, String name) {
        try {
            JSONObject imageLinks = volumeInfo.getJSONObject("imageLinks");
            return imageLinks.getString(name);
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * Return a String containing all data read from a Reader
     */
    private String readAll(BufferedReader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        String inputLine;

        while ((inputLine = rd.readLine()) != null) {
            sb.append(inputLine);
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
    private List<String> categories;
    private List<String> authors;
    private int pageCount;
    private String thumbnail;
    private ArrayList<Bitmap> bookPhotos;

    private String owner_uid;
    private String bookConditions;
    private List<String> tags;



    /**
     * Constructor for the Book Class
     * @param isbn
     * @param title
     * @param subTitle
     * @param authors
     * @param publisher
     * @param publishedDate
     * @param description
     * @param pageCount
     * @param categories
     * @param language
     * @param thumbnail
     */
    public Book(String isbn, String title, String subTitle, List<String> authors, String publisher,
                String publishedDate, String description, int pageCount, List<String> categories,
                String language, String thumbnail) {
        this.isbn = isbn;
        this.title = title;
        this.subTitle = subTitle;
        this.publisher = publisher;
        this.publishedDate = publishedDate;
        this.description = description;
        this.language = language;
        this.thumbnail = thumbnail;

        if (authors == null)
            this.authors = new ArrayList<>();
        else
            this.authors = authors;

        this.pageCount = pageCount;

        if (categories == null)
            this.categories = new ArrayList<>();
        else
            this.categories = categories;

        this.bookPhotos = new ArrayList<>();

        try {

            URL url = new URL(this.thumbnail.toString());
            Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
            this.bookPhotos.add(bmp);

        } catch(Exception e) {
            e.printStackTrace();
            Log.d("error", "Unable to retrieve the bitmap from the thumbnail Uri.");
        }

        this.owner_uid = "";
        this.bookConditions = "";
        this.tags = new ArrayList<>();

    }

    public Book() {
        this.isbn = "";
        this.title = "";
        this.subTitle = "";
        this.publisher = "";
        this.publishedDate = "";
        this.description = "";
        this.language = "";
        this.categories = new ArrayList<>();
        this.authors = new ArrayList<>();
        this.pageCount = -1;
        this.thumbnail = "";
        this.bookPhotos = new ArrayList<>();

        this.owner_uid = "";
        this.bookConditions = "";
        this.tags = new ArrayList<>();
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

    public List<String> getAuthors() {
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

    public List<String> getCategories() {
        return categories;
    }

    public String getLanguage() {
        return language;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public ArrayList<Bitmap> getBookPhotos() {
        return this.bookPhotos;
    }

    public void addBookPhoto(Bitmap photo){
        this.bookPhotos.add(0, photo);
    }

    public String getOwner_uid() {
        return owner_uid;
    }

    public String getBookConditions() {
        return bookConditions;
    }

    public List<String> getTags() {
        return tags;
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
        this.thumbnail = in.readString();

        int num_authors = in.readInt();
        this.authors = in.readArrayList(String.class.getClassLoader());

        this.pageCount = in.readInt();

        int num_categories = in.readInt();
        this.categories = in.readArrayList(String.class.getClassLoader());

        //instantiate the photo collection, it's not parcelled
        this.bookPhotos = in.readArrayList(null);

        this.owner_uid = in.readString();
        this.bookConditions = in.readString();
        this.tags = in.readArrayList(String.class.getClassLoader());
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
        dest.writeString(getThumbnail());

        dest.writeInt(getAuthors().size());
        dest.writeList(getAuthors());

        dest.writeInt(getPageCount());

        dest.writeInt(getCategories().size());
        dest.writeList(getCategories());

        dest.writeList(getBookPhotos());

        dest.writeString(getOwner_uid());
        dest.writeString(getBookConditions());
        dest.writeList(getTags());
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