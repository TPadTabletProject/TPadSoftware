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

package com.example.hellotpadphone;

import nxr.tpad.lib.TPad;
import nxr.tpad.lib.TPadImpl;
import nxr.tpad.lib.consts.TPadVibration;
import nxr.tpad.lib.views.FrictionMapView;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

public class HelloTPadActivity extends Activity {

	// Define 'View' classes that will link to the .xml file
	View basicView;
	View timeView;
	
	// Define 'FrictionMapView' class that will link to the .xml file
	FrictionMapView fricView;

	// Instantiate a new TPad object
	TPad mTpad;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
				
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_hello_tpad);

		// Get TPad reference from the TPad Implementation Library
		mTpad = new TPadImpl(this);
		
		// Link the first 'View' called basicView to the view with the id=view1
		basicView = (View) findViewById(R.id.view1);
		// Set the background color of the view to blue
		basicView.setBackgroundColor(Color.BLUE);

				
		basicView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				// Grab the x coordinate of the touch event, and the width of the view the event was in
				float x = event.getX();
				int width = view.getWidth();

				// The switch case below looks at the event's properties and specifies what type of touch it was
				switch (event.getAction()) {

				case MotionEvent.ACTION_DOWN:
					// If the initial touch was on the left half of the view, turn off the TPad, else turn it on to 100%
					if (x < width / 2f) {
						mTpad.turnOff();
					} else {
						mTpad.sendFriction(1f);
					}
					break;

				case MotionEvent.ACTION_MOVE:
					// If the user moves to the left half of the view, turn off the TPad, else turn it on to 100%
					if (x < width / 2f) {
						mTpad.turnOff();
					} else {
						mTpad.sendFriction(1f);
					}
					break;

				case MotionEvent.ACTION_UP:
					// If the user lifts up their finger from the screen, turn the TPad off (0%)
					mTpad.turnOff();
					break;
				}

				return true;
			}
		});
		
		// Same linking as before, only with a different view
		timeView = (View) findViewById(R.id.view2);
		timeView.setBackgroundColor(Color.RED);

		timeView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				
				
				switch (event.getAction()) {

				case MotionEvent.ACTION_DOWN:
					
					mTpad.sendVibration(TPadVibration.SINUSOID, 350, 1.0f);
					break;

				case MotionEvent.ACTION_UP:
					// Turn off the TPad when the user lifts up (set to 0%)
					mTpad.turnOff();
					break;
				}

				return true;
			}
		});
		
		// Link FrictionMapView to the .xml file
		fricView = (FrictionMapView) findViewById(R.id.view3);
		
		// Set the TPad of the FrictionMapView to the current TPad
		fricView.setTpad(mTpad);
		
		// Load an image from resources
		Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.spacialgrad);
		
		// Set the friction data bitmap to the test image
		fricView.setDataBitmap(defaultBitmap);
			
	}

	@Override
	protected void onDestroy() {
		mTpad.disconnectTPad();
		super.onDestroy();
	}

}