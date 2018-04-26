package it.polito.mad.sharenbook;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.algolia.instantsearch.helpers.InstantSearch;
import com.algolia.instantsearch.helpers.Searcher;

import com.algolia.search.saas.RequestOptions;
import com.bumptech.glide.Glide;

import it.polito.mad.sharenbook.Utils.GlideApp;
import it.polito.mad.sharenbook.Utils.MyAppGlideModule;

import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.module.AppGlideModule;

import com.bumptech.glide.Priority;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import it.polito.mad.sharenbook.model.Book;


public class SearchActivity extends AppCompatActivity {


    CharSequence searchInputText; //search query received in bundle
    ArrayList<Book> searchResult; //list of book that matched the query

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

        // Setup Searchbar


        //Algolia's InstantSearch setup
        searcher = Searcher.create("4DWHVL57AK", "03391b3ea81e4a5c37651a677670bcb8", "books");
        InstantSearch helper = new InstantSearch(searcher);

        searcher.registerResultListener((results, isLoadingMore) -> {

            if (results.nbHits > 0) {

                searchResult.clear();
                searchResult.addAll(parseResults(results.hits));
                sbAdapter.notifyDataSetChanged();

            } else {
                Toast.makeText(getApplicationContext(), R.string.sa_no_results, Toast.LENGTH_LONG).show();
            }
        });

        searcher.registerErrorListener((query, error) -> {

            Toast.makeText(getApplicationContext(), R.string.sa_no_results, Toast.LENGTH_LONG).show();
            Log.d("error", "Unable to retrieve search result from Algolia");
        });

        // Fire the search (async)
        helper.search(searchInputText == null ? "": searchInputText.toString());

    }

    /**
     * JSON Parser: from JSON to Book
     *
     * @param jsonObject : json representation of the book stored in algolia's "books" index
     * @return : the Book object
     */
    public Book BookJsonParser(JSONObject jsonObject) {

        String isbn = jsonObject.optString("isbn");
        String title = jsonObject.optString("title");
        String subtitle = jsonObject.optString("subtitle");

        //authors
        ArrayList<String> authors = new ArrayList<>();

        try {

            Object a = jsonObject.get("authors");

            if (a instanceof String) {

                String author = (String) a;
                author.replace("[", "");
                author.replace("]", "");
                authors.add(author);

            } else {

                JSONArray jsonCategories = jsonObject.getJSONArray("authors");
                for (int i = 0; i < jsonCategories.length(); i++)
                    authors.add(jsonCategories.optString(i));
            }

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

            Object c = jsonObject.get("categories");

            if (c instanceof String) {

                String category = (String) c;
                category.replace("[", "");
                category.replace("]", "");
                categories.add(category);

            } else {

                JSONArray jsonCategories = jsonObject.getJSONArray("categories");
                for (int i = 0; i < jsonCategories.length(); i++)
                    categories.add(jsonCategories.optString(i));
            }

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
     * @param hits : algolia's search hits
     * @return : the colleciton of books
     */
    public ArrayList<Book> parseResults(JSONArray hits) {

        ArrayList<Book> books = new ArrayList<>();

        for (int i = 0; i < hits.length(); i++) {

            try {

                JSONObject hit = hits.getJSONObject(i);

                Iterator<String> keyList = hit.keys();

                //first key (bookId): book object
                String bookId = keyList.next();
                Book b = BookJsonParser(hit.getJSONObject(bookId));
                b.setBookId(bookId); //save also the FireBase unique ID, is used to retrieve the photos from firebase storage
                books.add(b);

                //second key: objectId
                //third key: _highlightResult

            } catch (JSONException e) {
                Log.d("debug", "unable to retrieve search result from json hits");
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


    /**
     * Release resources when onDestroy event is catched
     */
    @Override
    protected void onDestroy() {
        searcher.destroy();
        super.onDestroy();
    }

    /**
     * Terminate activity if actionbar left arrow pressed
     */
    @Override
    public boolean onSupportNavigateUp() {

        finish();
        return true;
    }


    /**
     * onBackPressed method
     */
    @Override
    public void onBackPressed() {

        finish();
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

        //hide card
        holder.item_search_result.setVisibility(View.INVISIBLE);

        //photo
        ImageView photo = holder.item_search_result.findViewById(R.id.item_searchresult_photo);
        photo.setImageResource(R.drawable.book_photo_placeholder);

        int thumbnailOrFirstPhotoPosition = searchResult.get(position).getNumPhotos() - 1;
        String bookId = searchResult.get(position).getBookId();

        StorageReference thumbnailOrFirstPhotoRef = FirebaseStorage.getInstance().getReference().child("book_images/" + bookId + "/" + thumbnailOrFirstPhotoPosition + ".jpg");

        thumbnailOrFirstPhotoRef.getDownloadUrl().addOnSuccessListener((Uri uri) -> {

            String imageURL = uri.toString();

            GlideApp.with(context).load(imageURL)
                    .placeholder(R.drawable.book_photo_placeholder)
                    .error(R.drawable.book_photo_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade(500))
                    .into(photo);

        }).addOnFailureListener((exception) -> {
                    // handle errors here
                    Log.e("error", "Error while retrieving book photo/thumbnail from firebase storage.");
                }
        );

        //title
        TextView title = holder.item_search_result.findViewById(R.id.item_searchresult_title);
        title.setText(this.searchResult.get(position).getTitle());

        //subtitle
        TextView subtitle = holder.item_search_result.findViewById(R.id.item_searchresult_subtitle);
        subtitle.setText(this.searchResult.get(position).getSubtitle());

        holder.item_search_result.setVisibility(View.VISIBLE);
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.searchResult.size();
    }

}

