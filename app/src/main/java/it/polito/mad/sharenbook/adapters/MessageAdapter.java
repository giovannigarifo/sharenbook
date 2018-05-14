package it.polito.mad.sharenbook.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.app.ActivityCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.google.firebase.storage.StorageReference;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.model.Message;
import it.polito.mad.sharenbook.utils.UserInterface;

public class MessageAdapter extends BaseAdapter {

    private Context context;
    private List<Message> messages = new ArrayList<Message>();
    private StorageReference profilePicRef;

    public MessageAdapter(Context context, StorageReference profilePicRef){
        this.context = context;
        this.profilePicRef = profilePicRef;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        notifyDataSetChanged(); // to render the list we need to notify
    }

    @Override
    public int getCount() {
        return messages.size();
    }

    @Override
    public Object getItem(int position) {
        return messages.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MessageViewHolder holder = new MessageViewHolder();
        LayoutInflater messageInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        Message message = messages.get(position);

        if (message.isThisBelongToMe()) {
            convertView = messageInflater.inflate(R.layout.chat_my_message, null);
            holder.messageBody = (TextView) convertView.findViewById(R.id.chat_message_body);
            convertView.setTag(holder);
            holder.messageBody.setText(message.getMessage());
        } else {
            convertView = messageInflater.inflate(R.layout.chat_their_message, null);
            holder.avatar = convertView.findViewById(R.id.chat_avatar);
            holder.name = convertView.findViewById(R.id.chat_username);
            holder.messageBody =  convertView.findViewById(R.id.chat_message_body);
            convertView.setTag(holder);

            if(!message.isHide()) {
                holder.name.setText(message.getUsername());
                holder.messageBody.setText(message.getMessage());
                UserInterface.showGlideImage(context, profilePicRef, holder.avatar, 0);
            }
            else{
                holder.messageBody.setText(message.getMessage());
                holder.name.setVisibility(View.GONE);
                holder.avatar.setVisibility(View.INVISIBLE);
            }

        }

        return convertView;
    }

    public class MessageViewHolder{

        public CircularImageView avatar;
        public TextView name;
        public TextView messageBody;

    }
}
