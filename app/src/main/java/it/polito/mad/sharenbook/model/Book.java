package it.polito.mad.sharenbook.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

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
    private List<String> categories;
    private List<String> authors;
    private int pageCount;
    private String thumbnail;
    private List<Uri> bookPhotosUri;

    private String owner_uid;
    private String bookConditions;
    private List<String> tags;
    private int numPhotos;



    /**
     * Constructor for the Book Class
     * @param isbn
     * @param title
     * @param subtitle
     * @param authors
     * @param publisher
     * @param publishedDate
     * @param description
     * @param pageCount
     * @param categories
     * @param language
     * @param thumbnail
     */
    public Book(String isbn, String title, String subtitle, List<String> authors, String publisher,
                String publishedDate, String description, int pageCount, List<String> categories,
                String language, String thumbnail) {

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

        if (categories == null)
            this.categories = new ArrayList<>();
        else
            this.categories = categories;

        this.bookPhotosUri = new ArrayList<>();

        this.owner_uid = "";
        this.bookConditions = "";
        this.tags = new ArrayList<>();
        this.numPhotos = 0;
    }


    /**
     * Constructor for the Book Class
     * @param isbn
     * @param title
     * @param subtitle
     * @param authors
     * @param publisher
     * @param publishedDate
     * @param description
     * @param pageCount
     * @param categories
     * @param language
     * @param thumbnail
     * @param numPhotos
     */
    public Book(String isbn, String title, String subtitle, List<String> authors, String publisher,
                String publishedDate, String description, int pageCount, List<String> categories,
                String language, String thumbnail, int numPhotos) {

        this(isbn, title, subtitle, authors, publisher, publishedDate, description, pageCount, categories, language, thumbnail);
        this.bookId = "";
        this.numPhotos = numPhotos;
    }

    /**
     * Constructor for the Book Class
     */
    public Book() {
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
        this.bookConditions = "";
        this.tags = new ArrayList<>();
        this.numPhotos = 0;
    }



    public String getBookId() { return bookId; }

    public void setBookId(String bookId) { this.bookId = bookId; }

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

    public List<Uri> getBookPhotosUri() {
        return this.bookPhotosUri;
    }

    public void addBookPhotoUri(Uri photoUri){
        this.bookPhotosUri.add(0, photoUri);
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

    public int getNumPhotos() {
        return numPhotos;
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

    public void setCategories(List<String> categories) {
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

    public void setBookConditions(String bookConditions) {
        this.bookConditions = bookConditions;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void setNumPhotos(int numPhotos) {
        this.numPhotos = numPhotos;
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
        this.subtitle = in.readString();
        this.publisher = in.readString();
        this.publishedDate = in.readString();
        this.description = in.readString();
        this.language = in.readString();
        this.thumbnail = in.readString();

        this.authors = in.readArrayList(String.class.getClassLoader());

        this.pageCount = in.readInt();

        this.categories = in.readArrayList(String.class.getClassLoader());

        this.bookPhotosUri = in.readArrayList(Uri.class.getClassLoader());

        this.owner_uid = in.readString();
        this.bookConditions = in.readString();
        this.tags = in.readArrayList(String.class.getClassLoader());
        this.numPhotos = in.readInt();
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
        dest.writeString(getBookConditions());
        dest.writeList(getTags());
        dest.writeInt(getNumPhotos());
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
