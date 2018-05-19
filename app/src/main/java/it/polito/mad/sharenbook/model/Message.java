package it.polito.mad.sharenbook.model;


import android.content.Context;
import android.text.format.DateUtils;
import android.util.Log;

public class Message {

    String message;
    boolean thisBelongsToMe;
    String username;
    boolean hide;
    long timestamp;
    boolean viewed = false;
    Context context;


    public Message() {
    }

    public Message(String message, boolean thisBelongsToMe, String username, boolean hide, long timestamp, Context context) {

        this.message = message;
        this.thisBelongsToMe = thisBelongsToMe;
        this.username = username;
        this.hide = hide;
        this.timestamp = timestamp;
        this.context = context;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isViewed() {
        return viewed;
    }

    public void setViewed(boolean viewed) {
        this.viewed = viewed;
    }

    public String getTimeStampAsString(long timestamp) {
        String formattedDate = DateUtils.formatDateTime(context, timestamp,
                DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_NUMERIC_DATE
                        | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_24HOUR);

        return formattedDate;
    }

    public String getHour(long timestamp) {
        String timestamp_split[] = getTimeStampAsString(timestamp).split(" ");
        String hour = timestamp_split[1];

        return hour;
    }

    public String getDate(long timestamp) {
        String timestamp_split[] = getTimeStampAsString(timestamp).replace(",", "").split(" ");
        String data = timestamp_split[0];

        return data;
    }

}
