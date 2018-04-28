package it.polito.mad.sharenbook.utils;

import android.util.Patterns;
import android.widget.EditText;

import java.util.regex.Pattern;

public class InputValidator {

    //Regex for input validation
    private static final Pattern ISBN_REGEX = Pattern.compile("^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$");

    /**
     * Validate ISBN (10 or 13 digits, in latter case it should start with 978 or 979)
     *
     * @param input EditText containing the isbn number
     * @return true if input is a bad format isbn number
     */
    public static boolean isWrongIsbn(EditText input) {
        String data = input.getText().toString();
        return !ISBN_REGEX.matcher(data).matches();
    }

    /**
     * Check if an email address is bad formatted
     *
     * @param input EditText containing the email address
     * @return true if input is a bad format email address
     */
    public static boolean isWrongEmailAddress(EditText input) {
        String data = input.getText().toString();
        return !Patterns.EMAIL_ADDRESS.matcher(data).matches();
    }
}
