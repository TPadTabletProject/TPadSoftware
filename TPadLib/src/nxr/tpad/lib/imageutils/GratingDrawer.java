package nxr.tpad.lib.imageutils;

import nxr.tpad.lib.consts.TPadGrating;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

public class GratingDrawer {
	private Canvas mainCanvas;
	private Bitmap mainBitmap;
	private int mType;
	public boolean isNoisy = false;

	private String TAG = "Grating Drawer";

	public GratingDrawer(int type) {
		mType = type;
	}

	public Bitmap createGrating(int height, int width, int wavelength) {
		int[] tempLine = new int[width];
		mainBitmap = null;
		mainBitmap = Bitmap
				.createBitmap(width, height, Bitmap.Config.ARGB_8888);

		mainCanvas = new Canvas(mainBitmap);

		float spatialFreq = 1f / wavelength;

		float pixelVal = 0;

		switch (mType) {
		case TPadGrating.SINUSOID:

			if (isNoisy) {
				for (int i = 0; i < height; i++) {
					for (int j = 0; j < width; j++) {

						pixelVal = (float) ((Math.sin(2 * Math.PI * spatialFreq
								* j) + 1) * 1 / 2f);
						pixelVal *= Math.random();
						tempLine[j] = frictionToPixel(pixelVal);
					}

					mainBitmap.setPixels(tempLine, 0, width, 0, i, width, 1);
				}

			} else {

				for (int j = 0; j < width; j++) {

					pixelVal = (float) ((Math
							.sin(2 * Math.PI * spatialFreq * j) + 1) * 1 / 2f);

					tempLine[j] = frictionToPixel(pixelVal);
				}

				for (int i = 0; i < height; i++) {
					mainBitmap.setPixels(tempLine, 0, width, 0, i, width, 1);
				}
			}
			break;
		case TPadGrating.SQUARE:

			if (isNoisy) {
				for (int i = 0; i < height; i++) {
					for (int j = 0; j < width; j++) {

						pixelVal = (float) (((int) (Math.sin(2 * Math.PI
								* spatialFreq * j) + 1)));
						pixelVal *= Math.random();
						tempLine[j] = frictionToPixel(pixelVal);
					}

					mainBitmap.setPixels(tempLine, 0, width, 0, i, width, 1);
				}

			} else {

				for (int j = 0; j < width; j++) {

					pixelVal = (float) (((int) (Math.sin(2 * Math.PI
							* spatialFreq * j) + 1)));

					tempLine[j] = frictionToPixel(pixelVal);
				}

				for (int i = 0; i < height; i++) {
					mainBitmap.setPixels(tempLine, 0, width, 0, i, width, 1);
				}
			}
			break;
		}

		return mainBitmap;

	}

	private int frictionToPixel(float val) {
		float[] hsv = { 0, 0, val };
		return Color.HSVToColor(hsv);

	}

	public void destroy() {

		mainBitmap.recycle();
	}

}
