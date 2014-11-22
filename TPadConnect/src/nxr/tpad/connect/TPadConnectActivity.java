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


package nxr.tpad.connect;

import nxr.tpad.lib.TPad;
import nxr.tpad.lib.TPadImpl;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public class TPadConnectActivity extends Activity implements Runnable {

	private TextView mHeadText;
	private TextView mServiceStatusText, mTpadStatusText, mFreqText, mScaleText;
	public EditText mFreqEdit;
	private Button mFreqButton;
	private SeekBar mScaleBar;
	
	private String TAG = "TPadConnectAct";
	private boolean workerRunning = false;
	private Thread mWorker;
	
	private float localProgress = 0;

	public TPad mTpad;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.activity_tpad_connect);

		// Intent startIntent = new Intent();
		// startIntent.setAction("TPS");
		mTpad = new TPadImpl(this);

		if (!mTpad.getBound()) {
			startService(new Intent(this, TPadConnectService.class));
		}

		mHeadText = (TextView) findViewById(R.id.headText);
		mServiceStatusText = (TextView) findViewById(R.id.serviceStatusText);
		mTpadStatusText = (TextView) findViewById(R.id.tpadStatusText);
		mFreqEdit = (EditText) findViewById(R.id.freqEdit);
		mFreqText = (TextView) findViewById(R.id.freqText);
		mFreqButton = (Button) findViewById(R.id.freqButton);
		mScaleText = (TextView) findViewById(R.id.scaleText);
		mScaleBar = (SeekBar) findViewById(R.id.scaleBar);

		mHeadText.setText("TPad Connection Manager");

		mFreqEdit.setOnEditorActionListener(new OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
					int newFreq = Integer.parseInt(mFreqEdit.getText().toString());
					mTpad.sendNewFreq(newFreq);
				}
				return false;
			}

		});

		mFreqButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				int newFreq = Integer.parseInt(mFreqEdit.getText().toString());
				mTpad.sendNewFreq(newFreq);

			}

		});
		
		mScaleBar.setMax(100);
		
		mScaleBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				localProgress = (float) progress;
				
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				mTpad.sendNewScale(localProgress/(float)mScaleBar.getMax());
				Log.i(TAG, "New scale: " + String.valueOf(localProgress/(float)mScaleBar.getMax()));
				
			}
			
			
		});

		super.onCreate(savedInstanceState);

	}

	private void refreshStatusText() {
		if (mTpad.getBound()) {
			mServiceStatusText.setText("Connected!");
			mServiceStatusText.setTextColor(Color.GREEN);
		} else {
			mServiceStatusText.setText("Not connected");
			mServiceStatusText.setTextColor(Color.RED);
		}

		if (mTpad.getTpadStatus()) {
			mTpadStatusText.setText("Connected! (streaming)");
			mTpadStatusText.setTextColor(Color.GREEN);
		} else {
			mTpadStatusText.setText("Not connected");
			mTpadStatusText.setTextColor(Color.RED);
		}

		mFreqText.setText(String.valueOf(mTpad.getLocalFreq()) + " Hz");
		mScaleText.setText(String.valueOf(mTpad.getLocalScale()*100f) + "%");
		
		Log.i(TAG, "Scale value: " + String.valueOf(mTpad.getLocalScale()));
	}
	@Override
	protected void onResume() {
		workerRunning = true;
		mWorker = new Thread(this);
		mWorker.setPriority(Thread.NORM_PRIORITY);
		mWorker.start();

		Log.i(TAG, "Resuming Activity");
		super.onResume();
	}

	@Override
	protected void onPause() {
		workerRunning = false;
		while (true) {
			try {
				mWorker.join();
				break;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		mWorker = null;
		Log.i(TAG, "Pausing Activity");
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		mTpad.disconnectTPad();
		super.onDestroy();
	}

	@Override
	public void run() {

		while (workerRunning) {

			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mTpad.refreshFreq();
					mTpad.refreshScale();
					mTpad.getTpadStatus();
					refreshStatusText();

				}
			});

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}