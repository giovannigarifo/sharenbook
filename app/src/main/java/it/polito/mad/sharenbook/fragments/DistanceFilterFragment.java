package it.polito.mad.sharenbook.fragments;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import com.robertlevonyan.views.chip.Chip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import it.polito.mad.sharenbook.MapsActivity;
import it.polito.mad.sharenbook.R;


public class DistanceFilterFragment extends DialogFragment {

    private EditText mEditText;
    private SeekBar seekBar;
    private TextView tv_range;
    private Button btn_confirm, btn_undo;

    private List<Address> place = new ArrayList<>();

    public DistanceFilterFragment() {
        // Empty constructor is required for DialogFragment
        // Make sure not to add arguments to the constructor
        // Use `newInstance` instead as shown below
    }

    public static DistanceFilterFragment newInstance(String title) {
        DistanceFilterFragment frag = new DistanceFilterFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_distance_filter, container);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getViews(view);
        setSeekBar();
        setButtonListeners();

        // Fetch arguments from bundle and set title
        String title = getArguments().getString("title", "Enter Name");
        getDialog().setTitle(title);
        // Show soft keyboard automatically and request focus to field
        getDialog().getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }


    private void getViews(View view) {
        mEditText = view.findViewById(R.id.et_address);
        seekBar = view.findViewById(R.id.seekBar);
        tv_range = view.findViewById(R.id.tv_range);
        btn_confirm = view.findViewById(R.id.confirm_button);
        btn_undo = view.findViewById(R.id.undo_button);
    }

    private void setSeekBar(){

        String rangeString = getResources().getString(R.string.filter_distance_range, seekBar.getProgress());
        tv_range.setText(rangeString);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                progress = i;
                tv_range.setText(getResources().getString(R.string.filter_distance_range, progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                tv_range.setText(getResources().getString(R.string.filter_distance_range, progress));
            }
        });
    }

    private void setButtonListeners(){

        btn_undo.setOnClickListener(view -> {
            getDialog().dismiss();
        });

        btn_confirm.setOnClickListener(view -> {

            Boolean error = false;
            int range = seekBar.getProgress();

            if(range == 0){  //for API retro compatibility
                range = 1;
            }

            //validate inputs
            if(mEditText.getText().toString().isEmpty()){
                mEditText.setError(getString(R.string.address_empty));
            } else {
                String location = mEditText.getText().toString();
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                try {
                    place.clear();
                    place.addAll(geocoder.getFromLocationName(location, 1));
                    if (place.size() == 0) {
                        error = true;
                    }
                } catch (IOException e) { //if it was not possible to recognize location
                    error = true;
                }

                if(error){

                    mEditText.setError(getString(R.string.unknown_place));

                } else {
                    getDialog().dismiss();

                    Activity activity = getActivity();
                    if(activity instanceof MapsActivity){
                        MapsActivity mapsActivity = (MapsActivity) activity;
                        mapsActivity.filterByDistance(place, range);
                    }

                }
            }

        });

    }

}