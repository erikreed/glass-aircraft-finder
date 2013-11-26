package com.google.android.glass.sample.compass.model;
import java.lang.reflect.Array;
import java.lang.reflect.Field;

import org.json.JSONArray;
import org.json.JSONException;

public class Flight {

  public final String identification;
  public final String type;
  public final String registration;
  public final String flightNumber;
  public final double latitude;
  public final double longitude;
  public final double altitude;
  public final double angle; // heading
  public final double speed; // ground speed
  public final String timestamp; // zulu probably
  public final String airline;
  public final String flightNumber2;
  public final String[] path;

  public Flight(String identification, JSONArray input) throws JSONException {
    if (input.length() != 12 && input.length() != 11) {
      throw new IllegalArgumentException("Bad flight json. Input len: " + input.length());
    }
    this.identification = identification;
    type = input.getString(0);
    registration = input.getString(1);
    flightNumber = input.getString(2);
    latitude = input.getDouble(3);
    longitude = input.getDouble(4);
    altitude = input.getDouble(5);
    angle = input.getDouble(6);
    speed = input.getDouble(7);
    timestamp = input.getString(8);
    airline = input.getString(9);
    if (input.length() == 12) {
      flightNumber2 = input.getString(10);
      path = input.getString(11).split("-");
    } else {
      flightNumber2 = "N/A";
      path = input.getString(10).split("-");
    }
  }

  @Override
  public String toString() {
    return dump(this);
  }

  public static String dump(Object o) {
    StringBuffer buffer = new StringBuffer();
    Class<? extends Object> oClass = o.getClass();
    if (oClass.isArray()) {
      buffer.append("Array: ");
      buffer.append("[");
      for (int i = 0; i < Array.getLength(o); i++) {
        Object value = Array.get(o, i);
        if (value.getClass().isPrimitive() || value.getClass() == java.lang.Long.class
            || value.getClass() == java.lang.Integer.class
            || value.getClass() == java.lang.Boolean.class
            || value.getClass() == java.lang.String.class
            || value.getClass() == java.lang.Double.class
            || value.getClass() == java.lang.Short.class
            || value.getClass() == java.lang.Byte.class) {
          buffer.append(value);
          if (i != (Array.getLength(o) - 1)) buffer.append(",");
        } else {
          buffer.append(dump(value));
        }
      }
      buffer.append("]\n");
    } else {
      buffer.append("Class: " + oClass.getName());
      buffer.append("{\n");
      while (oClass != null) {
        Field[] fields = oClass.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
          fields[i].setAccessible(true);
          buffer.append(fields[i].getName());
          buffer.append("=");
          try {
            Object value = fields[i].get(o);
            if (value != null) {
              if (value.getClass().isPrimitive() || value.getClass() == java.lang.Long.class
                  || value.getClass() == java.lang.String.class
                  || value.getClass() == java.lang.Integer.class
                  || value.getClass() == java.lang.Boolean.class
                  || value.getClass() == java.lang.Double.class
                  || value.getClass() == java.lang.Short.class
                  || value.getClass() == java.lang.Byte.class) {
                buffer.append(value);
              } else {
                buffer.append(dump(value));
              }
            }
          } catch (IllegalAccessException e) {
            buffer.append(e.getMessage());
          }
          buffer.append("\n");
        }
        oClass = oClass.getSuperclass();
      }
      buffer.append("}\n");
    }
    return buffer.toString();
  }
}
