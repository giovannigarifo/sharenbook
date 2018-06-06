package it.polito.mad.sharenbook.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.constraint.ConstraintLayout;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.App;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.utils.UserInterface;


public class CategoriesAdapter extends BaseAdapter {

    private Context context;
    private List<String> categories = new ArrayList<>();

    public CategoriesAdapter(Context context){
        this.context = context;
    }

    public void addCategory(String cat) {
        this.categories.add(cat);
        notifyDataSetChanged();
    }

    public void clearCategories(){
        this.categories.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return categories.size();
    }

    @Override
    public Object getItem(int i) {
        return categories.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View view, ViewGroup viewGroup) {

        CategoriesAdapter.CategoriesViewHolder holder = new CategoriesViewHolder();
        LayoutInflater categoryInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        String category =  categories.get(position);

        view = categoryInflater.inflate(R.layout.custom_chiptag, null);

        holder.chiptag_shape = view.findViewById(R.id.chiptag);
        holder.chiptag_shape.setBackground(App.getContext().getResources().getDrawable(R.drawable.button_bg_transparent));

        holder.avatar = view.findViewById(R.id.img);
        holder.avatar.setVisibility(View.GONE);

        holder.name = view.findViewById(R.id.text_user);
        holder.name.setText(category);

        holder.popup_menu = view.findViewById(R.id.img_popup_menu);
        holder.popup_menu.setVisibility(View.GONE);

        view.setTag(holder);

        return view;
    }

    public class CategoriesViewHolder{

        ConstraintLayout chiptag_shape;
        ImageView avatar;
        TextView name;
        ImageView popup_menu;

    }

}
