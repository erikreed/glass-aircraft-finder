package com.google.android.glass.sample.compass.model;


import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Erik Reed
 */
public class FlightManager {

  private double currentLon = 0;
  private double currentLat = 0;
  private long timeLastRefreshed = 0;

  private static final String TAG = FlightManager.class.getSimpleName();

  /**
   * distance threshold for determining bounding box for aircraft
   */
  private static final double MAX_DISTANCE_KM = 20;

  private final ArrayList<Flight> flightsLoaded;

  public FlightManager(Context context) {
    flightsLoaded = new ArrayList<Flight>();
    refreshFlights();
  }
  
  public int getNumFlights() {
    return flightsLoaded.size();
  }

  public synchronized void refreshFlights() {
    if (currentLat * currentLon == 0) {
      Log.w(TAG, "Can't refresh flights--no lat/long info.");
      return;
    }
    if (System.currentTimeMillis() - timeLastRefreshed <= 5000) {
      Log.i(TAG, "Not refreshing flights--timeout not exceeded.");
      return;
    }
    Log.i(TAG, "Refreshing flights...");
    flightsLoaded.clear();
    double[] box = FlightRetrieval.getBoundingBox(currentLat, currentLon, MAX_DISTANCE_KM, null);
    try {
      ArrayList<Flight> flights = FlightRetrieval.getFlights(box);
      flightsLoaded.addAll(flights);
    } catch (Exception e) {
      Log.e(TAG, e.getMessage(), e);
    }
    timeLastRefreshed = System.currentTimeMillis();
  }

  public List<Flight> getFlights() {
    return flightsLoaded;
  }

  // terribly annoying Glass/Android bug--removing this uncalled method causes an
  // "Unable to execute dex: java.nio.BufferOverflowException. Check the Eclipse log for stack trace."
  @SuppressWarnings("unused")
  private Object jsonObjectToPlace(JSONObject object) {
    return null;
  }

  public void setLocation(double latitude, double longitude) {
    Log.i(TAG, String.format("Lat/long set to: %f/%f", latitude, longitude));
    currentLat = latitude;
    currentLon = longitude;
  }
}
