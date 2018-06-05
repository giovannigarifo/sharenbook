package it.polito.mad.sharenbook.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import java.util.Arrays;

import it.polito.mad.sharenbook.App;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.adapters.CategoriesAdapter;
import it.polito.mad.sharenbook.model.UserProfile;

public class ShowUserInfoFragment extends Fragment {

    private TextView tv_userCityContent, tv_userBioContent, tv_userEmailContent;
    private CategoriesAdapter categoriesAdapter;
    private it.polito.mad.sharenbook.views.ExpandableHeightGridView grid;
    private UserProfile user;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){

        user = getArguments().getParcelable("userData");

        View rootView = inflater.inflate(R.layout.fragment_show_user_info, container, false);

        getViews(rootView);

        tv_userCityContent.setText(user.getCity());
        tv_userBioContent.setText(user.getBio());
        tv_userEmailContent.setText(user.getEmail());

        categoriesAdapter = new CategoriesAdapter(App.getContext());
        grid.setAdapter(categoriesAdapter);
        grid.setExpanded(true);

        /* Add preferred categories */
        setPrefCategories();

        return rootView;
    }

    private void getViews(View view){
        tv_userCityContent = view.findViewById(R.id.tv_userCityContent);
        tv_userBioContent = view.findViewById(R.id.tv_userBioContent);
        tv_userEmailContent = view.findViewById(R.id.tv_userEmailContent);
        grid = view.findViewById(R.id.gridview);
    }

    private void setPrefCategories(){

        String[] bookCategories = getResources().getStringArray(R.array.book_categories);

        for(Integer cat : user.getCategories()) {
            categoriesAdapter.addCategory(Arrays.asList(bookCategories).get(cat));
        }

    }

}
