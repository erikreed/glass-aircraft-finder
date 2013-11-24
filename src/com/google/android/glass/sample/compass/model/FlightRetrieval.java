package com.google.android.glass.sample.compass.model;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.http.AndroidHttpClient;
import android.os.StrictMode;
import android.util.Log;

public class FlightRetrieval {

  public static ArrayList<Flight> getFlights(double[] box) throws MalformedURLException,
      IOException, JSONException {
    String trackerUrl = getUrl(box);
    // URL url = new URL(trackerUrl);
    // URLConnection conn = url.openConnection();
//     conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
    // BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    // String inputText = in.readLine();
    // in.close();
    
    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
    StrictMode.setThreadPolicy(policy);

    String inputText = null;
    HttpClient httpclient = new DefaultHttpClient();
    HttpGet httpGet = new HttpGet(trackerUrl);
    httpGet.addHeader("X-Requested-With", "XMLHttpRequest");
    HttpResponse response = httpclient.execute(httpGet);
    StatusLine statusLine = response.getStatusLine();
    if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      response.getEntity().writeTo(out);
      out.close();
      inputText = out.toString();
    } else {
      // Closes the connection.
      response.getEntity().getContent().close();
      throw new IOException(statusLine.getReasonPhrase());
    }

//    Log.i("Flights", inputText);
    
    JSONObject json = new JSONObject(inputText);
    Log.i("Flights", json.toString(2));
    ArrayList<Flight> flights = new ArrayList<Flight>();
    int failed = 0;
    try {
      JSONObject plane;
      try {
        plane = json.getJSONObject("planes").getJSONObject("1");
      } catch (JSONException e2) {
        plane = json.getJSONObject("planes");
      }
      failed = parseFlights(flights, failed, plane);
    } catch (JSONException e) {
      JSONArray planes = json.getJSONArray("planes");
      for (int i = 0; i < planes.length(); i++) {
        JSONObject plane = planes.getJSONObject(i);
        failed = parseFlights(flights, failed, plane);
      }
    }
    boolean isPartial = json.getBoolean("isPartial");
    Log.i("Flights", flights.toString());
    System.out.printf("\n\nPartial: %s. Successfully loaded %d flights (%d failed)\n",
        Boolean.toString(isPartial), flights.size(), failed);
    return flights;
  }

  private static int parseFlights(ArrayList<Flight> flights, int failed, JSONObject plane)
      throws JSONException {
    Iterator iter = plane.keys();
    while (iter.hasNext()) {
      String id = (String) iter.next();
      try {
        Flight flight = new Flight(id, plane.getJSONArray(id));
        flights.add(flight);
      } catch (IllegalArgumentException e2) {
        failed++;
      }
    }
    return failed;
  }

  private static String getUrl(double[] box) {
    return String.format("http://planefinder.net/endpoints/update.php?" + "faa=1&bounds="
        + "%f,%f,%f,%f", box[0], box[1], box[2], box[3]);
  }

  public static double[] getBoundingBox(double lat, double lon, double radius, double[] asd) {
    double R = 6371; // earth radius in km
    double lon1 = lon - Math.toDegrees(radius / R / Math.cos(Math.toRadians(lat)));
    double lon2 = lon + Math.toDegrees(radius / R / Math.cos(Math.toRadians(lat)));
    double lat1 = lat + Math.toDegrees(radius / R);
    double lat2 = lat - Math.toDegrees(radius / R);
    double[] box = new double[] {Math.min(lat1, lat2), Math.min(lon1, lon2), Math.max(lat1, lat2),
        Math.max(lon1, lon2)};
    return box;
  }
}
