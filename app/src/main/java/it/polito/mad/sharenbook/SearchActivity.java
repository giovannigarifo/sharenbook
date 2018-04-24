package it.polito.mad.sharenbook;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.algolia.instantsearch.helpers.InstantSearch;
import com.algolia.instantsearch.helpers.Searcher;

import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class SearchActivity extends AppCompatActivity {


    CharSequence searchInputText;
    ArrayList<Book> searchResult;

    // Algolia instant search
    Searcher searcher;

    /*
     * OnCreate
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        //retrieve book info
        if (savedInstanceState == null) {

            Bundle bundle = getIntent().getExtras();

            if (bundle == null)
                Log.d("debug", "[SearchActivity] no search input text received from calling Activity");
            else
                searchInputText = bundle.getCharSequence("searchInputText"); //retrieve text from intent

        } else
            searchInputText = savedInstanceState.getCharSequence("searchInputText"); //retrieve text info from saveInstanceState


        // RecylerView setup
        searchResult = new ArrayList<>();

        RecyclerView search_rv_result = (RecyclerView) findViewById(R.id.search_rv_result);
        LinearLayoutManager llm = new LinearLayoutManager(SearchActivity.this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        search_rv_result.setLayoutManager(llm);

        SearchBookAdapter sbAdapter = new SearchBookAdapter(this.searchResult, getApplicationContext());
        search_rv_result.setAdapter(sbAdapter);


        //Algolia's InstantSearch
        searcher = Searcher.create("4DWHVL57AK", "03391b3ea81e4a5c37651a677670bcb8", "books");
        InstantSearch helper = new InstantSearch(searcher);

        searcher.registerResultListener((results, isLoadingMore) -> {

            searchResult.clear();
            searchResult.addAll(parseResults(results.hits));
            sbAdapter.notifyDataSetChanged();
        });

        searcher.registerErrorListener((query, error) -> {

            Log.d("error", "Unable to retrieve search result from Algolia");
        });

        // Fire the search (async)
        helper.search(searchInputText.toString());

    }

    /*
     * JSON Parser: from JSON to Book
     */
    public Book BookJsonParser(JSONObject jsonObject) {

        String isbn = jsonObject.optString("isbn");
        String title = jsonObject.optString("title");
        String subtitle = jsonObject.optString("subtitle");

        //authors
        ArrayList<String> authors = new ArrayList<>();

        try {

            JSONArray jsonAuthors = jsonObject.getJSONArray("authors");
            for (int i = 0; i < jsonAuthors.length(); i++)
                authors.add(jsonAuthors.optString(i));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String publisher = jsonObject.optString("publisher");
        String publishedDate = jsonObject.optString("publishedDate");
        String description = jsonObject.optString("description");
        int pageCount = jsonObject.optInt("pageCount");

        //categories
        ArrayList<String> categories = new ArrayList<>();

        try {

            JSONArray jsonCategories = jsonObject.getJSONArray("categories");
            for (int i = 0; i < jsonCategories.length(); i++)
                categories.add(jsonCategories.optString(i));

        } catch (JSONException e) {
            e.printStackTrace();
        }

        String language = jsonObject.optString("language");
        String thumbnail = jsonObject.optString("thumbnail");
        int numPhotos = jsonObject.optInt("numPhotos");

        return new Book(isbn, title, subtitle, authors, publisher, publishedDate, description, pageCount, categories, language, thumbnail, numPhotos);
    }


    /**
     * Parser for the result of the search that returns an ArrayList of books that matched the query
     *
     * @param hits : the book hitted by the query
     * @return : ArrayList of books
     */
    public ArrayList<Book> parseResults(JSONArray hits) {

        if (hits == null)
            return null;

        ArrayList<Book> books = new ArrayList<>();

        JSONObject hit = hits.optJSONObject(0); //retrieve first hit
        JSONObject jsonBooks = hit.optJSONObject("books"); //retrieve book array

        Iterator<String> keyList = jsonBooks.keys();

        while (keyList.hasNext()) {

            String key = keyList.next();

            try {

                //save the book into the collection
                Book b = BookJsonParser(jsonBooks.getJSONObject(key));
                b.setBookId(key); //save also the FireBase unique ID
                books.add(b);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        return books;
    }


    /**
     * Saves the state of the activity when dealing with system wide events (e.g. rotation)
     *
     * @param outState : the bundle object that contains all the serialized information to be saved
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {

        super.onSaveInstanceState(outState);

        outState.putCharSequence("searchInputText", searchInputText);
    }


    @Override
    protected void onDestroy() {
        searcher.destroy();
        super.onDestroy();
    }

}


/**
 * SearchBookAdapter class
 */

class SearchBookAdapter extends RecyclerView.Adapter<SearchBookAdapter.SearchBookViewHolder> {

    private List<Book> searchResult;
    private Context context;

    //constructor
    SearchBookAdapter(ArrayList<Book> searchResult, Context context) {
        this.searchResult = searchResult;
        this.context = context;
    }

    //Inner Class that provides a reference to the views for each data item of the collection
    class SearchBookViewHolder extends RecyclerView.ViewHolder {

        private LinearLayout item_search_result;

        SearchBookViewHolder(LinearLayout ll) {
            super(ll);
            this.item_search_result = ll;
        }
    }

    /**
     * Create new ViewHolder objects (invoked by the layout manager) and set the view to use to
     * display it's content
     *
     * @param parent   :
     * @param viewType :
     * @return BookPhotoViewHolder :
     */
    @NonNull
    @Override
    public SearchBookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater li = LayoutInflater.from(parent.getContext());
        LinearLayout ll = (LinearLayout) li.inflate(R.layout.item_search_result, parent, false);

        return new SearchBookViewHolder(ll);
    }

    /**
     * Replace the contents of a ViewHolder (invoked by the layout manager)
     *
     * @param holder   :
     * @param position :
     */
    @Override
    public void onBindViewHolder(@NonNull SearchBookViewHolder holder, int position) {

        //photo
        ImageView photo = holder.item_search_result.findViewById(R.id.item_searchresult_photo);

        int thumbnailOrFirstPhotoPosition = searchResult.get(position).getNumPhotos() - 1;
        String bookId = searchResult.get(position).getBookId();

        StorageReference thumbnailOrFirstPhotoRef = FirebaseStorage.getInstance().getReference().child("book_images/" + bookId + "/" + thumbnailOrFirstPhotoPosition + ".jpg");

        thumbnailOrFirstPhotoRef.getDownloadUrl().addOnSuccessListener((uri) -> {
            String imageURL = uri.toString();
            Glide.with(context).load(imageURL).into(photo);

        }).addOnFailureListener((exception) -> {
                    // handle errors here
                }
        );

        //title
        TextView title = holder.item_search_result.findViewById(R.id.item_searchresult_title);
        title.setText(this.searchResult.get(position).getTitle());

        //subtitle
        TextView subtitle = holder.item_search_result.findViewById(R.id.item_searchresult_subtitle);
        subtitle.setText(this.searchResult.get(position).getSubtitle());
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.searchResult.size();
    }

}

