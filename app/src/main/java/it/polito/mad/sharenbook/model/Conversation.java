package it.polito.mad.sharenbook.model;

import android.support.annotation.NonNull;

import com.google.firebase.storage.StorageReference;

public class Conversation implements Comparable<Conversation>{

    private String conversationCounterpart;
    private Message messageReceived;
    private int newInboxMessageCounter;
    private StorageReference profilePicRef;
    private long pictureSignature = 0;

    public Conversation(Conversation conversation){
        this.conversationCounterpart = conversation.getConversationCounterpart();
        this.messageReceived = conversation.getMessageReceived();
        this.newInboxMessageCounter = conversation.getNewInboxMessageCounter();
        this.profilePicRef = conversation.getProfilePicRef();
    }

    public Conversation(String conversationCounterpart, Message messageReceived, int newInboxMessageCounter, StorageReference profilePicRef){

        this.conversationCounterpart = conversationCounterpart;
        this.messageReceived = messageReceived;
        this.newInboxMessageCounter = newInboxMessageCounter;
        this.profilePicRef = profilePicRef;
    }

    public String getConversationCounterpart() {
        return conversationCounterpart;
    }

    public void setConversationCounterpart(String conversationCounterpart) {
        this.conversationCounterpart = conversationCounterpart;
    }

    public Message getMessageReceived() {
        return messageReceived;
    }

    public void setMessageReceived(Message messageReceived) {
        this.messageReceived = messageReceived;
    }

    public int getNewInboxMessageCounter() {
        return newInboxMessageCounter;
    }

    public void setNewInboxMessageCounter(int newInboxMessageCounter) {
        this.newInboxMessageCounter = newInboxMessageCounter;
    }

    public StorageReference getProfilePicRef() {
        return profilePicRef;
    }

    public void setProfilePicRef(StorageReference profilePicRef) {
        this.profilePicRef = profilePicRef;
    }
    public long getPictureSignature() {
        return pictureSignature;
    }

    public void setPictureSignature(long pictureSignature) {
        this.pictureSignature = pictureSignature;
    }


    @Override
    public int compareTo(@NonNull Conversation other) {

        //extract the message
        Message messageUnderComparison = other.getMessageReceived();
        Long currentMessageTimestamp = this.messageReceived.timestamp;
        Long underComparisonTimestamp = messageUnderComparison.getTimestamp();

        if((currentMessageTimestamp.compareTo(underComparisonTimestamp))!=0){
            return currentMessageTimestamp.compareTo(underComparisonTimestamp);
        }else
            return this.conversationCounterpart.compareTo(other.getConversationCounterpart());

    }


}
