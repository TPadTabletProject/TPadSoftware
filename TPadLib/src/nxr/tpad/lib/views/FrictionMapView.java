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

import nxr.tpad.lib.TPad;
import nxr.tpad.lib.TPadService;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;

public class FrictionMapView extends View {
	private int height, width;

	//private TPadPhoneActivity tpadActivity;
	private TPad mTpad;

	private Bitmap dataBitmap;
	private Paint dataPaint;
	private float scaleFactor;

	private VelocityTracker vTracker;
	private static float vy, vx;
	private static float py, px;

	private static final int PREDICT_HORIZON = (int) (TPadService.OUTPUT_SAMPLE_RATE * (.020f)); // 500 samples, .1 second @ sample rate output
	private static float[] predictedPixels = new float[PREDICT_HORIZON];

	public FrictionMapView(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		Bitmap defaultBitmap = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888);
		setDataBitmap(defaultBitmap);

		dataPaint = new Paint();
		dataPaint.setColor(Color.DKGRAY);
		dataPaint.setAntiAlias(true);


	}
	
	public void setTpad(TPad tpad){
		mTpad = tpad;
		
	}


	public void setDataBitmap(Bitmap bmp) {
		dataBitmap = null;
		dataBitmap = bmp.copy(Bitmap.Config.ARGB_8888, true);
		invalidate();

	}

	private void resetScaleFactor() {		
		scaleFactor = dataBitmap.getWidth() / (float) width;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.drawColor(Color.MAGENTA);
		resetScaleFactor();
		Bitmap tempBitmap = Bitmap.createScaledBitmap(dataBitmap, (int)(dataBitmap.getWidth()/scaleFactor), (int)(dataBitmap.getHeight()/scaleFactor), false);
		canvas.drawBitmap(tempBitmap, 0,0, dataPaint);
		tempBitmap.recycle();
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		width = MeasureSpec.getSize(widthMeasureSpec);
		height = MeasureSpec.getSize(heightMeasureSpec);
		resetScaleFactor();
		invalidate();
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		switch (event.getActionMasked()) {

		case MotionEvent.ACTION_DOWN:
			px = event.getX() * scaleFactor;
			py = event.getY() * scaleFactor;

			vx = 0;
			vy = 0;

			// Start a new velocity tracker
			if (vTracker == null) {
				vTracker = VelocityTracker.obtain();
			} else {
				vTracker.clear();
			}
			vTracker.addMovement(event);

			break;

		case MotionEvent.ACTION_MOVE:
			px = event.getX() * scaleFactor;
			py = event.getY() * scaleFactor;

			vTracker.addMovement(event);

			// Compute velocity in pixels per 1 ms
			vTracker.computeCurrentVelocity(1);

			// get current velocities
			vx = vTracker.getXVelocity() * scaleFactor;
			vy = vTracker.getYVelocity() * scaleFactor;

			predictPixels();
			mTpad.sendFrictionBuffer(predictedPixels);

			break;

		case MotionEvent.ACTION_UP:
			mTpad.sendFriction(0f);
			break;

		case MotionEvent.ACTION_CANCEL:
			vTracker.recycle();
			break;
		}

		return true;
	}

	private void predictPixels() {
		float friction;
		int x = (int) px;
		int y = (int) py;
		
		float freqScaleFactor = (float) (1/(TPadService.OUTPUT_SAMPLE_RATE/1000.));

		for (int i = 0; i < predictedPixels.length; i++) {

			x = (int) (px + vx * i * freqScaleFactor ); // 1st order hold in x direction
			if (x >= dataBitmap.getWidth()) {
				x = dataBitmap.getWidth() - 1;
			} else if (x < 0)
				x = 0;

			y = (int) (py + vy * i * freqScaleFactor); // 1st order hold in y direction
			if (y >= dataBitmap.getHeight()) {
				y = dataBitmap.getHeight() - 1;
			} else if (y < 0)
				y = 0;

			friction = pixelToFriction(dataBitmap.getPixel(x, y));

			predictedPixels[i] = friction;
		}

	}

	private float pixelToFriction(int pixel) {
		float[] hsv = new float[3];
		Color.colorToHSV(pixel, hsv);
		return hsv[2];
	}

}
