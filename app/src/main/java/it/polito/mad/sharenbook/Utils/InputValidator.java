package it.polito.mad.sharenbook.Utils;

import android.widget.EditText;

import java.util.regex.Pattern;

public class InputValidator {

    //Regex for input validation
    private static final Pattern ISBN_REGEX = Pattern.compile("^(97(8|9))?\\d{9}(\\d|X)$");

    // Validate ISBN (10 or 13 digits, in latter case it should start with 978 or 979)
    public static boolean isWrongIsbn(EditText input) {
        String data = input.getText().toString();
        return !ISBN_REGEX.matcher(data).matches();
    }

}
