package it.polito.mad.sharenbook.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by claudiosava on 16/04/18.
 */

public class UserProfile implements Parcelable {

    private String userID;
    private String fullname;
    private String username;
    private String email;
    private String city;
    private String bio;
    private Uri picture_uri;

    public UserProfile (Parcel in){

        this.userID = in.readString();
        this.fullname = in.readString();
        this.username = in.readString();
        this.email = in.readString();
        this.city = in.readString();
        this.bio = in.readString();
        this.picture_uri = Uri.parse(in.readString());

    }

    public UserProfile (String userID,String fullname,String email, String pictureURI){

        this.userID = userID;
        this.fullname = fullname;
        this.email = email;

        if(pictureURI != null)
            this.picture_uri = Uri.parse(pictureURI);

    }

    public Map<String,Object> toMap(){
        Map<String,Object> result = new HashMap<String,Object>();

        result.put("userID",getUserID());
        result.put("fullname",getFullname());
        result.put("username",getUsername());
        result.put("email",getEmail());
        result.put("city",getCity());
        result.put("bio",getBio());
        result.put("picture_uri", getPicture_uri());

        return result;
    }


    public String getUserID() {
        return userID;
    }

    public String getFullname() {
        return fullname;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getCity() {
        return city;
    }

    public String getBio() {
        return bio;
    }

    public Uri getPicture_uri() {
        return picture_uri;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {

        dest.writeString(getUserID());
        dest.writeString(getFullname());
        dest.writeString(getUsername());
        dest.writeString(getEmail());
        dest.writeString(getCity());
        dest.writeString(getBio());
        dest.writeString(getPicture_uri().toString());

    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public UserProfile createFromParcel(Parcel in) {
            return new UserProfile(in);
        }

        public UserProfile[] newArray(int size) {
            return new UserProfile[size];
        }
    };
}
