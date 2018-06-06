package it.polito.mad.sharenbook.adapters;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import it.polito.mad.sharenbook.R;

/**
 * Book Conditions Adapter
 */
public class MultipleCheckableCheckboxAdapter extends ArrayAdapter<String> {

    private Context context;
    private ArrayList<String> selectedStrings;
    private ArrayList<String> checkedCheckboxes;

    //constructor
    public MultipleCheckableCheckboxAdapter(Context context, int textViewResourceId, String[] collection) {

        super(context, textViewResourceId, collection);
        this.context = context;
        this.selectedStrings = new ArrayList<>();
        this.checkedCheckboxes = new ArrayList<>();
    }

    /**
     *
     * @param context:
     * @param textViewResourceId:
     * @param collection : the collection of String to display as checkboxes
     * @param alreadyChecked : the collection of the already selected string if it's necessary to show a previous state
     */
    public MultipleCheckableCheckboxAdapter(Context context, int textViewResourceId, String[] collection, ArrayList<String> alreadyChecked) {

        super(context, textViewResourceId, collection);
        this.selectedStrings = new ArrayList<>();
        this.checkedCheckboxes = new ArrayList<>();
        this.checkedCheckboxes.addAll(alreadyChecked); //add all the already checked checkboxes strings
    }

    @Override
    public View getView(int position, View item_checkbox, ViewGroup parent) {

        LayoutInflater inflater =
                (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //inflate the layout
        item_checkbox = inflater.inflate(R.layout.item_checkbox, parent, false);

        //retrieve the view
        AppCompatCheckBox checkbox = item_checkbox.findViewById(R.id.checkbox);

        if(checkedCheckboxes.contains(getItem(position))){
            checkbox.setChecked(true);
            if(!selectedStrings.contains(getItem(position)))
                selectedStrings.add(getItem(position));
        }

        //checkbox listeners
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked && !selectedStrings.contains(checkbox.getText().toString())){
                selectedStrings.add(checkbox.getText().toString());
            }
            else{
                selectedStrings.remove(checkbox.getText().toString());
            }

        });

        //set checkbox text
        checkbox.setText(getItem(position));

        return item_checkbox;
    }


    public ArrayList<String> getSelectedStrings() {
        return selectedStrings;
    }

    public ArrayList<String> getCheckedItems() {
        return checkedCheckboxes;
    }

    public void clearSelectedStrings(){
        this.selectedStrings.clear();
        this.checkedCheckboxes.clear();
        this.notifyDataSetChanged();
    }

    public void setAlreadyCheckedCheckboxes(List<Integer> alreadyChecked){

        String[] book_categories = context.getResources().getStringArray(R.array.book_categories);

        for( int i=0; i < alreadyChecked.size(); i++){
            this.checkedCheckboxes.add(book_categories[alreadyChecked.get(i)]);
        }
    }

    public void setCheckboxCheck(String cat){
        this.checkedCheckboxes.add(cat);
    }
}