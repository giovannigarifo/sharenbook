package it.polito.mad.sharenbook.model;

import android.content.Context;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import android.text.format.DateUtils;

import it.polito.mad.sharenbook.R;


/**
 * Book class
 */
public class Book implements Parcelable {

    private String bookId;
    private String isbn;
    private String title;
    private String subtitle;
    private String publisher;
    private String publishedDate;
    private String description;
    private String language;
    private List<Integer> categories;
    private List<String> authors;
    private int pageCount;
    private String thumbnail;
    private List<Uri> bookPhotosUri;

    private String owner_uid;
    private String owner_username;
    private int bookConditions;
    private List<String> tags;
    private int numPhotos;
    private long creationTime;
    private double location_lat;
    private double location_long;
    private List<String> photosName;
    private boolean shared;


    /**
     * Constructor for the Book Class called by BookDetails
     */
    public Book(String isbn, String title, String subtitle, List<String> authors, String publisher,
                String publishedDate, String description, int pageCount, String language, String thumbnail) {

        this.bookId = "";
        this.isbn = isbn;
        this.title = title;
        this.subtitle = subtitle;
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
        this.bookPhotosUri = new ArrayList<>();

        this.categories = new ArrayList<>();
        this.owner_uid = "";
        this.owner_username = "";
        this.bookConditions = -1;
        this.tags = new ArrayList<>();
        this.numPhotos = 0;
        this.creationTime = 0;
        this.location_lat = 0;
        this.location_long = 0;
        this.photosName = new ArrayList<>();
        this.shared = false;
    }


    /**
     * Constructor for the Book Class called Algolia
     */
    public Book(String bookId, String owner_uid, String owner_username, String isbn, String title, String subtitle, List<String> authors, String publisher,
                String publishedDate, String description, int pageCount, List<Integer> categories, String language, String thumbnail, int numPhotos,
                int bookConditions, List<String> tags, long creationTime, double location_lat, double location_long, List<String> photosName, boolean shared) {

        this(isbn, title, subtitle, authors, publisher, publishedDate, description, pageCount, language, thumbnail);
        this.bookId = bookId;
        this.categories = categories;
        this.owner_uid = owner_uid;
        this.owner_username = owner_username;
        this.numPhotos = numPhotos;
        this.bookConditions = bookConditions;
        this.tags = tags;
        this.creationTime = creationTime;
        this.location_lat = location_lat;
        this.location_long = location_long;
        this.photosName = photosName;
        this.shared = shared;
    }

    /**
     * Constructor for the Book Class called by Parcelable
     */
    public Book() {
        this.bookId = "";
        this.isbn = "";
        this.title = "";
        this.subtitle = "";
        this.publisher = "";
        this.publishedDate = "";
        this.description = "";
        this.language = "";
        this.categories = new ArrayList<>();
        this.authors = new ArrayList<>();
        this.pageCount = -1;
        this.thumbnail = "";
        this.bookPhotosUri = new ArrayList<>();

        this.owner_uid = "";
        this.owner_username = "";
        this.bookConditions = -1;
        this.tags = new ArrayList<>();
        this.numPhotos = 0;
        this.creationTime = 0;
        this.location_lat = 0;
        this.location_long = 0;
        this.photosName = new ArrayList<>();
        this.shared = false;
    }


    public String getBookId() {
        return bookId;
    }

    public String getIsbn() {
        return isbn;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public String getAuthorsAsString() {

        String authors = "";

        for (int i = 0; i < this.authors.size(); i++) {

            String author = this.authors.get(i);

            if (i == 0) {
                authors = authors.concat(author);
            } else {
                authors = authors.concat(", " + author);
            }
        }

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

    public List<Integer> getCategories() {
        return categories;
    }

    /**
     * @param bookCategoriesStringArray the array of strings obtained from string.xml file
     */
    public String getCategoriesAsString(String[] bookCategoriesStringArray) {

        String categoriesAsString = "";

        for (int i = 0; i < this.categories.size(); i++) {

            String category = Arrays.asList(bookCategoriesStringArray).get(this.categories.get(i));

            if (i == 0)
                categoriesAsString = category;
            else
                categoriesAsString = categoriesAsString + ", " + category;
        }

        return categoriesAsString;
    }

    public String getLanguage() {
        return language;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public List<Uri> getBookPhotosUri() {
        return this.bookPhotosUri;
    }

    public void addBookPhotoUri(Uri photoUri) {
        this.bookPhotosUri.add(0, photoUri);
    }

    public String getOwner_uid() {
        return owner_uid;
    }

    public String getOwner_username() {
        return owner_username;
    }

    public int getBookConditions() {
        return bookConditions;
    }

    /**
     * @param bookConditionsStringArray the array of strings obtained from string.xml file
     */
    public String getBookConditionsAsString(String[] bookConditionsStringArray) {
        return bookConditionsStringArray[this.bookConditions];
    }

    public List<String> getTags() {
        return tags;
    }

    public int getNumPhotos() {
        return numPhotos;
    }

    public long getCreationTime() {
        return creationTime;
    }

    public String getCreationTimeAsString(Context context) {

        //Calendar c = Calendar.getInstance();
        //c.setTimeInMillis(this.creationTime);
        //String date = DateFormat.format("dd/MM, hh:mm", c).toString();

        String formattedDate = DateUtils.formatDateTime(context, this.creationTime,
                DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_NUMERIC_DATE
                        | DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_24HOUR);

        return formattedDate;
    }

    public double getLocation_lat() {
        return location_lat;
    }

    public double getLocation_long() {
        return location_long;
    }

    public List<String> getPhotosName() {
        return photosName;
    }

    public boolean isShared() {
        return shared;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public void setPublishedDate(String publishedDate) {
        this.publishedDate = publishedDate;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setCategories(List<Integer> categories) {
        this.categories = categories;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public void setBookPhotosUri(List<Uri> bookPhotosUri) {
        this.bookPhotosUri = bookPhotosUri;
    }

    public void setOwner_uid(String owner_uid) {
        this.owner_uid = owner_uid;
    }

    public void setOwner_username(String owner_username) {
        this.owner_username = owner_username;
    }

    public void setBookConditions(int bookConditions) {
        this.bookConditions = bookConditions;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setNumPhotos(int numPhotos) {
        this.numPhotos = numPhotos;
    }

    public void setCreationTime(long creationTime) {
        this.creationTime = creationTime;
    }

    public void setLocation_lat(double location) {
        this.location_lat = location;
    }

    public void setLocation_long(double location) {
        this.location_long = location;
    }

    public void setPhotosName(List<String> photosName) {
        this.photosName = photosName;
    }

    public void setShared(boolean shared) {
        this.shared = shared;
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

        this.bookId = in.readString();
        this.isbn = in.readString();
        this.title = in.readString();
        this.subtitle = in.readString();
        this.publisher = in.readString();
        this.publishedDate = in.readString();
        this.description = in.readString();
        this.language = in.readString();
        this.thumbnail = in.readString();

        this.authors = in.readArrayList(String.class.getClassLoader());

        this.pageCount = in.readInt();

        this.categories = in.readArrayList(Integer.class.getClassLoader());

        this.bookPhotosUri = in.readArrayList(Uri.class.getClassLoader());

        this.owner_uid = in.readString();
        this.owner_username = in.readString();
        this.bookConditions = in.readInt();
        this.tags = in.readArrayList(String.class.getClassLoader());
        this.numPhotos = in.readInt();
        this.creationTime = in.readLong();
        this.location_lat = in.readDouble();
        this.location_long = in.readDouble();
        this.photosName = in.readArrayList(String.class.getClassLoader());
        this.shared = in.readInt() == 1;
    }

    /**
     * method that parcelize a Book object
     *
     * @param dest  : the Parcel object in which the Book must be parcelized
     * @param flags : optional flags
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(getBookId());
        dest.writeString(getIsbn());
        dest.writeString(getTitle());
        dest.writeString(getSubtitle());
        dest.writeString(getPublisher());
        dest.writeString(getPublishedDate());
        dest.writeString(getDescription());
        dest.writeString(getLanguage());
        dest.writeString(getThumbnail());

        dest.writeList(getAuthors());

        dest.writeInt(getPageCount());

        dest.writeList(getCategories());

        dest.writeList(getBookPhotosUri());

        dest.writeString(getOwner_uid());
        dest.writeString(getOwner_username());
        dest.writeInt(getBookConditions());
        dest.writeList(getTags());
        dest.writeInt(getNumPhotos());
        dest.writeLong(getCreationTime());
        dest.writeDouble(getLocation_lat());
        dest.writeDouble(getLocation_long());
        dest.writeList(getPhotosName());
        dest.writeInt(isShared() ? 1 : 0);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator CREATOR = new Creator() {

        public Book createFromParcel(Parcel in) {
            return new Book(in);
        }

        public Book[] newArray(int size) {
            return new Book[size];
        }
    };

}
