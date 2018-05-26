package it.polito.mad.sharenbook.model;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    private String picture_timestamp;
    private List<Integer> categories;

    public UserProfile() {

    }

    public UserProfile(Parcel in) {

        this.userID = in.readString();
        this.fullname = in.readString();
        this.username = in.readString();
        this.email = in.readString();
        this.city = in.readString();
        this.bio = in.readString();
        this.picture_timestamp = in.readString();
        this.categories = in.readArrayList(Integer.class.getClassLoader());

    }

    /**
     * UserProfile Complete Constructor
     */
    public UserProfile(String userID, String fullname, String username, String email, String city, String bio, String picture_timestamp) {

        this.userID = userID;
        this.fullname = fullname;
        this.username = username;
        this.email = email;
        this.city = city;
        this.bio = bio;

        if (picture_timestamp != null)
            this.picture_timestamp = picture_timestamp;

        this.categories = new ArrayList<>();

    }


    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<String, Object>();

        result.put("userID", getUserID());
        result.put("fullname", getFullname());
        result.put("username", getUsername());
        result.put("email", getEmail());
        result.put("city", getCity());
        result.put("bio", getBio());
        result.put("picture_timestamp", getPicture_timestamp());
        //result.put("pref_categories", getCategories());

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

    public String getPicture_timestamp() {
        return picture_timestamp;
    }

    public List<Integer> getCategories() {
        return categories;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public void setPicture_timestamp(String picture_timestamp) {
        this.picture_timestamp = picture_timestamp;
    }

    public void setCategories(List<Integer> categories) {
        this.categories = categories;
    }

    public String getCategoriesAsString(String[] bookCategoriesStringArray) {

        String categoriesAsString = "";

        if (this.categories != null)
            for (int i = 0; i < this.categories.size(); i++) {
                String category = Arrays.asList(bookCategoriesStringArray).get(this.categories.get(i));

                if (i == 0)
                    categoriesAsString = category;
                else
                    categoriesAsString = categoriesAsString + ", " + category;
            }

        return categoriesAsString;
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
        dest.writeString(getPicture_timestamp());
        dest.writeList(getCategories());

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
