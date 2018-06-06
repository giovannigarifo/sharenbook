package it.polito.mad.sharenbook.fragments;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.SearchActivity;
import it.polito.mad.sharenbook.adapters.MultipleCheckableCheckboxAdapter;
import it.polito.mad.sharenbook.adapters.SingleCheckableCheckboxAdapter;
import it.polito.mad.sharenbook.views.ExpandableHeightGridView;


public class SearchFilterFragment extends AppCompatDialogFragment {

    // parent activity
    private SearchActivity searchActivity;

    //scrollview
    private ScrollView fragment_sf_scrollview;

    //address
    private EditText fragment_sf_et_address;
    private SeekBar fragment_sf_sb_range;
    private TextView fragment_sf_tv_range;

    //conditions and categories
    private ExpandableHeightGridView fragment_sf_ehgv_conditions, fragment_sf_ehgv_categories;
    private MultipleCheckableCheckboxAdapter conditionAdapter;
    private MultipleCheckableCheckboxAdapter categoryAdapter;

    //author
    private EditText fragment_sf_et_author;

    //tags
    private EditText fragment_sf_et_tags;

    //buttons
    private Button btn_confirm, btn_undo, btn_clear;

    //number of setted filters
    private int filtersCounter;


    /**
     * Empty constructor is required for DialogFragment
     * Make sure not to add arguments to the constructor
     * Use `newInstance` instead as shown below
     */
    public SearchFilterFragment() {
    }

    public static SearchFilterFragment newInstance(String title) {

        SearchFilterFragment frag = new SearchFilterFragment();
        frag.setRetainInstance(true); //save dialog state when rotating
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_search_filter, container);

        String title = null;

        if (getArguments() != null)
            title = getArguments().getString("title", "Enter Name");

        getDialog().setTitle(title);

        //get the parent activity
        searchActivity = (SearchActivity) getActivity();

        return v;
    }

    @Override
    public void onDestroyView() {
        if (getDialog() != null && getRetainInstance())
            getDialog().setOnDismissListener(null);
        super.onDestroyView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putStringArrayList("selectedConditions", conditionAdapter.getSelectedStrings());
        outState.putStringArrayList("selectedCategories", categoryAdapter.getSelectedStrings());
        outState.putString("address", fragment_sf_et_address.getText().toString());
        outState.putInt("range", fragment_sf_sb_range.getProgress());
        outState.putString("author", fragment_sf_et_author.getText().toString());
        outState.putString("tags", fragment_sf_et_tags.getText().toString());
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        super.onViewCreated(view, savedInstanceState);

        getViews(view);
        setSeekBar();
        setButtonListeners();

        // Show soft keyboard automatically and request focus to field
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        //collections of conditions and categories
        String[] book_conditions = getResources().getStringArray(R.array.book_conditions);
        String[] book_categories = getResources().getStringArray(R.array.book_categories);

        //load filter state
        if (savedInstanceState != null) {

            //load temporary selection if bundle contains data, e.g. rotation happened
            conditionAdapter = new MultipleCheckableCheckboxAdapter(this.getContext(), R.layout.item_checkbox, book_conditions,
                    savedInstanceState.getStringArrayList("selectedConditions"));

            categoryAdapter = new MultipleCheckableCheckboxAdapter(this.getContext(), R.layout.item_checkbox, book_categories,
                    savedInstanceState.getStringArrayList("selectedCategories"));

            fragment_sf_et_address.setText(savedInstanceState.getString("address"));
            fragment_sf_sb_range.setProgress(savedInstanceState.getInt("range"));
            fragment_sf_et_author.setText(savedInstanceState.getString("author"));
            fragment_sf_et_tags.setText(savedInstanceState.getString("tags"));

        } else if (this.searchActivity.filtersStatesArePresent()) {

            //load previously selected filters if they are present, e.g. previous filters setted by user and saved in SearchActivity
            conditionAdapter = new MultipleCheckableCheckboxAdapter(this.getContext(), R.layout.item_checkbox, book_conditions,
                    this.searchActivity.filterState_selectedConditions);

            categoryAdapter = new MultipleCheckableCheckboxAdapter(this.getContext(), R.layout.item_checkbox, book_categories,
                    this.searchActivity.filterState_selectedCategories);

            fragment_sf_et_address.setText(this.searchActivity.filterState_location);
            fragment_sf_sb_range.setProgress(this.searchActivity.filterState_range);
            fragment_sf_et_author.setText(this.searchActivity.filterState_author);
            fragment_sf_et_tags.setText(this.searchActivity.filterState_tags);

        } else {

            conditionAdapter = new MultipleCheckableCheckboxAdapter(this.getContext(), R.layout.item_checkbox, book_conditions);
            categoryAdapter = new MultipleCheckableCheckboxAdapter(this.getContext(), R.layout.item_checkbox, book_categories);
        }

        //set conditions adapter for Book Condition expandable height grid view
        fragment_sf_ehgv_conditions.setAdapter(conditionAdapter);
        fragment_sf_ehgv_conditions.setNumColumns(2);
        fragment_sf_ehgv_conditions.setExpanded(true);

        //set categories adapter for Book categories expandable height grid view
        fragment_sf_ehgv_categories.setAdapter(categoryAdapter);
        fragment_sf_ehgv_categories.setNumColumns(2);
        fragment_sf_ehgv_categories.setExpanded(true);
    }


    private void getViews(View view) {

        fragment_sf_scrollview = view.findViewById(R.id.fragment_sf_scrollview);

        fragment_sf_et_address = view.findViewById(R.id.fragment_sf_et_address);
        fragment_sf_sb_range = view.findViewById(R.id.fragment_sf_sb_range);
        fragment_sf_tv_range = view.findViewById(R.id.fragment_sf_tv_range);

        //gridviews
        fragment_sf_ehgv_conditions = view.findViewById(R.id.fragment_sf_ehgv_conditions);
        fragment_sf_ehgv_categories = view.findViewById(R.id.fragment_sf_ehgv_categories);

        //edittexts
        fragment_sf_et_author = view.findViewById(R.id.fragment_sf_et_author);
        fragment_sf_et_tags = view.findViewById(R.id.fragment_sf_et_tags);

        btn_confirm = view.findViewById(R.id.confirm_button);
        btn_undo = view.findViewById(R.id.undo_button);
        btn_clear = view.findViewById(R.id.clear_filters_button);
    }


    private void setSeekBar() {

        String rangeString = getResources().getString(R.string.filter_distance_range, fragment_sf_sb_range.getProgress());

        fragment_sf_tv_range.setText(rangeString);

        fragment_sf_sb_range.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                progress = i;
                fragment_sf_tv_range.setText(getResources().getString(R.string.filter_distance_range, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                fragment_sf_tv_range.setText(getResources().getString(R.string.filter_distance_range, progress));
            }
        });
    }

    /**
     * Set listeners for the dialog buttons
     */
    private void setButtonListeners() {

        btn_undo.setOnClickListener(view -> getDialog().dismiss());

        btn_clear.setOnClickListener(view -> {

            //clear all inserted input
            fragment_sf_et_address.setText("");
            fragment_sf_sb_range.setProgress(5);
            conditionAdapter.clearSelectedStrings();
            categoryAdapter.clearSelectedStrings();
            fragment_sf_et_author.setText("");
            fragment_sf_et_tags.setText("");

            //clear the filters state saved in the search activity
            this.searchActivity.clearFiltersState();

            //remove slected filter counter from the FILTERS button
            this.searchActivity.clearFilterCounterInFilterButton();

            //remove focus from edittexts
            fragment_sf_et_address.clearFocus();
            fragment_sf_et_author.clearFocus();
            fragment_sf_et_tags.clearFocus();

            //clear actual searchresult
            this.searchActivity.clearCurrentSearchResult();

            //if the user cleared the filters but there is an input text inserted, search for it without filters
            if (searchActivity.searchInputText != null)
                if (searchActivity.searchInputText != "")
                    this.searchActivity.onSearchConfirmed(searchActivity.searchInputText.toString());


            //close dialog
            getDialog().dismiss();
        });

        /*
         * retrieve user input, create the filter string and pass it back to the SearchActivity
         */
        btn_confirm.setOnClickListener(view -> {

            //initially no filters setted
            filtersCounter = 0;

            //create conditions filter
            String conditionFilter = "";
            ArrayList<String> selectedConditions = conditionAdapter.getSelectedStrings(); //get the condition selected by the user

            if (selectedConditions.size() > 0) {

                filtersCounter++;

                String[] bookConditions = getResources().getStringArray(R.array.book_conditions); //retrieve the array of all available conditions

                for (int i = 0; i < selectedConditions.size(); i++) {

                    Integer conditionIndex = Arrays.asList(bookConditions).indexOf(selectedConditions.get(i)); //retrieve index of the condition

                    if (i == 0)
                        conditionFilter = "bookData.bookConditions=" + conditionIndex.toString();
                    else
                        conditionFilter = conditionFilter + " OR " + "bookData.bookConditions=" + conditionIndex.toString();
                }

                if (selectedConditions.size() > 1)
                    conditionFilter = "(" + conditionFilter + ")";
            }

            //create categories filter
            String categoryFilter = "";
            ArrayList<String> selectedCategories = categoryAdapter.getSelectedStrings();

            if (selectedCategories.size() > 0) {

                filtersCounter++;

                String[] bookCategories = getResources().getStringArray(R.array.book_categories); //retrieve the array of all available conditions

                for (int i = 0; i < selectedCategories.size(); i++) {

                    Integer categoryIndex = Arrays.asList(bookCategories).indexOf(selectedCategories.get(i)); //retrieve index of the condition

                    if (i == 0)
                        categoryFilter = "bookData.categories=" + categoryIndex.toString();
                    else
                        categoryFilter = categoryFilter + " OR " + "bookData.categories=" + categoryIndex.toString();
                }

                if (selectedCategories.size() > 1)
                    categoryFilter = "(" + categoryFilter + ")";
            }

            //create tags filter
            String tagsFilter = "";

            if (fragment_sf_et_tags.getText().toString().length() > 0) {

                filtersCounter++;

                String[] tags = fragment_sf_et_tags.getText().toString().split("\\s+");
                List<String> splittedTags = Arrays.asList(tags);

                for (int i = 0; i < splittedTags.size(); i++) {

                    if (i == 0)
                        tagsFilter = "bookData.tags:'" + splittedTags.get(i).trim().replaceAll("\'\"", "") + "'";
                    else
                        tagsFilter = tagsFilter + " OR " + "bookData.tags:'" + splittedTags.get(i).trim().replaceAll("\'\"", "") + "'";
                }

                if (splittedTags.size() > 1)
                    tagsFilter = "(" + tagsFilter + ")";
            }

            //create author filter
            String authorFilter = "";
            if (fragment_sf_et_author.getText().toString().length() > 0) {

                filtersCounter++;

                authorFilter = "bookData.authors:'" + fragment_sf_et_author.getText().toString().trim().replaceAll("\'\"", "") + "'";
            }


            //aggregate filters
            //"(" + conditionFilter + ") AND (" + categoryFilter + ") AND (" + tagsFilter + ") AND " + authorFilter;
            String aggregatedFilters = "";

            if (conditionFilter.length() > 0)
                aggregatedFilters = conditionFilter;

            if (categoryFilter.length() > 0 && aggregatedFilters.length() > 0)
                aggregatedFilters = aggregatedFilters + " AND " + categoryFilter;
            else if (categoryFilter.length() > 0 && aggregatedFilters.length() == 0)
                aggregatedFilters = categoryFilter;

            if (tagsFilter.length() > 0 && aggregatedFilters.length() > 0)
                aggregatedFilters = aggregatedFilters + " AND " + tagsFilter;
            else if (tagsFilter.length() > 0 && aggregatedFilters.length() == 0)
                aggregatedFilters = tagsFilter;

            if (authorFilter.length() > 0 && aggregatedFilters.length() > 0)
                aggregatedFilters = aggregatedFilters + " AND " + authorFilter;
            else if (authorFilter.length() > 0 && aggregatedFilters.length() == 0)
                aggregatedFilters = authorFilter;

            //location filter, then fire the search
            Boolean location_error = false;
            int range = fragment_sf_sb_range.getProgress();
            List<Address> place = new ArrayList<>();

            //for API retro compatibility
            if (range == 0)
                range = 1;

            if (!fragment_sf_et_address.getText().toString().isEmpty()) {

                filtersCounter++;

                String location = fragment_sf_et_address.getText().toString();
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());

                try {

                    place.clear();
                    place.addAll(geocoder.getFromLocationName(location, 1));
                    if (place.size() == 0)
                        location_error = true;

                } catch (IOException e) { //if it was not possible to recognize location
                    location_error = true;
                }

                if (location_error)
                    fragment_sf_et_address.setError(getString(R.string.unknown_place));
                else {
                    //the user inserted a valid location filter
                    getDialog().dismiss();
                    confirmSearchAndSaveFiltersState(aggregatedFilters, place.get(0), range);
                }

            } else {
                //the user don't want to use location as filter
                if (!location_error) {
                    getDialog().dismiss();
                    confirmSearchAndSaveFiltersState(aggregatedFilters, null, -1);
                }
            }

            //save filters state
            this.searchActivity.setFiltersState(selectedConditions, selectedCategories,
                    fragment_sf_et_tags.getText().toString(), fragment_sf_et_author.getText().toString(),
                    fragment_sf_sb_range.getProgress(), fragment_sf_et_address.getText().toString());
        });

    }

    public void confirmSearchAndSaveFiltersState(String aggregatedFilters, Address place, int range) {

        this.searchActivity.setSearchFilters(aggregatedFilters);
        this.searchActivity.setFilterPlace(place); //can be null if no place selected
        this.searchActivity.setFilterRange(range); //can be -1 if no range selected

        //add selected filters counter to the FILTERS button
        this.searchActivity.showFilterCounterInFilterButton(filtersCounter);

        //fire search
        this.searchActivity.onSearchConfirmed(searchActivity.searchInputText == null ? "" : searchActivity.searchInputText.toString());
    }


    /**
     * Set width and layout of the dialog to match parent
     */
    public void onResume() {
        super.onResume();

        try {
            getDialog().getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        } catch (java.lang.NullPointerException e) {
            e.printStackTrace();
        }
    }


}