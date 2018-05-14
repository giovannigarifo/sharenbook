package it.polito.mad.sharenbook.model;


public class Message {

    String message;
    boolean thisBelongsToMe;
    String username;
    boolean hide;



    public Message(String message, boolean thisBelongsToMe, String username, boolean hide){

        this.message = message;
        this.thisBelongsToMe = thisBelongsToMe;
        this.username = username;
        this.hide = hide;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isThisBelongToMe() {
        return thisBelongsToMe;
    }

    public void isThisBelongsToMe(boolean thisBelongsToMe) {
        this.thisBelongsToMe = thisBelongsToMe;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isHide() {
        return hide;
    }

    public void setHide(boolean hide) {
        this.hide = hide;
    }

}
