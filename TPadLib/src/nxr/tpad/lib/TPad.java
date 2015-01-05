/*
 * Copyright 2014 Craig Shultz. All rights reserved.
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

/**
 * This interface provides the basic TPad functions
 * <p>
 * All TPad enabled Android Applications must use this class to interface with the TPad. The TPad itself is managed through the usage of the {@link TPadService}, which runs in the background and
 * manages the connection of the phone to the TPad electronics board.
 * 
 * Typical usage:
 * 
 * <pre>
 * (Load TPad on startup)
 * TPad tpad = new TPadImpl(this);
 * 
 * (Set friction value to half total value)
 * tpad.sendFriction(0.5f);
 * 
 * (disconnect TPad when activity is destroyed)
 * tpad.disconnectTPad();
 * 
 * </pre>
 * 
 */

public interface TPad {

	/**
	 * Send new friction level value to TPad
	 * <p>
	 * The TPad can change the level of friction between the user's finger and the screen. Calling this function will set a new level of friction on the output of the TPad screen.
	 * <p>
	 * Note that a value of 1 will turn the TPad on to full power, so be careful leaving the screen on for long periods of time, or when the user is not touching the screen.
	 * 
	 * @param f
	 *            Friction level to be output to the screen. 1 corresponds to lowest friction possible (slipperiest screen, turns TPad fully on) 0 corresponds to highest friction possible (stickiest
	 *            screen, turns TPad off!) Any value in-between 0 and 1 will be proportionally sticky or slippery
	 * 
	 */
	public void sendFriction(float f);

	/**
	 * Send a buffer of friction values to be streamed to the TPad
	 * <p>
	 * Same as {@link #sendFriction(float f)}, but with a large buffer of friction values. Friction values will be played back to the TPad at a rate of {@link TPadService.OUTPUT_SAMPLE_RATE}.
	 * <p>
	 * For example: For a TPad that is playing back values at 6250 Hz, an array of 625 values will be played for 100 mS. The internal {@link #TPadService} buffer can, however, only hold enough data to
	 * be played back for 1 second. For longer playback times, send data to the Service in chunks of less than a second.
	 * <p>
	 * For best user experience, it is recommended to send data to the TPad as fast as possible. Therefore, you should send friction buffer chunks to the TPad every 10mS or so.
	 * <p>
	 * Note that a value of 1 will turn the TPad on to full power, so be careful leaving the screen on for long periods of time, or when the user is not touching the screen.
	 * 
	 * @param buffArray
	 *            The array of friction values to be sent to the TPad and played back at the output sample rate. Any new calls to sendFrictionBuffer will overwrite what is currently in the buffer to
	 *            playback.
	 */
	public void sendFrictionBuffer(float[] buffArray);

	/**
	 * Start a vibration to be played to the output of the TPad.
	 * <p>
	 * This function will output a single freqeuncy vibration-like signal to the TPad surface when called. The vibration will continue to play unless overwritten by another TPad command, or the TPad
	 * is turned off.
	 * <p>
	 * The type of stimulus this gives to the user is, in general, very similar to the onboard vibration motor that is common in most phones. With the TPad, however, this stimulus is felt directly on
	 * the fingertip, and not throughout the entire phone.
	 * <p>
	 * In general, this type of feedback is NOT what the TPad is specifically designed for, but it is possible to do. In most cases, it is recommended you use the TPad to change overall friction value
	 * on the screen using {@link #sendFriction(float f)}, {@link #sendFrictionBuffer(float[] buffArray)}, or a {@link FrictionMapView}; *
	 * 
	 * 
	 * @param type
	 *            Vibration waveform to be played. See {@link TPadVibration} for choices
	 * @param freq
	 *            Frequency of vibration waveform in Hz. Generally, use 0-1000Hz, and 200Hz for 'strongest' stimulus.
	 * @param amp
	 *            Overall amplitude (strength) of vibration. 0 is none, 1 is full.
	 */
	public void sendVibration(int type, float freq, float amp);

	/**
	 * Turns TPad off
	 * <p>
	 * Same as sending a 0 friction value. See {@link #sendFriction(float f)}
	 * 
	 */
	public void turnOff();

	public void refreshFreq();
	
	public void refreshScale();

	/**
	 * Tell TPad to operate at new carrier frequency
	 * <p>
	 * Each TPad must operate at a specific frequency in order to get the highest level of friction reduction. This frequency is unique to each TPad, but generally in the 30,000 Hz range.
	 * 
	 * @param freq
	 *            Carrier Frequency to change to (in Hz)
	 * 
	 */
	public void sendNewFreq(int f);
	
	public void calibrate();
	
	public boolean getTpadStatus();

	public boolean getBound();

	public int getLocalFreq();
	
	public float getLocalScale();

	public void sendNewScale(float scale);

	/**
	 * Unbind from TPad Connect Service
	 * <p>
	 * Call this function when you are finished using the TPad for good. I.E. in the onDestroy() callback of the Activity class.
	 * 
	 */
	public void disconnectTPad();

}