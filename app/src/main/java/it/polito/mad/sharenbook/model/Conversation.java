package it.polito.mad.sharenbook.model;

import com.google.firebase.storage.StorageReference;

public class Conversation {

    private String conversationCounterpart;
    private Message messageReceived;
    private int newInboxMessageCounter;
    private StorageReference profilePicRef;

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


}
