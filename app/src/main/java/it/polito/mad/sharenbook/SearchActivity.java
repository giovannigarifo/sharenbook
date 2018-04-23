package it.polito.mad.sharenbook;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.widget.Toast;

import com.algolia.search.saas.APIClient;
import com.algolia.search.saas.AlgoliaException;
import com.algolia.search.saas.Index;
import com.algolia.search.saas.Query;
import com.algolia.search.saas.listeners.SearchListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {


    CharSequence searchInputText;
    ArrayList<Book> searchResult;

    // Agolia
    APIClient apiClient;
    Index index;
    Query query;


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


        /*
         * RecylerView setup
         */
        searchResult = new ArrayList<>();

        RecyclerView rv = (RecyclerView) findViewById(R.id.search_rv_result);
        LinearLayoutManager llm = new LinearLayoutManager(SearchActivity.this);
        rv.setLayoutManager(llm);

        RVAdapter adapter = new RVAdapter(searchResult, getApplicationContext());
        rv.setAdapter(adapter);

        /*
         * Agolia setup
         */

        apiClient = new APIClient("4DWHVL57AK", "03391b3ea81e4a5c37651a677670bcb8"); // Init Algolia

        index = apiClient.initIndex("books");

        // Pre-build query.
        query = new Query();

        List<String> attributesToRetrieve = new ArrayList<>();
        attributesToRetrieve.add("title");
        attributesToRetrieve.add("subtitle");
        query.setAttributesToRetrieve(attributesToRetrieve);

        List<String> attributesToHighlight = new ArrayList<>();
        attributesToHighlight.add("title");
        query.setAttributesToHighlight(attributesToHighlight);

        query.setHitsPerPage(20);

        //set the text to be queried
        query.setQueryString(searchInputText.toString());

        //send the search query
        index.searchASync(query, new SearchListener() {

            @Override
            public void searchResult(Index index, Query query, JSONObject jsonResults) {

                Toast.makeText(SearchActivity.this, "Query result received!", Toast.LENGTH_SHORT).show();

                searchResult = parseResults(jsonResults); //parse the result obtained from Agolia
                adapter.notifyDataSetChanged();
            }

            @Override
            public void searchError(Index index, Query query, AlgoliaException e) {
                // TODO: Any error will be notified here.
                Toast.makeText(SearchActivity.this, "Query error received!", Toast.LENGTH_SHORT).show();
            }
        });


    }

    /*
     * JSON Parser: from JSON to Book
     */
    public Book BookJsonParser(JSONObject jsonObject) {

        if (jsonObject == null)
            return null;

        String title = jsonObject.optString("title");
        String subtitle = jsonObject.optString("subtitle");

        if (title != null && subtitle != null)
            return new Book("", title, subtitle, new ArrayList<>(), "", "", "", 0, new ArrayList<>(), "", "");
        return null;
    }


    public ArrayList<Book> parseResults(JSONObject jsonObject) {

        if (jsonObject == null)
            return null;

        ArrayList<Book> results = new ArrayList<>();

        JSONArray hits = jsonObject.optJSONArray("hits");

        if (hits == null)
            return null;

        for (int i = 0; i < hits.length(); ++i) {
            JSONObject hit = hits.optJSONObject(i);
            if (hit == null)
                continue;
            Book b = BookJsonParser(hit);
            if (b == null)
                continue;
            results.add(b);
        }

        return results;
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
}
