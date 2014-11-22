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


package nxr.tpad.lib;

import ioio.lib.api.DigitalInput;
import ioio.lib.api.DigitalOutput;
import ioio.lib.api.IOIO;
import ioio.lib.api.Sequencer;
import ioio.lib.api.Sequencer.ChannelConfig;
import ioio.lib.api.Sequencer.ChannelConfigPwmPosition;
import ioio.lib.api.Sequencer.ChannelCuePwmPosition;
import ioio.lib.api.SpiMaster;
import ioio.lib.api.SpiMaster.Rate;
import ioio.lib.api.exception.ConnectionLostException;
import ioio.lib.util.BaseIOIOLooper;
import ioio.lib.util.IOIOLooper;
import ioio.lib.util.android.IOIOService;

import java.nio.FloatBuffer;

import nxr.tpad.lib.consts.TPadMessage;
import nxr.tpad.lib.consts.TPadVibration;
import nxr.tpad.lib.R;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;

public abstract class TPadService extends IOIOService {

	private final String TAG = "TPadPhoneLib";
	public final static int BUFFER_SIZE = 6250; // enough buffer for a 1 hz signal
	public final static long OUTPUT_SAMPLE_RATE = 6250; // 6.250kHz output rate

	private static float TPadValue;
	private static volatile boolean textureOn = false;
	private static FloatBuffer tpadFrictionBuffer = FloatBuffer.allocate(BUFFER_SIZE);
	private static FloatBuffer tpadVibrationBuffer = FloatBuffer.allocate(BUFFER_SIZE);

	private Looper looper;

	private int TPadFreq = 30000;
	private double FGN_MCLK = 4000000l; // 4MHz onboard precision oscillator
	private int cueWidth = (int) (62500 / OUTPUT_SAMPLE_RATE);

	private SpiMaster spiBus_; // Controls function generator and the DAC on the same SPI

	private final int SS_FGN_PIN = 4; // IOIO Pin 32, PIC Pin 12

	private final int SPI_CLK = 5; // IOIO Pin 40, PIC Pin 22
	private final int SPI_MOSI = 6; // IOIO Pin 39, PIC Pin 21
	private final int SPI_MISO = 3; // NOT USED, PIC pin 42

	private final int MUTE = 40; // IOIO Pin 34, PIC pin 14

	private final int PWM_OUT = 7;

	private final int SENSE = 45;

	private short maxPwmOutput = (short) (1.2 / (3.3 / 2) * 512);

	private int maxFgnOutput = (int) Math.pow(2, 28); // This represents a 4MHz output max!

	class Looper extends BaseIOIOLooper {
		private DigitalOutput led_;
		private DigitalOutput mute_;
		private int freq = 20000;
		private Sequencer.ChannelCuePwmPosition pwmCueChannel_ = new ChannelCuePwmPosition();
		private Sequencer.ChannelCue[] cue_ = new Sequencer.ChannelCue[] { pwmCueChannel_ };
		private Sequencer sequencer_;

		double timeoutTimer;
		double loopTimer;
		double lastloop;
		int timeoutMillis = 1000;

		public int tpadConnected = 0;
		
		public float tpadScale = 0.6f;

		@Override
		public void setup() throws ConnectionLostException, InterruptedException {

			final ChannelConfigPwmPosition pwmConfig = new Sequencer.ChannelConfigPwmPosition(Sequencer.Clock.CLK_16M, 512, 0, new DigitalOutput.Spec(PWM_OUT)); // 1/(62.5nS*512) = 62500 pwm freq

			final ChannelConfig[] config = new ChannelConfig[] { pwmConfig };

			sequencer_ = ioio_.openSequencer(config);

			int numOfCues = sequencer_.available();
			Log.i(TAG, "Num of Cues: " + String.valueOf(numOfCues));

			freq = TPadFreq;

			led_ = ioio_.openDigitalOutput(IOIO.LED_PIN, true);

			mute_ = ioio_.openDigitalOutput(MUTE, false);

			spiBus_ = ioio_.openSpiMaster(new DigitalInput.Spec(SPI_MISO), new DigitalOutput.Spec(SPI_MOSI), new DigitalOutput.Spec(SPI_CLK), new DigitalOutput.Spec[] { new DigitalOutput.Spec(
					SS_FGN_PIN) }, new SpiMaster.Config(Rate.RATE_8M, true, true));

			initializeFGN();

			tpadFrictionBuffer.clear();
			tpadVibrationBuffer.clear();

			sequencer_.start();

			Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY + Process.THREAD_PRIORITY_LESS_FAVORABLE);

			Thread.sleep(100);

			mute_.write(true);
		}

		@Override
		public void loop() throws ConnectionLostException, InterruptedException {

			lastloop = loopTimer;
			loopTimer = System.nanoTime();

			if (tpadConnected == 0) {
				tpadConnected = 1;
			}

			ioio_.beginBatch();

			synchronized (tpadFrictionBuffer) {

				if (tpadFrictionBuffer.hasRemaining()) {

					led_.write(false);

					for (int i = 0; i < 10; i++) {
						if (tpadFrictionBuffer.hasRemaining()) {
							TPadValue = tpadFrictionBuffer.get();
							push(TPadValue);
						} else {
							ioio_.endBatch();
							Log.i(TAG, "Early Return, loop: " + String.valueOf(i));
							return;
						}
					}

				} else if (textureOn) {

					synchronized (tpadVibrationBuffer) {
						if (tpadVibrationBuffer.hasRemaining()) {
							tpadFrictionBuffer.clear();
							tpadFrictionBuffer.put(tpadVibrationBuffer);
							tpadFrictionBuffer.flip();
						} else {
							tpadFrictionBuffer.rewind();
							led_.write(false);

							for (int i = 0; i < 10; i++) {
								if (tpadFrictionBuffer.hasRemaining()) {
									TPadValue = tpadFrictionBuffer.get();
									push(TPadValue);
								} else {
									ioio_.endBatch();
									// Log.i(TAG, "Early Return, loop: " + String.valueOf(i));
									return;
								}
							}

						}

					}
				} else {
					led_.write(true);

				}

			}

			if (freq != TPadFreq) {
				sendNewFreq(TPadFreq);
				freq = TPadFreq;
			}

			ioio_.endBatch();

			// wait until the end of our refresh period. Ensures more precise timings
			while ((System.nanoTime()) < (loopTimer + 1.6 * 1000000.0)) {

			}

		}

		@Override
		public void disconnected() {
			tpadConnected = 0;
			super.disconnected();
		}

		private void push(float value) throws ConnectionLostException, InterruptedException {
			
			float reciprocal = (-1f * (value * tpadScale - .5f) + .5f);
			// float reciprocal = (-1f * (value - .5f) + .5f);

			short widthVal = (short) (reciprocal * maxPwmOutput);

			if (widthVal == 1 || widthVal < 0) {
				widthVal = 0;
			}
			pwmCueChannel_.pulseWidth = widthVal;

			sequencer_.push(cue_, cueWidth);

		}

		private void initializeFGN() throws ConnectionLostException {

			// Begin by setting the 16 bit control register.
			// D15 = D14 = 0 to set the control register
			// D13 = 1 allows us to load a complete 28 bit word in two 14 bit writes
			// D12 = 1 but this is ignored since D13 is 1
			// D11 = D10 = 0
			// D9 = 0 (bits control FSELECT/PSELECT)
			// D8 = 0 to disable reset
			// D7 = 0 to enable MCLK
			// D6 = 0 to enable DAC onboard
			// D5 = D4 = D3 = 0 to disable SIGN BIT OUT and its options
			// D2 = 0 Reserved, must be 0
			// D1 = 0 for SIN output (1 for triangle)
			// D0 = 0 Reserved, must be 0

			// Bit labels _______ 5432109876543210
			short controlData = 0b0011000000000000;

			byte[] dataBytes = new byte[2];
			dataBytes[0] = (byte) (controlData >>> 8);
			dataBytes[1] = (byte) controlData; // LitteEndian style

			spiBus_.writeReadAsync(0, dataBytes, dataBytes.length, dataBytes.length, null, 0);

			dataBytes = null;
			dataBytes = new byte[4];
			Log.i(TAG, "Freq Out: " + String.valueOf(TPadFreq));
			int freqInt = (int) (TPadFreq / FGN_MCLK * maxFgnOutput); // This is a fraction of the mclk, which is 4MHz

			// ________________________5432109876543210________________5432109876543210
			short lsbData = (short) (0b0100000000000000 | (freqInt & 0b0011111111111111)); // Only take the first 14 bits of the 28 bit number
			dataBytes[0] = (byte) (lsbData >>> 8);
			dataBytes[1] = (byte) lsbData;

			short msbData = (short) (0b0100000000000000 | (freqInt >>> 14)); // Only take the last 14 bits of the 28 bit number
			dataBytes[2] = (byte) (msbData >>> 8);
			dataBytes[3] = (byte) msbData;

			spiBus_.writeReadAsync(0, dataBytes, dataBytes.length, dataBytes.length, null, 0);

		}

	}

	public void setFreq(int i) {
		TPadFreq = i;
	}
	
	public void setScale(float scale) {
		looper.tpadScale = scale;
	}

	private void sendNewFreq(int newFreq) throws ConnectionLostException {

		byte[] dataBytes = new byte[4];

		Log.i(TAG, "Freq Out: " + String.valueOf(TPadFreq));
		int freqInt = (int) (TPadFreq / FGN_MCLK * maxFgnOutput); // This is a fraction of the mclk, which is 4MHz

		// ________________________5432109876543210________________5432109876543210
		short lsbData = (short) (0b0100000000000000 | (freqInt & 0b0011111111111111)); // Only take the first 14 bits of the 28 bit number
		dataBytes[0] = (byte) (lsbData >>> 8);
		dataBytes[1] = (byte) lsbData;

		short msbData = (short) (0b0100000000000000 | (freqInt >>> 14)); // Only take the last 14 bits of the 28 bit number
		dataBytes[2] = (byte) (msbData >>> 8);
		dataBytes[3] = (byte) msbData;

		spiBus_.writeReadAsync(0, dataBytes, dataBytes.length, dataBytes.length, null, 0);

	}

	public void sendTPad(float f) {
		synchronized (tpadFrictionBuffer) {
			textureOn = false;
			tpadFrictionBuffer.clear();
			tpadFrictionBuffer.put(f);
			tpadFrictionBuffer.flip();
		}

	}

	public void sendTPadBuffer(float[] buffArray) {
		synchronized (tpadFrictionBuffer) {
			textureOn = false;
			tpadFrictionBuffer.clear();
			tpadFrictionBuffer.put(buffArray);
			tpadFrictionBuffer.flip();
		}
	}

	public void sendVibration(int type, float freq, float amp) {

		int periodSamps = (int) ((1 / freq) * OUTPUT_SAMPLE_RATE);

		synchronized (tpadVibrationBuffer) {

			tpadVibrationBuffer.clear();
			tpadVibrationBuffer.limit(periodSamps);

			float tp = 0;

			switch (type) {

			case TPadVibration.SINUSOID:

				for (float i = 0; i < periodSamps; i++) {

					tp = (float) ((1 + Math.sin(2 * Math.PI * freq * i / OUTPUT_SAMPLE_RATE)) / 2f);

					tpadVibrationBuffer.put(amp * tp);

				}

				break;
			case TPadVibration.SAWTOOTH:
				for (float i = 0; i < periodSamps; i++) {

					tpadVibrationBuffer.put(amp * (i / periodSamps));

				}
				break;

			case TPadVibration.TRIANGLE:
				for (float i = 0; i < periodSamps / 2; i++) {

					tpadVibrationBuffer.put(amp * tp++ * 2 / periodSamps);

				}
				for (float i = periodSamps / 2; i < periodSamps; i++) {

					tpadVibrationBuffer.put(amp * tp-- * 2 / periodSamps);

				}

				break;
			case TPadVibration.SQUARE:

				for (float i = 0; i < tpadVibrationBuffer.limit(); i++) {

					tp = (float) ((1 + Math.sin(2 * Math.PI * freq * i / OUTPUT_SAMPLE_RATE)) / 2f);

					if (tp > (.5)) {
						tpadVibrationBuffer.put(amp);

					} else
						tpadVibrationBuffer.put(0);

				}

				break;
			default:
				break;

			}

			tpadVibrationBuffer.flip();
		}

		synchronized (tpadFrictionBuffer) {
			textureOn = true;
		}

	}

	/*
	 * public void sendTPadDualTexture(int type1, float freq1, float amp1, int type2, float freq2, float amp2) {
	 * 
	 * float minfreq = Math.min(freq1, freq2);
	 * 
	 * int periodSamps = (int) ((1 / minfreq) * OUTPUT_SAMPLE_RATE); float[] tempArray = new float[periodSamps];
	 * 
	 * float tp = 0;
	 * 
	 * switch (type1) {
	 * 
	 * case TPadVibration.SINUSOID:
	 * 
	 * for (int i = 0; i < periodSamps; i++) {
	 * 
	 * tp = (float) ((1 + Math.sin(2 * Math.PI * freq1 * i / OUTPUT_SAMPLE_RATE)) / 2f); tempArray[i] = amp1 * tp;
	 * 
	 * }
	 * 
	 * break; case TPadVibration.SAWTOOTH: for (int i = 0; i < periodSamps; i++) { tempArray[i] = amp1 * (i / periodSamps);
	 * 
	 * } break;
	 * 
	 * case TPadVibration.SQUARE:
	 * 
	 * for (int i = 0; i < periodSamps; i++) {
	 * 
	 * tp = (float) ((1 + Math.sin(2 * Math.PI * freq1 * i / OUTPUT_SAMPLE_RATE)) / 2f);
	 * 
	 * if (tp > (.5)) { tempArray[i] = amp1;
	 * 
	 * } else tempArray[i] = 0;
	 * 
	 * }
	 * 
	 * break; default: break;
	 * 
	 * }
	 * 
	 * switch (type2) {
	 * 
	 * case TPadVibration.SINUSOID:
	 * 
	 * for (int i = 0; i < periodSamps; i++) {
	 * 
	 * tp = (float) ((1 + Math.sin(2 * Math.PI * freq2 * i / OUTPUT_SAMPLE_RATE)) / 2f); tempArray[i] *= amp2 * tp;
	 * 
	 * }
	 * 
	 * break; case TPadVibration.SAWTOOTH: for (int i = 0; i < periodSamps; i++) { tempArray[i] *= amp2 * (i / periodSamps);
	 * 
	 * } break;
	 * 
	 * case TPadVibration.SQUARE:
	 * 
	 * for (int i = 0; i < periodSamps; i++) {
	 * 
	 * tp = (float) ((1 + Math.sin(2 * Math.PI * freq2 * i / OUTPUT_SAMPLE_RATE)) / 2f);
	 * 
	 * if (tp > (.5)) { tempArray[i] *= amp2;
	 * 
	 * } else tempArray[i] *= 0;
	 * 
	 * }
	 * 
	 * break; default: break;
	 * 
	 * }
	 * 
	 * synchronized (tpadTextureBuffer) {
	 * 
	 * tpadTextureBuffer.clear(); tpadTextureBuffer.limit(periodSamps);
	 * 
	 * tpadTextureBuffer.put(tempArray);
	 * 
	 * tpadTextureBuffer.flip(); }
	 * 
	 * synchronized (tpadValueBuffer) { textureOn = true; }
	 * 
	 * }
	 */

	public void addTextureBuff() {
		synchronized (tpadFrictionBuffer) {
			tpadFrictionBuffer.clear();
			tpadFrictionBuffer.put(tpadVibrationBuffer.array());
			tpadFrictionBuffer.flip();
		}
	}

	@Override
	protected IOIOLooper createIOIOLooper() {
		looper = new Looper();

		return looper;
	}

	final static int[] vibroVals = new int[] {TPadVibration.SINUSOID,TPadVibration.SAWTOOTH, TPadVibration.TRIANGLE, TPadVibration.SQUARE};

	class IncomingHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			Bundle data;
			switch (msg.what) {

			case TPadMessage.SET_TPAD_FREQ:
				data = msg.getData();
				setFreq(data.getInt("newFreq"));
				Log.i(TAG, "New freq: " + String.valueOf(data.getInt("newFreq")));
				break;

			case TPadMessage.SET_TPAD_SCALE:
				data = msg.getData();
				setScale(data.getFloat("newScale"));
				Log.i(TAG, "New scale: " + String.valueOf(data.getFloat("newScale")));
				break;
				
			case TPadMessage.CHECK_TPAD:
				try {
					Message rsp = Message.obtain(null, TPadMessage.CHECK_TPAD);
					Bundle brsp = new Bundle();
					brsp.putInt("connected", looper.tpadConnected);

					rsp.setData(brsp);
					msg.replyTo.send(rsp);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;

			case TPadMessage.GET_TPAD_FREQ:
				try {
					Message rsp = Message.obtain(null, TPadMessage.GET_TPAD_FREQ);
					Bundle brsp = new Bundle();
					brsp.putInt("Freq", getFreq());

					rsp.setData(brsp);
					msg.replyTo.send(rsp);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;
			case TPadMessage.GET_TPAD_SCALE:
				try {
					Message rsp = Message.obtain(null, TPadMessage.GET_TPAD_SCALE);
					Bundle brsp = new Bundle();
					brsp.putFloat("Scale", getScale());
					Log.i(TAG, "Service sending scale back:" + String.valueOf(getScale()));
					rsp.setData(brsp);
					msg.replyTo.send(rsp);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				break;

			case TPadMessage.SEND_TPAD_TEXTURE:
				data = msg.getData();

				int type = vibroVals[data.getInt("type")];
				float freq = data.getFloat("freq");
				float amp = data.getFloat("amp");

				sendVibration(type, freq, amp);

				break;
			/*
			 * case TPadMessage.SEND_TPAD_DUAL_TEXTURE: data = msg.getData();
			 * 
			 * int dtype = textureVals[data.getInt("type")]; float dfreq = data.getFloat("freq"); float damp = data.getFloat("amp");
			 * 
			 * int dtype2 = textureVals[data.getInt("type2")]; float dfreq2 = data.getFloat("freq2"); float damp2 = data.getFloat("amp2");
			 * 
			 * sendTPadDualTexture(dtype, dfreq, damp, dtype2, dfreq2, damp2); break;
			 */

			case TPadMessage.SEND_TPAD:
				data = msg.getData();
				sendTPad(data.getFloat("f"));
				break;

			case TPadMessage.SEND_TPAD_BUFFER:
				data = msg.getData();
				sendTPadBuffer(data.getFloatArray("buffArray"));
				break;

			default:
				Log.i("TPad", "msg not recognized");
				break;
			}

		}
	}

	final Messenger myMessenger = new Messenger(new IncomingHandler());

	@Override
	public void onCreate() {
		super.onCreate();
		Log.i(TAG, "Service Starting");
		setFreq(TPadFreq);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return myMessenger.getBinder();
	}

	public int getFreq() {
		return looper.freq;
	}
	
	public float getScale() {
		return looper.tpadScale;
	}


	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = "TPad Service Launched";
		long when = System.currentTimeMillis();
		Context context = getApplicationContext();
		CharSequence contentTitle = "TPad Connect";
		CharSequence contentText = "TPad Service Running";

		Intent notificationIntent = new Intent(this, TPadService.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
		Notification notification = new Notification(icon, tickerText, when);

		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		startForeground(1010, notification);

		super.onStartCommand(intent, flags, startId);

		return START_STICKY;
	}

}