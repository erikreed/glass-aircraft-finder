/*
 * Copyright (C) 2013 Google Inc.
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

package com.google.android.glass.sample.compass;

import com.google.android.glass.sample.compass.model.Flight;
import com.google.android.glass.sample.compass.util.MathUtils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.location.Location;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a stylized compass, with text labels at the cardinal and ordinal directions, and tick marks
 * at the half-winds. The red "needles" in the display mark the current heading.
 */
public class CompassView extends View {

  /** Various dimensions and other drawing-related constants. */
  private static final float NEEDLE_WIDTH = 6;
  private static final float NEEDLE_HEIGHT = 125;
  private static final int NEEDLE_COLOR = Color.BLUE;
  private static final float TICK_WIDTH = 2;
  private static final float TICK_HEIGHT = 10;
  private static final float DIRECTION_TEXT_HEIGHT = 84.0f;
  private static final float PLACE_TEXT_HEIGHT = 22.0f;
  private static final float FLIGHT_ICON_WIDTH = 20.0f;
  private static final float PLACE_TEXT_LEADING = 4.0f;
  private static final float PLACE_TEXT_MARGIN = 8.0f;

  /**
   * The maximum number of places names to allow to stack vertically underneath the compass
   * direction labels.
   */
  private static final int MAX_OVERLAPPING_PLACE_NAMES = 4;

  /**
   * If the difference between two consecutive headings is less than this value, the canvas will be
   * redrawn immediately rather than animated.
   */
  private static final float MIN_DISTANCE_TO_ANIMATE = 15.0f;

  /** The actual heading that represents the direction that the user is facing. */
  private float mHeading;

  /**
   * Represents the heading that is currently being displayed when the view is drawn. This is used
   * during animations, to keep track of the heading that should be drawn on the current frame,
   * which may be different than the desired end point.
   */
  private float mAnimatedHeading;

  private OrientationManager mOrientation;
  private List<Flight> mFlights;

  private final Paint mPaint;
  private final Paint mTickPaint;
  private final Path mPath;
  private final TextPaint mPlacePaint;
  private final Bitmap mBitmapCessna;
  private final Bitmap mBitmapAirbus320;
  private final Rect mTextBounds;
  private final List<Rect> mAllBounds;
  private final NumberFormat mDistanceFormat;
  private final String[] mDirections;
  private final ValueAnimator mAnimator;
  private Bitmap mBitmapAirbus380;
  private Bitmap mBitmapAirbus340;
  private Bitmap mBitmapAirbus330;
  private Bitmap mBitmapBoeing737;
  private Bitmap mBitmapBoeing767;
  private Bitmap mBitmapBoeing777;
  private Bitmap mBitmapBoeing787;
  private Bitmap mBitmapBoeing747;
  private Bitmap mBitmapMD11;
  private Bitmap mBitmapCRJ1000;
  private Bitmap mBitmapBE200C;
  private Bitmap mBitmapBizjet;
  private Bitmap mBitmapE195;
  private Bitmap mBitmapERJ;

  public CompassView(Context context) {
    this(context, null, 0);
  }

  public CompassView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CompassView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    mPaint = new Paint();
    mPaint.setStyle(Paint.Style.FILL);
    mPaint.setAntiAlias(true);
    mPaint.setTextSize(DIRECTION_TEXT_HEIGHT);
    mPaint.setTypeface(Typeface.createFromFile(new File("/system/glass_fonts", "Roboto-Thin.ttf")));

    mTickPaint = new Paint();
    mTickPaint.setStyle(Paint.Style.STROKE);
    mTickPaint.setStrokeWidth(TICK_WIDTH);
    mTickPaint.setAntiAlias(true);
    mTickPaint.setColor(Color.WHITE);

    mPlacePaint = new TextPaint();
    mPlacePaint.setStyle(Paint.Style.FILL);
    mPlacePaint.setAntiAlias(true);
    mPlacePaint.setColor(Color.WHITE);
    mPlacePaint.setTextSize(PLACE_TEXT_HEIGHT);
    mPlacePaint.setTypeface(Typeface.createFromFile(new File("/system/glass_fonts",
        "Roboto-Light.ttf")));

    mPath = new Path();
    mTextBounds = new Rect();
    mAllBounds = new ArrayList<Rect>();

    mDistanceFormat = NumberFormat.getNumberInstance();
    mDistanceFormat.setMinimumFractionDigits(0);
    mDistanceFormat.setMaximumFractionDigits(1);

    mBitmapCessna = BitmapFactory.decodeResource(context.getResources(), R.drawable.cessna);
    mBitmapAirbus320 = BitmapFactory.decodeResource(context.getResources(), R.drawable.a320);
    mBitmapAirbus330 = BitmapFactory.decodeResource(context.getResources(), R.drawable.a330);
    mBitmapAirbus340 = BitmapFactory.decodeResource(context.getResources(), R.drawable.a340);
    mBitmapAirbus380 = BitmapFactory.decodeResource(context.getResources(), R.drawable.a380);
    mBitmapBoeing737 = BitmapFactory.decodeResource(context.getResources(), R.drawable.b737);
    mBitmapBoeing767 = BitmapFactory.decodeResource(context.getResources(), R.drawable.b767);
    mBitmapBoeing777 = BitmapFactory.decodeResource(context.getResources(), R.drawable.b777);
    mBitmapBoeing787 = BitmapFactory.decodeResource(context.getResources(), R.drawable.b787);
    mBitmapBoeing747 = BitmapFactory.decodeResource(context.getResources(), R.drawable.b747);
    mBitmapMD11 = BitmapFactory.decodeResource(context.getResources(), R.drawable.md11);
    mBitmapCRJ1000 = BitmapFactory.decodeResource(context.getResources(), R.drawable.crj1000);
    mBitmapBE200C = BitmapFactory.decodeResource(context.getResources(), R.drawable.be200c);
    mBitmapBizjet = BitmapFactory.decodeResource(context.getResources(), R.drawable.bizjet);
    mBitmapE195= BitmapFactory.decodeResource(context.getResources(), R.drawable.e195);
    mBitmapERJ = BitmapFactory.decodeResource(context.getResources(), R.drawable.erj);
    
    // We use NaN to indicate that the compass is being drawn for the first
    // time, so that we can jump directly to the starting orientation
    // instead of spinning from a default value of 0.
    mAnimatedHeading = Float.NaN;

    mDirections = context.getResources().getStringArray(R.array.direction_abbreviations);

    mAnimator = new ValueAnimator();
    setupAnimator();
  }

  /**
   * Sets the instance of {@link OrientationManager} that this view will use to get the current
   * heading and location.
   * 
   * @param orientationManager the instance of {@code OrientationManager} that this view will use
   */
  public void setOrientationManager(OrientationManager orientationManager) {
    mOrientation = orientationManager;
  }

  /**
   * Gets the current heading in degrees.
   * 
   * @return the current heading.
   */
  public float getHeading() {
    return mHeading;
  }

  /**
   * Sets the current heading in degrees and redraws the compass. If the angle is not between 0 and
   * 360, it is shifted to be in that range.
   * 
   * @param degrees the current heading
   */
  public void setHeading(float degrees) {
    mHeading = MathUtils.mod(degrees, 360.0f);
    animateTo(mHeading);
  }

  public void setFlights(List<Flight> flights) {
    mFlights = flights;
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    // The view displays 90 degrees across its width so that one 90 degree head rotation is
    // equal to one full view cycle.
    float pixelsPerDegree = getWidth() / 90.0f;
    float centerX = getWidth() / 2.0f;
    float centerY = getHeight() / 2.0f;

    canvas.save();
    canvas.translate(-mAnimatedHeading * pixelsPerDegree + centerX, centerY);

    // In order to ensure that places on a boundary close to 0 or 360 get drawn correctly, we
    // draw them three times; once to the left, once at the "true" bearing, and once to the
    // right.
    for (int i = -1; i <= 1; i++) {
      drawFlights(canvas, pixelsPerDegree, i * pixelsPerDegree * 360);
    }

    drawCompassDirections(canvas, pixelsPerDegree);

    canvas.restore();

    mPaint.setColor(NEEDLE_COLOR);
    drawNeedle(canvas, false);
    drawNeedle(canvas, true);
  }

  /**
   * Draws the compass direction strings (N, NW, W, etc.).
   * 
   * @param canvas the {@link Canvas} upon which to draw
   * @param pixelsPerDegree the size, in pixels, of one degree step
   */
  private void drawCompassDirections(Canvas canvas, float pixelsPerDegree) {
    float degreesPerTick = 360.0f / mDirections.length;

    mPaint.setColor(Color.WHITE);

    // We draw two extra ticks/labels on each side of the view so that the
    // full range is visible even when the heading is approximately 0.
    for (int i = -2; i <= mDirections.length + 2; i++) {
      if (MathUtils.mod(i, 2) == 0) {
        // Draw a text label for the even indices.
        String direction = mDirections[MathUtils.mod(i, mDirections.length)];
        mPaint.getTextBounds(direction, 0, direction.length(), mTextBounds);

        canvas.drawText(direction, i * degreesPerTick * pixelsPerDegree - mTextBounds.width() / 2,
            mTextBounds.height() / 2, mPaint);
      } else {
        // Draw a tick mark for the odd indices.
        canvas.drawLine(i * degreesPerTick * pixelsPerDegree, -TICK_HEIGHT / 2, i * degreesPerTick
            * pixelsPerDegree, TICK_HEIGHT / 2, mTickPaint);
      }
    }
  }

  /**
   * Draws the pins and text labels for the nearby list of places.
   * 
   * @param canvas the {@link Canvas} upon which to draw
   * @param pixelsPerDegree the size, in pixels, of one degree step
   * @param offset the number of pixels to translate the drawing operations by in the horizontal
   *        direction; used because place names are drawn three times to get proper wraparound
   */
  private void drawFlights(Canvas canvas, float pixelsPerDegree, float offset) {
    if (mOrientation.hasLocation() && mFlights != null) {
      synchronized (mFlights) {
        Location userLocation = mOrientation.getLocation();
        double userLat = userLocation.getLatitude();
        double userLon = userLocation.getLongitude();

        mAllBounds.clear();

        // Loop over the list of nearby places (those within 10 km of the user's current
        // location), and compute the relative bearing from the user's location to the
        // place's location. This determines the position on the compass view where the
        // pin will be drawn.
        for (Flight flight : mFlights) {
          double flightLat = flight.latitude;
          double flightLon = flight.longitude;
          float bearing = MathUtils.getBearing(userLat, userLon, flightLat, flightLon);

          String name = flight.flightNumber;
          double distanceKm = MathUtils.getDistance(userLat, userLon, flightLat, flightLon);
          String text =
              getContext().getResources().getString(R.string.place_text_format, name,
                  mDistanceFormat.format(distanceKm));

          // Measure the text and offset the text bounds to the location where the text
          // will finally be drawn.
          Rect textBounds = new Rect();
          mPlacePaint.getTextBounds(text, 0, text.length(), textBounds);
          textBounds.offsetTo(
              (int) (offset + bearing * pixelsPerDegree + FLIGHT_ICON_WIDTH / 2 + PLACE_TEXT_MARGIN),
              canvas.getHeight() / 2 - (int) PLACE_TEXT_HEIGHT);

          // Extend the bounds rectangle to include the pin icon and a small margin
          // to the right of the text, for the overlap calculations below.
          textBounds.left -= FLIGHT_ICON_WIDTH + PLACE_TEXT_MARGIN;
          textBounds.right += PLACE_TEXT_MARGIN;

          // This loop attempts to find the best vertical position for the string by
          // starting at the bottom of the display and checking to see if it overlaps
          // with any other labels that were already drawn. If there is an overlap, we
          // move up and check again, repeating this process until we find a vertical
          // position where there is no overlap, or when we reach the limit on
          // overlapping place names.
          boolean intersects;
          int numberOfTries = 0;
          do {
            intersects = false;
            numberOfTries++;
            textBounds.offset(0, (int) -(PLACE_TEXT_HEIGHT + PLACE_TEXT_LEADING));

            for (Rect existing : mAllBounds) {
              if (Rect.intersects(existing, textBounds)) {
                intersects = true;
                break;
              }
            }
          } while (intersects && numberOfTries <= MAX_OVERLAPPING_PLACE_NAMES);

          // Only draw the string if it would not go high enough to overlap the compass
          // directions. This means some places may not be drawn, even if they're nearby.
          if (numberOfTries <= MAX_OVERLAPPING_PLACE_NAMES) {
            mAllBounds.add(textBounds);
            Bitmap planeIcon;
            String type = flight.type;
            if (type.startsWith("A32") || type.startsWith("A31")) {
              planeIcon = mBitmapAirbus320;
            } else if (type.startsWith("A33") || type.startsWith("A30") || type.startsWith("B75")) {
              planeIcon = mBitmapAirbus330;
            } else if (type.startsWith("B73")) {
              planeIcon = mBitmapBoeing737;
            } else if (type.startsWith("A34")) {
              planeIcon = mBitmapAirbus340;
            } else if (type.startsWith("A38")) {
              planeIcon = mBitmapAirbus380;
            } else if (type.startsWith("B74")) {
              planeIcon = mBitmapBoeing747;
            } else if (type.startsWith("B76")) {
              planeIcon = mBitmapBoeing767;
            } else if (type.startsWith("B77")) {
              planeIcon = mBitmapBoeing777;
            } else if (type.startsWith("B78")) {
              planeIcon = mBitmapBoeing787;
            } else if (type.startsWith("MD") || type.startsWith("DC")) {
              planeIcon = mBitmapMD11;
            } else if (type.startsWith("BE")) {
              planeIcon = mBitmapBE200C;
            } else if (type.startsWith("CRJ")) {
              planeIcon = mBitmapCRJ1000;
            } else if (type.startsWith("E1")) {
              planeIcon = mBitmapE195;
            } else if (type.startsWith("ERJ")) {
              planeIcon = mBitmapERJ;
            } else if (type.startsWith("U")) {
              planeIcon = mBitmapBE200C;
            } else if (type.startsWith("FA") || type.startsWith("F2") || type.startsWith("GL") || 
                type.startsWith("LJ") || type.startsWith("C56") || type.startsWith("E5")) {
              planeIcon = mBitmapBizjet;
            } else {
              planeIcon = mBitmapCessna;
            }
            canvas.drawBitmap(planeIcon, offset + bearing * pixelsPerDegree - FLIGHT_ICON_WIDTH
                / 2, textBounds.top + 2, mPaint);
            canvas.drawText(text, offset + bearing * pixelsPerDegree + FLIGHT_ICON_WIDTH / 2
                + PLACE_TEXT_MARGIN, textBounds.top + PLACE_TEXT_HEIGHT, mPlacePaint);
          }
        }
      }
    }
  }

  /**
   * Draws a needle that is centered at the top or bottom of the compass.
   * 
   * @param canvas the {@link Canvas} upon which to draw
   * @param bottom true to draw the bottom needle, or false to draw the top needle
   */
  private void drawNeedle(Canvas canvas, boolean bottom) {
    float centerX = getWidth() / 2.0f;
    float origin;
    float sign;

    // Flip the vertical coordinates if we're drawing the bottom needle.
    if (bottom) {
      origin = getHeight();
      sign = -1;
    } else {
      origin = 0;
      sign = 1;
    }

    float needleHalfWidth = NEEDLE_WIDTH / 2;

    mPath.reset();
    mPath.moveTo(centerX - needleHalfWidth, origin);
    mPath.lineTo(centerX - needleHalfWidth, origin + sign * (NEEDLE_HEIGHT - 4));
    mPath.lineTo(centerX, origin + sign * NEEDLE_HEIGHT);
    mPath.lineTo(centerX + needleHalfWidth, origin + sign * (NEEDLE_HEIGHT - 4));
    mPath.lineTo(centerX + needleHalfWidth, origin);
    mPath.close();

    canvas.drawPath(mPath, mPaint);
  }

  /**
   * Sets up a {@link ValueAnimator} that will be used to animate the compass when the distance
   * between two sensor events is large.
   */
  private void setupAnimator() {
    mAnimator.setInterpolator(new LinearInterpolator());
    mAnimator.setDuration(250);

    // Notifies us at each frame of the animation so we can redraw the view.
    mAnimator.addUpdateListener(new AnimatorUpdateListener() {

      @Override
      public void onAnimationUpdate(ValueAnimator animator) {
        mAnimatedHeading = MathUtils.mod((Float) mAnimator.getAnimatedValue(), 360.0f);
        invalidate();
      }
    });

    // Notifies us when the animation is over. During an animation, the user's head may have
    // continued to move to a different orientation than the original destination angle of the
    // animation. Since we can't easily change the animation goal while it is running, we call
    // animateTo() again, which will either redraw at the new orientation (if the difference is
    // small enough), or start another animation to the new heading. This seems to produce
    // fluid results.
    mAnimator.addListener(new AnimatorListenerAdapter() {

      @Override
      public void onAnimationEnd(Animator animator) {
        animateTo(mHeading);
      }
    });
  }

  /**
   * Animates the view to the specified heading, or simply redraws it immediately if the difference
   * between the current heading and new heading are small enough that it wouldn't be noticeable.
   * 
   * @param end the desired heading
   */
  private void animateTo(float end) {
    // Only act if the animator is not currently running. If the user's orientation changes
    // while the animator is running, we wait until the end of the animation to update the
    // display again, to prevent jerkiness.
    if (!mAnimator.isRunning()) {
      float start = mAnimatedHeading;
      float distance = Math.abs(end - start);
      float reverseDistance = 360.0f - distance;
      float shortest = Math.min(distance, reverseDistance);

      if (Float.isNaN(mAnimatedHeading) || shortest < MIN_DISTANCE_TO_ANIMATE) {
        // If the distance to the destination angle is small enough (or if this is the
        // first time the compass is being displayed), it will be more fluid to just redraw
        // immediately instead of doing an animation.
        mAnimatedHeading = end;
        invalidate();
      } else {
        // For larger distances (i.e., if the compass "jumps" because of sensor calibration
        // issues), we animate the effect to provide a more fluid user experience. The
        // calculation below finds the shortest distance between the two angles, which may
        // involve crossing 0/360 degrees.
        float goal;

        if (distance < reverseDistance) {
          goal = end;
        } else if (end < start) {
          goal = end + 360.0f;
        } else {
          goal = end - 360.0f;
        }

        mAnimator.setFloatValues(start, goal);
        mAnimator.start();
      }
    }
  }
}
