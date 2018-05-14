package it.polito.mad.sharenbook.model;

import com.mikhaellopez.circularimageview.CircularImageView;

public class Message {



    String message;
    boolean thisBelongToMe;
    String username;
    boolean hide;



    public Message(String message, boolean thisBelongToMe, String username, boolean hide){

        this.message = message;
        this.thisBelongToMe = thisBelongToMe;
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
        return thisBelongToMe;
    }

    public void isThisBelongToMe(boolean thisBelongToMe) {
        this.thisBelongToMe = thisBelongToMe;
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
