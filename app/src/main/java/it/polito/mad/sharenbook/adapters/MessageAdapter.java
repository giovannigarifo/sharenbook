package it.polito.mad.sharenbook.adapters;

import android.app.Activity;
import android.content.Context;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.model.Message;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.UserInterface;

public class MessageAdapter extends BaseAdapter {

    private Context context;
    private List<Message> messages = new ArrayList<>();
    private StorageReference profilePicRef;
    private long picSignature = 0;

    public MessageAdapter(Context context, StorageReference profilePicRef){
        this.context = context;
        this.profilePicRef = profilePicRef;
    }

    public void addMessage(Message message) {
        this.messages.add(message);
        notifyDataSetChanged(); // to render the list we need to notify
    }

    public void clearMessages(){
        this.messages.clear();
        notifyDataSetChanged();
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
        String messageBody = message.getMessage();

        if(messageBody == null){    //show unread messages
            convertView = messageInflater.inflate(R.layout.chat_unread_messages, null);
        }
        else if (message.isThisBelongToMe()) {
            convertView = messageInflater.inflate(R.layout.chat_my_message, null);
            holder.messageBody = convertView.findViewById(R.id.chat_message_body);
            holder.timestamp = convertView.findViewById(R.id.timestamp);
            holder.timestamp.setText(message.getHour(message.getTimestamp()));
            holder.messageBody.setText(messageBody);
            convertView.setTag(holder);

            if(!message.isHide())
                holder.messageBody.setBackgroundResource(R.drawable.chat_my_message_shape2);

        } else {
            convertView = messageInflater.inflate(R.layout.chat_their_message, null);
            holder.avatar = convertView.findViewById(R.id.chat_avatar);
            holder.name = convertView.findViewById(R.id.chat_username);
            holder.messageBody =  convertView.findViewById(R.id.chat_message_body);
            holder.timestamp = convertView.findViewById(R.id.timestamp);
            holder.timestamp.setText(message.getHour(message.getTimestamp()));

            convertView.setTag(holder);

            if(!message.isHide()) {
                holder.name.setText(message.getUsername());
                holder.messageBody.setText(messageBody);

                if(picSignature != 0)
                    UserInterface.showGlideImage(context, profilePicRef, holder.avatar, picSignature);
                else
                    GlideApp.with(context).load(context.getResources().getDrawable(R.drawable.ic_profile)).into(holder.avatar);
            }
            else{
                holder.messageBody.setText(messageBody);
                holder.messageBody.setBackgroundResource(R.drawable.chat_their_message_shape2);
                holder.avatar.setBackgroundResource(R.drawable.chat_avatar_shape);
                holder.name.setVisibility(View.GONE);
            }

        }

        return convertView;
    }

    public class MessageViewHolder{

        ImageView avatar;
        TextView name;
        TextView messageBody;
        TextView timestamp;

    }

    public long getPicSignature() {
        return picSignature;
    }

    public void setPicSignature(long picSignature) {
        this.picSignature = picSignature;
        notifyDataSetChanged();
    }

}
