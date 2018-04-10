package it.polito.mad.lab1;

import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

class BookDetails
{
    private final String GOOGLE_BOOK_WS = "https://www.googleapis.com/books/v1/volumes?q=isbn:";
    private String isbnNumber;
    private JSONObject jsonBook;
    private int totalItems;
    private ArrayList<Book> bookList;

    /**
     * Retrieve book details from GoogleApi Books WS
     * @param isbnNumber
     */
    public BookDetails(String isbnNumber)
    {
        bookList = new ArrayList<>();
        this.isbnNumber = isbnNumber;

        try {
            this.jsonBook = readJsonFromUrl(GOOGLE_BOOK_WS + isbnNumber);
            totalItems = jsonBook.getInt("totalItems");
            createBookList();
        }
        catch (IOException | JSONException e) {
            totalItems = 0;
        }
    }

    public int getTotalItems()
    {
        return totalItems;
    }

    public String getIsbn()
    {
        return isbnNumber;
    }

    public ArrayList<Book> getBookList()
    {
        return bookList;
    }

    private void createBookList() throws JSONException
    {
        JSONArray items = jsonBook.getJSONArray("items");

        for (int i = 0; i < items.length(); i++)
        {
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
                    pageCount, categories, language, thumbnail, averageRating, ratingsCount);
            bookList.add(newBook);
        }
    }

    private String retrieveString(JSONObject volumeInfo, String name)
    {
        try {
            return volumeInfo.getString(name);
        }
        catch (JSONException e) {
            return "";
        }
    }

    private String[] retrieveArrayString(JSONObject volumeInfo, String name)
    {
        JSONArray jsonArray;

        try {
            jsonArray = volumeInfo.getJSONArray(name);
        }
        catch (JSONException e) {
            return new String[]{};
        }

        String[] array = new String[jsonArray.length()];

        for (int i = 0; i < jsonArray.length(); i++)
        {
            try {
                array[i] = jsonArray.getString(i);
            }
            catch (JSONException e) {
                array[i] = "";
            }
        }

        return array;
    }

    private int retrieveInteger(JSONObject volumeInfo, String name)
    {
        try {
            return volumeInfo.getInt(name);
        }
        catch (JSONException e) {
            return -1;
        }
    }

    private double retrieveDouble(JSONObject volumeInfo, String name)
    {
        try {
            return volumeInfo.getDouble(name);
        }
        catch (JSONException e) {
            return -1;
        }
    }

    private Uri retrieveImageUri(JSONObject volumeInfo, String name)
    {
        try {
            JSONObject imageLinks = volumeInfo.getJSONObject("imageLinks");
            return Uri.parse(imageLinks.getString(name));
        }
        catch (JSONException e) {
            return null;
        }
    }

    /**
     * Return a String containing all data read from a Reader
     */
    private String readAll(Reader rd) throws IOException
    {
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
    private JSONObject readJsonFromUrl(String url) throws IOException, JSONException
    {
        InputStream is = new URL(url).openStream();

        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
            String jsonText = readAll(rd);
            JSONObject json = new JSONObject(jsonText);
            return json;
        }
        finally {
            is.close();
        }
    }

    /**
     * Book class
     */
    class Book
    {
        private String isbn;
        private String title;
        private String subTitle;
        private String[] authors;
        private String publisher;
        private String publishedDate;
        private String description;
        private int pageCount;
        private String[] categories;
        private String language;
        private Uri thumbnail;
        private double averageRating;
        private int ratingsCount;

        public Book(String isbn, String title, String subTitle, String[] authors, String publisher,
                    String publishedDate, String description, int pageCount, String[] categories,
                    String language, Uri thumbnail, double averageRating, int ratingsCount)
        {
            this.isbn = isbn;
            this.title = title;
            this.subTitle = subTitle;
            this.authors = authors;
            this.publisher = publisher;
            this.publishedDate = publishedDate;
            this.description = description;
            this.pageCount = pageCount;
            this.categories = categories;
            this.language = language;
            this.thumbnail = thumbnail;
            this.averageRating = averageRating;
            this.ratingsCount = ratingsCount;
        }

        public String getIsbn()
        {
            return isbn;
        }

        public String getTitle()
        {
            return title;
        }

        public String getSubTitle()
        {
            return subTitle;
        }

        public String[] getAuthors()
        {
            return authors;
        }

        public String getPublisher()
        {
            return publisher;
        }

        public String getPublishedDate()
        {
            return publishedDate;
        }

        public String getDescription()
        {
            return description;
        }

        public int getPageCount()
        {
            return pageCount;
        }

        public String[] getCategories()
        {
            return categories;
        }

        public String getLanguage()
        {
            return language;
        }

        public Uri getThumbnail()
        {
            return thumbnail;
        }

        public double getAverageRating()
        {
            return averageRating;
        }

        public int getRatingsCount()
        {
            return ratingsCount;
        }
    }
}
