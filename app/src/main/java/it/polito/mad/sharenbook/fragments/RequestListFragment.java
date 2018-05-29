package it.polito.mad.sharenbook.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;

import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.UserInterface;
import it.polito.mad.sharenbook.utils.Utils;

public class RequestListFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        ArrayList<String> usernameList = getArguments().getStringArrayList("usernameList");
        long[] creationTimeArray = getArguments().getLongArray("creationTimeList");

        View rootView = inflater.inflate(R.layout.fragment_list_request, container, false);
        ListView requestListView = rootView.findViewById(R.id.list_view_requests);

        RequestsAdapter requestAdapter = new RequestsAdapter(getActivity(), usernameList, creationTimeArray);
        requestListView.setAdapter(requestAdapter);

        return rootView;
    }

    class RequestsAdapter extends BaseAdapter {

        private Activity mActivity;
        private ArrayList<String> mUsernameList;
        private long[] mRequestTimeArray;

        private DatabaseReference usernamesDb;
        private StorageReference imagesStorage;

        class ViewHolder {
            ImageView userImage;
            TextView usernameText;
            TextView requestTimeText;
            ImageView optionsButton;
        }

        RequestsAdapter(Activity activity, ArrayList<String> usernameList, long[] requestTimeArray) {
            mActivity = activity;
            mUsernameList = usernameList;
            mRequestTimeArray = requestTimeArray;
            usernamesDb = FirebaseDatabase.getInstance().getReference(getString(R.string.usernames_key));
            imagesStorage = FirebaseStorage.getInstance().getReference(getString(R.string.user_images_key));
        }

        @Override
        public int getCount() {
            return mUsernameList.size();
        }

        @Override
        public Object getItem(int position) {
            return mUsernameList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null) {

                String username = mUsernameList.get(position);
                long requestTime = mRequestTimeArray[position];

                ViewHolder holder = new ViewHolder();

                convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.request_list_item, parent, false);
                holder.userImage = convertView.findViewById(R.id.user_image);
                holder.usernameText = convertView.findViewById(R.id.username);
                holder.requestTimeText = convertView.findViewById(R.id.request_time);
                holder.optionsButton = convertView.findViewById(R.id.options_button);

                // Get user picSignature
                usernamesDb.child(username).child(getString(R.string.pic_signature_key)).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        if (dataSnapshot.exists()) {
                            long picSignature = (long) dataSnapshot.getValue();
                            UserInterface.showGlideImage(mActivity, imagesStorage.child(username), holder.userImage, picSignature);

                        } else {
                            GlideApp.with(mActivity).load(mActivity.getResources().getDrawable(R.drawable.ic_profile)).into(holder.userImage);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });

                // Bind item values
                holder.usernameText.setText(username);
                holder.requestTimeText.setText(Utils.convertTime(requestTime, "dd M, HH:mm"));
            }


            return  convertView;
        }
    }
}
