package it.polito.mad.sharenbook.Utils;




import it.polito.mad.sharenbook.model.UserProfile;

public class NavigationDrawerManager {

    private static NavigationDrawerProfile navigationDrawerProfile;

    public static void setNavigationDrawerProfileByUser (UserProfile user){

        if(navigationDrawerProfile == null) { // if not exits creates it
            navigationDrawerProfile = new NavigationDrawerProfile(user);
        }else { //otherwise the method is used to overwrite the fields
            navigationDrawerProfile.setUser_fullname(user.getFullname());
            navigationDrawerProfile.setUser_email(user.getEmail());
            navigationDrawerProfile.setUser_picturePath("images/" + user.getUserID() + ".jpg");
            navigationDrawerProfile.setPictureSignature(user.getPicture_timestamp());


        }


    }

    public static void setNavigationDrawerProfileByFields(String name,String email,String userID, String pictureSignature){

        if(navigationDrawerProfile == null) {
            navigationDrawerProfile = new NavigationDrawerProfile(name,email,"images/" + userID + ".jpg",pictureSignature);
        }else {
            navigationDrawerProfile.setUser_fullname(name);
            navigationDrawerProfile.setUser_email(email);
            navigationDrawerProfile.setUser_picturePath("images/" + userID + ".jpg");
            navigationDrawerProfile.setPictureSignature(pictureSignature);

        }

    }

    public static NavigationDrawerProfile getNavigationDrawerProfile(){
        return navigationDrawerProfile;
    }
}
