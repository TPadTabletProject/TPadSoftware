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

import nxr.tpad.lib.consts.TPadMessage;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

public class TPadImpl implements TPad {

	// SINUSOID, SQUARE, SAWTOOTH, TRIANGLE, RANDOM
	public final int iSINUSOID = 1;
	public final int iSQUARE = 2;
	public final int iSAWTOOTH = 3;
	public final int iTRIANGLE = 4;
	public final int iRANDOM = 5;

	Messenger myService = null;
	public boolean isBound;

	ResponseHandler mResponseHandler;

	public Context mContext;

	public Integer localFreq = 0;
	public Integer localStatus = 0;
	public Float localScale = 0f;

	public TPadImpl(Context context) {
		mContext = context;

		Intent intent = new Intent();
		intent.setAction("TPS");
		mContext.bindService(intent, myConnection, Context.BIND_AUTO_CREATE);

		mResponseHandler = new ResponseHandler();
		
	}

	public class ResponseHandler extends Handler {
		@Override
		public void handleMessage(Message msg) {
			int respCode = msg.what;
			Bundle data;
			switch (respCode) {

			case TPadMessage.GET_TPAD_FREQ:
				data = msg.getData();
				setLocalFreq(data.getInt("Freq"));
				break;
			case TPadMessage.CHECK_TPAD:
				data = msg.getData();
				setLocalStatus(data.getInt("connected"));
				break;
			case TPadMessage.GET_TPAD_SCALE:
				data = msg.getData();
				setLocalScale(data.getFloat("Scale"));
				break;

			default:
				Log.i("TPad", "msg not recognized");
				break;
			}

		}
	}

	public void refreshFreq() {
		if (!isBound)
			return;
		Message msg = Message.obtain(null, TPadMessage.GET_TPAD_FREQ);
		msg.replyTo = new Messenger(mResponseHandler);
		Bundle bundle = new Bundle();
		bundle.putInt("getfreq", TPadMessage.GET_TPAD_FREQ);

		msg.setData(bundle);

		try {
			myService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}
	
	public void refreshScale() {
		if (!isBound)
			return;
		Message msg = Message.obtain(null, TPadMessage.GET_TPAD_SCALE);
		msg.replyTo = new Messenger(mResponseHandler);
		Bundle bundle = new Bundle();
		bundle.putInt("getscale", TPadMessage.GET_TPAD_SCALE);

		msg.setData(bundle);

		try {
			myService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendNewFreq(int f) {
		if (!isBound)
			return;
		Message msg = Message.obtain(null, TPadMessage.SET_TPAD_FREQ);
		msg.replyTo = new Messenger(mResponseHandler);
		Bundle bundle = new Bundle();
		bundle.putInt("newFreq", f);

		msg.setData(bundle);

		try {
			myService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		//showNotification();
	}
	
	@Override
	public void sendNewScale(float scale) {
		if (!isBound)
			return;
		Message msg = Message.obtain(null, TPadMessage.SET_TPAD_SCALE);
		msg.replyTo = new Messenger(mResponseHandler);
		Bundle bundle = new Bundle();
		bundle.putFloat("newScale", scale);

		msg.setData(bundle);

		try {
			myService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean getBound() {
		return isBound;
	}
	
	@Override
	public boolean getTpadStatus() {
		if (!isBound)
			return false;
		Message msg = Message.obtain(null, TPadMessage.CHECK_TPAD);
		msg.replyTo = new Messenger(mResponseHandler);
		Bundle bundle = new Bundle();
		bundle.putInt("check", TPadMessage.CHECK_TPAD);

		msg.setData(bundle);

		try {
			myService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}

		boolean status = false;
		synchronized (localStatus) {
			if (localStatus == 1)
				status = true;
			else
				status = false;
		}
		return status;
	}

	@Override
	public int getLocalFreq() {
		return localFreq;
	}

	public void setLocalStatus(int stat) {
		synchronized (localStatus) {
			localStatus = stat;
		}

	}

	public void setLocalFreq(int f) {
		synchronized (localFreq) {
			localFreq = f;
		}
	}
	
	public void setLocalScale(float s) {
		synchronized (localScale) {
			localScale = s;
		}
		
	}
	
	@Override
	public float getLocalScale() {
		float s;
		synchronized (localScale) {
			s = localScale;
		}
		return s;
	}

	
	

	public void sendFriction(float f) {
		// Log.i("tPhone", "send TPad: " + f);
		// Log.i("tPhone", "isBound: " + isBound);

		if (!isBound)
			return;
		Message msg = Message.obtain(null, TPadMessage.SEND_TPAD);
		msg.replyTo = new Messenger(mResponseHandler);
		Bundle bundle = new Bundle();
		bundle.putFloat("f", f);
		msg.setData(bundle);
		// Log.i("tPhone", "Bundle ready, sending message");

		try {
			// Log.i("tPhone", "Sending TPad Message (2)");
			myService.send(msg);
			// Log.i("tPhone", "Message Return(2)");
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendFrictionBuffer(float[] buffArray) {
		Log.i("TPad2", "SendTPadBuffer calling event");
		if (!isBound)
			return;
		Message msg = Message.obtain(null, TPadMessage.SEND_TPAD_BUFFER);
		msg.replyTo = new Messenger(mResponseHandler);
		Bundle bundle = new Bundle();
		bundle.putFloatArray("buffArray", buffArray);
		msg.setData(bundle);
		try {
			myService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void sendVibration(int type, float freq, float amp) {
		if (!isBound)
			return;
		Message msg = Message.obtain(null, TPadMessage.SEND_TPAD_TEXTURE);

		msg.replyTo = new Messenger(mResponseHandler);
		Bundle bundle = new Bundle();
		bundle.putInt("type", type);
		bundle.putFloat("freq", freq);
		bundle.putFloat("amp", amp);
		msg.setData(bundle);
		try {
			// Log.i("TPad", "send message");
			myService.send(msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}

	public void disconnectTPad() {
		mContext.unbindService(myConnection);

	}

	public ServiceConnection myConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			myService = new Messenger(service);
			isBound = true;
			Log.i("tPhone", "isBound = " + isBound);
			refreshFreq();
		}

		public void onServiceDisconnected(ComponentName className) {
			myService = null;
			isBound = false;
		}
	};

	private NotificationManager mNotificationManager;

	private boolean showNotification() {
		/*
		 * String ns = Context.NOTIFICATION_SERVICE; mNotificationManager = (NotificationManager) getSystemService(ns);
		 * 
		 * int icon = R.drawable.ic_launcher; CharSequence tickerText = "TPad Service Launched"; long when = System.currentTimeMillis(); Context context = getApplicationContext(); CharSequence
		 * contentTitle = "TPad Connect"; CharSequence contentText = "This TPad's Frequency: " + localFreq;
		 * 
		 * Intent notificationIntent = new Intent(this, TPadService.class); PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0); Notification notification = new
		 * Notification(icon, tickerText, when);
		 * 
		 * notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent); final int HELLO_ID = 1;
		 * 
		 * mNotificationManager.notify(HELLO_ID, notification);
		 */
		return true;
	}

	@Override
	public void turnOff() {
		sendFriction(0f);		
	}


}