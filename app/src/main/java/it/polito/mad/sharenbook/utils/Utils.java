package it.polito.mad.sharenbook.utils;

import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;

import javax.net.ssl.HttpsURLConnection;

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
     * Convert long timestamp to string time
     */
    public static String convertTime(long time, String format){
        Date date = new Date(time);
        Format dateFormat = new SimpleDateFormat(format);
        return dateFormat.format(date);
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


    public static <E> List<E> toReverseList(Iterable<E> iterable) {
        if(iterable instanceof List) {
            return (List<E>) iterable;
        }
        ArrayList<E> list = new ArrayList<E>();
        if(iterable != null) {
            for(E e: iterable) {
                list.add(e);
            }
        }
        Collections.reverse(list);
        return list;
    }


    public static void sendNotification(String jsonBody){
        AsyncTask.execute(() -> {
            int SDK_INT = Build.VERSION.SDK_INT;
            if(SDK_INT > 8){

                StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                        .permitAll().build();
                StrictMode.setThreadPolicy(policy);

                HttpsURLConnection conn = null;
                OutputStream outputStream = null;

                try{

                    String jsonResponse;

                    URL url = new URL("https://onesignal.com/api/v1/notifications");
                    conn = (HttpsURLConnection) url.openConnection();
                    conn.setUseCaches(false);
                    conn.setDoOutput(true);
                    conn.setDoInput(true);

                    conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                    conn.setRequestProperty("Authorization", "Basic ZTc3MjExODEtYmM4Yy00YjU5LWFjNWEtM2VlNGNmYTA0OWU1");
                    conn.setRequestMethod("POST");

                    Log.d("strJsonBody:" , jsonBody);

                    byte[] sendBytes = jsonBody.getBytes("UTF-8");
                    conn.setFixedLengthStreamingMode(sendBytes.length);

                    outputStream = conn.getOutputStream();
                    outputStream.write(sendBytes);

                    int httpResponse = conn.getResponseCode();
                    Log.d("httpResponse: " , "" + httpResponse);

                    if (httpResponse >= HttpURLConnection.HTTP_OK
                            && httpResponse < HttpURLConnection.HTTP_BAD_REQUEST) {
                        Scanner scanner = new Scanner(conn.getInputStream(), "UTF-8");
                        jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                        scanner.close();
                    } else {
                        Scanner scanner = new Scanner(conn.getErrorStream(), "UTF-8");
                        jsonResponse = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
                        scanner.close();
                    }
                    Log.d("jsonResponse: " , jsonResponse);

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {

                    if(conn!=null)
                        conn.disconnect();

                    try {
                        if(outputStream!=null)
                            outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                }

            }
        });
    }
}
