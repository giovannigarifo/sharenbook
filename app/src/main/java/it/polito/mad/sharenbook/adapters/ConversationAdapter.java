package it.polito.mad.sharenbook.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageException;
import com.mikhaellopez.circularimageview.CircularImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import it.polito.mad.sharenbook.ChatActivity;
import it.polito.mad.sharenbook.R;
import it.polito.mad.sharenbook.model.Conversation;
import it.polito.mad.sharenbook.utils.GlideApp;
import it.polito.mad.sharenbook.utils.UserInterface;

public class ConversationAdapter extends BaseAdapter {

    private List<Conversation> conversations = new ArrayList<>();
    private long withoutIncomingMessagesCounter = 0;
    private Context context;


    public long getWithoutIncomingMessagesCounter() {
        return withoutIncomingMessagesCounter;
    }

    public void setWithoutIncomingMessagesCounter(long withoutIncomingMessagesCounter) {
        this.withoutIncomingMessagesCounter = withoutIncomingMessagesCounter;
    }

    public ConversationAdapter(Context context){
        this.context = context;
    }

    public void addConversation(Conversation conversation) {

        if(this.conversations.size()==(int)this.withoutIncomingMessagesCounter){
            /** CHECK IF LAST MESSAGE VIEWED*/
            if(conversation.getMessageReceived().getUsername().equals(conversation.getConversationCounterpart())
                    && !conversation.getMessageReceived().isViewed()) {
                conversation.setNewInboxMessageCounter(1);
            }
            this.conversations.add(0,conversation);
            Log.d("Conversation aft sort:","message not viewed so counter"+conversation.getNewInboxMessageCounter());
            notifyDataSetChanged();
        }
        else {
            /** CHECK IF LAST MESSAGE VIEWED*/
            if(conversation.getMessageReceived().getUsername().equals(conversation.getConversationCounterpart())
                    && !conversation.getMessageReceived().isViewed()) {
                conversation.setNewInboxMessageCounter(1);
                Log.d("Conversation","sorting->added counter because message is not viewed"+conversation.getNewInboxMessageCounter());
            }
            this.conversations.add(conversation);
            sortAfterInsertNewElement();
            // Log.d("Conversation during:","size:"+conversations.size()+"chats"+withoutIncomingMessagesCounter);
            if (this.conversations.size() == this.withoutIncomingMessagesCounter)
                notifyDataSetChanged();
        }

    }

    private void sortAfterInsertNewElement() {
        // The last object is the only one that is potentially not in the right
        // position

        Conversation swap;
        for (int pos = conversations.size() - 1; pos > 0
                && conversations.get(pos).compareTo(conversations.get(pos - 1)) > 0; pos--) {
            Collections.swap(conversations,pos,pos-1);
        }

    }

    public void modifyConversation(Conversation conversation){

        Conversation upFront = null;
        int index = 0;
        for(Conversation conv : conversations){
            if(conv.getConversationCounterpart().equals(conversation.getConversationCounterpart())) {
                conv.setMessageReceived(conversation.getMessageReceived());
                conv.setProfilePicRef(conversation.getProfilePicRef());
                if(conversation.getMessageReceived().getUsername().equals(conversation.getConversationCounterpart())
                        &&!conversation.getMessageReceived().isViewed()) {
                    conv.setNewInboxMessageCounter(1);
                    Log.d("Conversation","received message counter->"+conv.getNewInboxMessageCounter());
                }
                else {
                    conv.setNewInboxMessageCounter(0);
                    Log.d("Conversation","sent message counter ->"+conv.getNewInboxMessageCounter());
                }
                index = conversations.indexOf(conv);
                upFront = new Conversation(conversation);
                break;
            }

        }

        if(upFront!=null && index!=0){
            conversations.remove(index);
            addConversation(upFront);
        }
        notifyDataSetChanged();
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


        // if(conversation.getProfilePicRef()!=null)
        //   UserInterface.showGlideImage(context, conversation.getProfilePicRef(), holder.avatar, 0);

        String username = conversation.getConversationCounterpart();

        DatabaseReference recipientPicSignature = FirebaseDatabase.getInstance().getReference("usernames").child(username).child("picSignature");
        recipientPicSignature.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d("PictureListener:","lunched");
                if(dataSnapshot.exists()){
                    long picSignature = (long) dataSnapshot.getValue();
                    UserInterface.showGlideImage(context,
                            conversation.getProfilePicRef(),
                            holder.avatar,
                            picSignature);
                } else {
                    GlideApp.with(context).load(context.getResources().getDrawable(R.drawable.ic_profile)).into(holder.avatar);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        holder.username.setText(conversation.getConversationCounterpart());
        holder.lastMessageBody.setText(conversation.getMessageReceived().getMessage());
        holder.date.setText(conversation.getMessageReceived().getTimeStampAsString(conversation.getMessageReceived().getTimestamp()));

        if(conversation.getNewInboxMessageCounter() == 0)
            holder.inboxCounter.setVisibility(View.GONE);
        else{
            holder.inboxCounter.setVisibility(View.VISIBLE);
            holder.inboxCounter.setText(context.getString(R.string.NEW));

        }


        convertView.setTag(holder);


        holder.conversation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                conversations.get(position).setNewInboxMessageCounter(0);
                notifyDataSetChanged();

                Intent chatActivity = new Intent(context, ChatActivity.class);
                chatActivity.putExtra("recipientUsername",conversation.getConversationCounterpart() );
                context.startActivity(chatActivity);

            }
        });

        return convertView;
    }

    public class ConversationViewHolder{

        ImageView avatar;
        TextView username;
        TextView lastMessageBody;
        TextView date;
        TextView inboxCounter;
        RelativeLayout conversation;
    }
}
