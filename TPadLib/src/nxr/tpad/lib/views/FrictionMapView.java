/*
 * Copyright 2014 TPad Tablet Project. All rights reserved.
 *
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL ARSHAN POURSOHI OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied.
 */

package nxr.tpad.lib.views;

import ioio.lib.spi.Log;
import nxr.tpad.lib.TPad;
import nxr.tpad.lib.TPadService;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

public class FrictionMapView extends View {
	// Local Height and width of view class. These change dynamically when the
	// view is resized by the system
	private int height, width;

	private boolean displayShowing = true;
	private boolean dataDisplayed = true;

	// Local reference to TPad object
	public TPad mTpad;

	// Holders of Haptic data
	public Bitmap dataBitmap;
	public Bitmap displayBitmap;
	public Paint dataPaint;

	// Scale factor for shrinking incoming bitmaps to proper size of the view
	// class
	private float hapticScaleFactor;
	private float visualScaleFactor;

	// Android velocity tracking object, used in predicting finger position
	public VelocityTracker vTracker;

	// Velocity and position variables of the finger. Get updated at approx 60Hz
	// when the user is touching
	public static float vy, vx;
	public static float py, px;

	// Prediction "Horizon", that is the number of samples needed to extrapolate
	// finger position until the next position is taken
	public static final int PREDICT_HORIZON = (int) (TPadService.OUTPUT_SAMPLE_RATE * (.020f)); // 125
																								// samples,
																								// 20ms
																								// @
																								// sample
																								// rate
																								// output
	// Array for holding the extrapolated pixel values
	public static float[] predictedPixels = new float[PREDICT_HORIZON];

	String TAG = "FrictionMapView";

	// Main Constructor for FrictionMapView
	public FrictionMapView(Context context, AttributeSet attrs) {
		super(context, attrs);

		height = 10;
		width = 10;
		
		// Set default bitmap to 10x10 square, just so we don't get null pointer
		// exceptions
		Bitmap defaultBitmap = Bitmap.createBitmap(10, 10,Bitmap.Config.ARGB_8888);

		setDataBitmap(defaultBitmap);
		setDisplayBitmap(defaultBitmap);

		defaultBitmap.recycle();

		// Set some graphic properties flags of the dataBitmap since it will
		// also be shown to the screen
		dataPaint = new Paint();
		dataPaint.setColor(Color.MAGENTA);
		dataPaint.setAntiAlias(true);

	}

	// Called by creating activity to initialize the local TPad reference object
	public void setTpad(TPad tpad) {
		mTpad = tpad;
	}

	// Called by outside class to set a new bitmap as the haptic data for this
	// view
	public void setDataBitmap(Bitmap bmp) {

		hapticScaleFactor = bmp.getWidth() / (float) width;
		// Create new bitmap from a copy of the reference. This ensures the
		// reference copy won't be used, and it can then be destroyed.
		dataBitmap = null;
		dataBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);
		
		invalidate();

	}

	public void setDisplayBitmap(Bitmap bmp) {

		visualScaleFactor = bmp.getWidth() / (float) width;
		// Create new bitmap from a copy of the reference. This ensures the
		// reference copy won't be used, and it can then be destroyed.
		displayBitmap = null;
		displayBitmap = bmp.copy(Bitmap.Config.ARGB_4444, true);

		// Invalidate calls the onDraw() function of the view class, when will
		// draw the newly copied dataBitmap to the screen
		invalidate();

	}
	
	private void resetScaleFactors(){
		hapticScaleFactor = dataBitmap.getWidth() / (float) width;
		visualScaleFactor = displayBitmap.getWidth() / (float) width;
	}

	public void setDisplayShowing(boolean bool) {
		displayShowing = bool;
		invalidate();
	}

	public boolean isDisplayShowing() {
		return displayShowing;
	}

	public void setDataDisplayed(boolean bool) {
		dataDisplayed = bool;
		invalidate();

	}

	public boolean isDataDisplayed() {
		return dataDisplayed;
	}

	// Main method for updating graphics on the screen. Only gets called at the
	// beginning, or when a new dataBitmap is loaded
	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		// Draw a background color in case of overdraw on the screen. Mostly for
		// error checking purposes
		canvas.drawColor(Color.DKGRAY);

		if (displayShowing) {
			if (dataDisplayed) {
				drawScaledBitmap(dataBitmap, canvas, hapticScaleFactor);
			} else {
				drawScaledBitmap(displayBitmap, canvas, visualScaleFactor);
			}
		}
	}

	private void drawScaledBitmap(Bitmap bit, Canvas can, float factor) {

		Log.i(TAG, "Factor:" + String.valueOf(factor) + " Bitwidth:"+ String.valueOf(bit.getWidth()));

		// Create a temporary bitmap that is a properly scaled version of the
		// data Bitmap
		Bitmap tempBitmap = Bitmap.createScaledBitmap(bit,(int) (bit.getWidth() / factor),(int) (bit.getHeight() / factor), false);
		can.drawBitmap(tempBitmap, 0, 0, dataPaint);

		tempBitmap.recycle();

	}
	// Method getting called when the view changes dimentions. Used in the
	// background for most purposes.
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		width = MeasureSpec.getSize(widthMeasureSpec);
		height = MeasureSpec.getSize(heightMeasureSpec);
		Log.i(TAG, "New Measure: " +String.valueOf(width)+"w "+String.valueOf(height)+"h");
		
		resetScaleFactors();		
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	// Main Touch even method. Gets called whenever Android reports a new touch
	// event.
	@Override
	public boolean onTouchEvent(MotionEvent event) {

		// Switch by which type of event occured
		switch (event.getActionMasked()) {

		// Case where user first touches down on the screen
		case MotionEvent.ACTION_DOWN:

			// Get position coordinates, and scale for proper dataBitmap size
			px = event.getX() * hapticScaleFactor;
			py = event.getY() * hapticScaleFactor;

			// Reset velocities to zero
			vx = 0;
			vy = 0;

			// Start a new velocity tracker
			if (vTracker == null) {
				vTracker = VelocityTracker.obtain();
			} else {
				vTracker.clear();
			}

			// Add first event to tracker
			vTracker.addMovement(event);

			break;

		case MotionEvent.ACTION_MOVE:

			// Get position coordinates, and scale for proper dataBitmap size
			px = event.getX() * hapticScaleFactor;
			py = event.getY() * hapticScaleFactor;

			// Add new motion even to the velocity tracker
			vTracker.addMovement(event);

			// Compute velocity in pixels/ms
			vTracker.computeCurrentVelocity(1);

			// Get computed velocities, and scale them appropriately
			vx = vTracker.getXVelocity() * hapticScaleFactor;
			vy = vTracker.getYVelocity() * hapticScaleFactor;

			// Call prediction algorithm below. This function computes the
			// proper extrapolation of friction values and automatically updates
			// predictedPixels with these values
			predictPixels();

			// Send the predicted values to the tpad as an array
			mTpad.sendFrictionBuffer(predictedPixels);

			break;

		case MotionEvent.ACTION_UP:
			// Turn off TPad when user lifts finger
			mTpad.sendFriction(0f);
			break;

		case MotionEvent.ACTION_CANCEL:
			// Recycle velocity tracker on a cancel event
			vTracker.recycle();
			break;
		}

		return true;
	}

	// Main extrapolation algorithm used to calculate upsampled friction values
	// to the TPad
	public void predictPixels() {
		// Local friction values
		float friction;

		// Local x,y values, based on most recent px, py
		int x = (int) px;
		int y = (int) py;

		// A frequency scaling factor to ensure we are producing the correct
		// number of samples to be played back
		float freqScaleFactor = (float) (1 / (TPadService.OUTPUT_SAMPLE_RATE / 1000.));

		// Main extrapolation loop. This is where the extrapolated data is
		// produced
		for (int i = 0; i < predictedPixels.length; i++) {

			// 1st order hold in x direction
			x = (int) (px + vx * i * freqScaleFactor);

			// Ensure we are not going off the edge of the bitmap with our
			// extrapolated point
			if (x >= dataBitmap.getWidth()) {
				x = dataBitmap.getWidth() - 1;
			} else if (x < 0)
				x = 0;

			// 1st order hold in y direction
			y = (int) (py + vy * i * freqScaleFactor);

			// Ensure we are not going off the edge of the bitmap with our
			// extrapolated point
			if (y >= dataBitmap.getHeight()) {
				y = dataBitmap.getHeight() - 1;
			} else if (y < 0)
				y = 0;

			// Get the pixel value at this predicted position, convert it to a
			// friction value, and store it
			friction = pixelToFriction(dataBitmap.getPixel(x, y));

			// Save the stored friction value into the buffer that will be sent
			// to the TPad to be played back as real-time as possible
			predictedPixels[i] = friction;
		}

	}

	// Main mapping to go from a pixel color to a friction value. OVERRIDE THIS
	// FUNCTION FOR NEW MAPPINGS FROM COLOR TO FRICTION
	public float pixelToFriction(int pixel) {
		// Setup a Hue, Saturation, Value matrix
		float[] hsv = new float[3];

		// Convert the RGB color in to HSV data and store it
		Color.colorToHSV(pixel, hsv);

		// Return the Value of the color, which, generally, corresponds to the
		// grayscale value of the color from 0-1
		return hsv[2];
	}

}
