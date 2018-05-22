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
import java.util.HashMap;
import java.util.List;

import it.polito.mad.sharenbook.R;

/**
 * Book Conditions Adapter
 */
public class MultipleCheckableCheckboxAdapter extends ArrayAdapter<String> {

    private ArrayList<String> selectedStrings;
    private List<String> checkedCheckboxes;

    //constructor
    public MultipleCheckableCheckboxAdapter(Context context, int textViewResourceId, String[] collection) {

        super(context, textViewResourceId, collection);
        selectedStrings = new ArrayList<>();
        checkedCheckboxes = new ArrayList<>();
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
            selectedStrings.add(getItem(position));
        }

        //checkbox listeners
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if (isChecked)
                selectedStrings.add(checkbox.getText().toString());
            else
                selectedStrings.remove(checkbox.getText().toString());
        });

        //set checkbox text
        checkbox.setText(getItem(position));

        return item_checkbox;
    }


    public ArrayList<String> getSelectedStrings() {
        return selectedStrings;
    }

    public void setCheckboxCheck(String cat){
        checkedCheckboxes.add(cat);
    }
}