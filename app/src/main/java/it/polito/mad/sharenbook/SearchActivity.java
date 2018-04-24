package it.polito.mad.sharenbook;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.algolia.instantsearch.helpers.InstantSearch;
import com.algolia.instantsearch.helpers.Searcher;
//import com.algolia.search.saas.APIClient;
//import com.algolia.search.saas.AlgoliaException;
//import com.algolia.search.saas.Index;
//import com.algolia.search.saas.Query;
//import com.algolia.search.saas.listeners.SearchListener;

import com.algolia.instantsearch.*;
import com.algolia.instantsearch.model.AlgoliaErrorListener;
import com.algolia.instantsearch.model.AlgoliaResultsListener;
import com.algolia.instantsearch.model.SearchResults;
import com.algolia.instantsearch.ui.views.AlgoliaHitView;
import com.algolia.instantsearch.ui.views.Hits;
import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Query;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class SearchActivity extends AppCompatActivity {


    CharSequence searchInputText;
    ArrayList<Book> searchResult;

    // Algolia instant search
    Searcher searcher;


    /*
    // Agolia
    APIClient apiClient;
    Index index;
    Query query;
*/

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

        SearchBookAdapter sbAdapter = new SearchBookAdapter(this.searchResult, this.getContentResolver());
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

        if (title != null && subtitle != null)
            return new Book(isbn, title, subtitle, authors, publisher, publishedDate, description, pageCount, categories, language, thumbnail);
        return null;
    }


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
                books.add(BookJsonParser(jsonBooks.getJSONObject(key)));
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
    private ContentResolver mContentResolver;

    //constructor
    SearchBookAdapter(ArrayList<Book> searchResult, ContentResolver contentResolver) {
        this.searchResult = searchResult;
        this.mContentResolver = contentResolver;
    }

    //Inner Class that provides a reference to the views for each data item of the collection
    class SearchBookViewHolder extends RecyclerView.ViewHolder {

        private RelativeLayout item_search_result;

        SearchBookViewHolder(RelativeLayout rl) {
            super(rl);
            this.item_search_result = rl;
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
        RelativeLayout rl = (RelativeLayout) li.inflate(R.layout.item_search_result, parent, false);

        return new SearchBookViewHolder(rl);
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


        //title
        TextView title = holder.item_search_result.findViewById(R.id.item_searchresult_title);
        title.setText(this.searchResult.get(position).getTitle());

        //subtitle

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.searchResult.size();
    }

}

