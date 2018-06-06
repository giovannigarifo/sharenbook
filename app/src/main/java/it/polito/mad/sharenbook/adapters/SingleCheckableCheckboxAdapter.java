package it.polito.mad.sharenbook.adapters;

import android.content.Context;
import android.support.v7.widget.AppCompatCheckBox;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.lang.reflect.Array;
import java.util.ArrayList;

import it.polito.mad.sharenbook.R;

/**
 * Book Conditions Adapter
 */
public class SingleCheckableCheckboxAdapter extends ArrayAdapter<String> {

    private int selectedPosition = -1;// no selection by default

    //constructor
    public SingleCheckableCheckboxAdapter(Context context, int textViewResourceId, String[] collection) {

        super(context, textViewResourceId, collection);
    }

    @Override
    public View getView(int position, View item_checkbox, ViewGroup parent) {

        LayoutInflater inflater =
                (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        //retrieve the layout
        item_checkbox = inflater.inflate(R.layout.item_checkbox, null);

        //retrieve the view
        AppCompatCheckBox checkbox = item_checkbox.findViewById(R.id.checkbox);

        //set checked or not
        if(selectedPosition == position)
            checkbox.setChecked(true);
        else checkbox.setChecked(false);

        //checkbox listeners
        checkbox.setOnCheckedChangeListener((buttonView, isChecked) -> {

            if(isChecked)
            {
                selectedPosition = position;
            }
            else{
                selectedPosition = -1;
            }
            notifyDataSetChanged();
        });

        //set checkbox text
        checkbox.setText(getItem(position));

        return item_checkbox;
    }


    public int getSelectedPosition() {
        return selectedPosition;
    }

    public void setSelectedPosition(int sel){
        this.selectedPosition = sel;
    }


}