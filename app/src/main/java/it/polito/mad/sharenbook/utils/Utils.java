package it.polito.mad.sharenbook.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Utils {

    // Approx Earth radius in KM
    private static final int EARTH_RADIUS = 6371;

    /**
     * Convert a comma separated multiple words string to a string list
     */
    public static List<String> commaStringToList(String commaString) {
        List<String> stringList = new ArrayList<>();
        for (String s : commaString.split(",")) {
            stringList.add(s.trim());
        }
        return stringList;
    }

    /**
     * Convert a string list to comma separated multiple words string
     */
    public static String listToCommaString(List<String> stringList) {
        StringBuilder sb = new StringBuilder();

        String prefix = "";
        for (String string : stringList) {
            sb.append(prefix);
            prefix = ", ";
            sb.append(string);
        }

        return sb.toString();
    }

    /**
     * Calculate distance between two location (lat and long) and return as string in km
     */
    public static String distanceBetweenLocations(double startLat, double startLong, double endLat, double endLong) {

        double dLat  = Math.toRadians((endLat - startLat));
        double dLong = Math.toRadians((endLong - startLong));

        startLat = Math.toRadians(startLat);
        endLat   = Math.toRadians(endLat);

        double a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLong);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        double distance = EARTH_RADIUS * c;

        return String.format(Locale.getDefault(), "%.1f", distance) + " Km";
    }

    private static double haversin(double val) {
        return Math.pow(Math.sin(val / 2), 2);
    }
}
