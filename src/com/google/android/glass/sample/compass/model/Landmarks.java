/*
 * Copyright (C) 2013 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.glass.sample.compass.model;


import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides access to a list of hard-coded landmarks (located in
 * {@code res/raw/landmarks.json}) that will appear on the compass when the user is near them.
 */
public class Landmarks {

  private static final String TAG = Landmarks.class.getSimpleName();

  /**
   * The threshold used to display a landmark on the compass.
   */
  private static final double MAX_DISTANCE_KM = 10;

  /**
   * The list of landmarks loaded from resources.
   */
  private final ArrayList<Flight> flightsLoaded;

  /**
   * Initializes a new {@code Landmarks} object by loading the landmarks from the resource bundle.
   */
  public Landmarks(Context context) {
    flightsLoaded = new ArrayList<Flight>();

    // This class will be instantiated on the service's main thread, and doing I/O on the
    // main thread can be dangerous if it will block for a noticeable amount of time. In
    // this case, we assume that the landmark data will be small enough that there is not
    // a significant penalty to the application. If the landmark data were much larger,
    // we may want to load it in the background instead.
    refreshFlights();

  }
  
  public int getNumFlights() {
    return flightsLoaded.size();
  }

  public synchronized void refreshFlights() {
    Log.i(TAG, "Refreshing flights...");
    flightsLoaded.clear();
    double[] box = FlightRetrieval.getBoundingBox(37.433982, -122.118807, MAX_DISTANCE_KM, null);
    try {
      ArrayList<Flight> flights = FlightRetrieval.getFlights(box);
      flightsLoaded.addAll(flights);
    } catch (Exception e) {
      Log.e(TAG, e.getMessage(), e);
    }
  }

  /**
   * Gets a list of landmarks that are within ten kilometers of the specified coordinates. This
   * function will never return null; if there are no locations within that threshold, then an empty
   * list will be returned.
   */
  public List<Flight> getFlights(double latitude, double longitude) {
    return flightsLoaded;
  }

  /**
   * Converts a JSON object that represents a place into a {@link Place} object.
   */
  private Place jsonObjectToPlace(JSONObject object) {
    return null;
  }

}
