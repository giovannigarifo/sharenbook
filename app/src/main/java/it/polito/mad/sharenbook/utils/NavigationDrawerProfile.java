package it.polito.mad.sharenbook.utils;


import it.polito.mad.sharenbook.model.UserProfile;

public class NavigationDrawerProfile {




    private  String user_fullname;
    private  String user_email;
    private  String user_picturePath;
    private  String pictureSignature;


    public NavigationDrawerProfile(UserProfile user){

        user_fullname = user.getFullname();
        user_email = user.getEmail();
        user_picturePath = "images/" + user.getUsername() + ".jpg";
        pictureSignature = user.getPicture_timestamp();

    }

    public NavigationDrawerProfile(String user_fullname,String user_email,String username, String pictureSignature){

        this.user_fullname = user_fullname;
        this.user_email = user_email;
        this.user_picturePath = "images/" + username + ".jpg";
        this.pictureSignature = pictureSignature;

    }

    public String getUser_fullname() {
        return user_fullname;
    }

    public void setUser_fullname(String user_fullname) {
        this.user_fullname = user_fullname;
    }

    public String getUser_email() {
        return user_email;
    }

    public void setUser_email(String user_email) {
        this.user_email = user_email;
    }

    public String getUser_picturePath() {
        return user_picturePath;
    }

    public void setUser_picturePath(String user_picturePath) {
        this.user_picturePath = user_picturePath;
    }
    public String getPictureSignature() {
        return pictureSignature;
    }

    public void setPictureSignature(String pictureSignature) {
        this.pictureSignature = pictureSignature;
    }

}
