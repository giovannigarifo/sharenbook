package it.polito.mad.sharenbook.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.ArrayList;
import java.util.List;

import it.polito.mad.sharenbook.ChatActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.model.Conversation;
import it.polito.mad.sharenbook.utils.UserInterface;

public class ConversationAdapter extends BaseAdapter {

    private List<Conversation> conversations = new ArrayList<>();
    private Context context;


    public ConversationAdapter(Context context){
        this.context = context;
    }

    public void addConversation(Conversation conversation) {
        this.conversations.add(conversation);
        notifyDataSetChanged(); // to render the list we need to notify
    }

    @Override
    public int getCount() {
        return conversations.size();
    }

    @Override
    public Object getItem(int position) {
        return conversations.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ConversationViewHolder holder = new ConversationViewHolder();
        Conversation conversation = conversations.get(position);
        if(convertView == null) {
            LayoutInflater conversationInflater = (LayoutInflater) context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = conversationInflater.inflate(R.layout.my_chats_item_layout,parent,false);
        }
        holder.username = convertView.findViewById(R.id.mychats_username);
        holder.lastMessageBody = convertView.findViewById(R.id.mychats_message_body);
        holder.date = convertView.findViewById(R.id.mychats_date);
        holder.inboxCounter = convertView.findViewById(R.id.mychats_inboxCounter);
        holder.avatar = convertView.findViewById(R.id.mychats_avatar);
        holder.conversation = convertView.findViewById(R.id.conversation);

        UserInterface.showGlideImage(context, conversation.getProfilePicRef(), holder.avatar, 0);
        holder.username.setText(conversation.getConversationCounterpart());
        holder.lastMessageBody.setText(conversation.getMessageReceived().getMessage());
        holder.date.setText(conversation.getMessageReceived().getTimeStampAsString(conversation.getMessageReceived().getTimestamp()));
        holder.inboxCounter.setVisibility(View.GONE);

        holder.conversation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chatActivity = new Intent(context, ChatActivity.class);
                SharedPreferences userPreferences =context.getSharedPreferences(context.getString(R.string.username_preferences), Context.MODE_PRIVATE);
                chatActivity.putExtra("recipientUsername",conversation.getConversationCounterpart() );
                chatActivity.putExtra("recipientUID", userPreferences.getString(conversation.getConversationCounterpart(),"void"));
                context.startActivity(chatActivity);
            }
        });

        return convertView;
    }

    public class ConversationViewHolder{

        CircularImageView avatar;
        TextView username;
        TextView lastMessageBody;
        TextView date;
        TextView inboxCounter;
        RelativeLayout conversation;
    }
}
