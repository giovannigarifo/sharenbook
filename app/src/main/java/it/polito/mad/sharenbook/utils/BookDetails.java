package it.polito.mad.sharenbook.utils;

import android.app.Activity;
import android.net.Uri;

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

import it.polito.mad.sharenbook.model.Book;

public class BookDetails {

    private final String GOOGLE_BOOK_WS = "https://www.googleapis.com/books/v1/volumes?q=isbn:";

    private Activity mActivity;
    private String isbnNumber;
    private JSONObject jsonBook;
    private int totalItems;
    private ArrayList<Book> bookList;

    /**
     * Retrieve book details from GoogleApi Books WS
     *
     * @param isbnNumber
     */
    public BookDetails(Activity activity, String isbnNumber) {

        this.mActivity = activity;
        this.isbnNumber = isbnNumber;
        bookList = new ArrayList<>();

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
            String language = retrieveString(volumeInfo, "language");
            String thumbnail = retrieveImageLink(volumeInfo, "thumbnail");

            // Create a new Book object
            Book newBook = new Book(isbn, title, subTitle, authors, publisher, publishedDate, description,
                    pageCount, language, thumbnail);

            // Download thumbnail if present
            if (!thumbnail.equals("")) {
                try {
                    Uri tnUri = ImageUtils.downloadImageToStorage(mActivity, thumbnail, ImageUtils.EXTERNAL_PICTURES);
                    Uri stretchedTnUri = ImageUtils.stretchJpegPhoto(mActivity, ImageUtils.EXTERNAL_CACHE, tnUri, ImageUtils.ASPECT_RATIO_PHOTO_PORT);
                    newBook.addBookPhotoUri(stretchedTnUri);
                    newBook.setThumbnail(stretchedTnUri.toString());

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Add newbook to book list
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


