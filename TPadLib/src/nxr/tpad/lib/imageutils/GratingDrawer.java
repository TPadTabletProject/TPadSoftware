package nxr.tpad.lib.imageutils;

import ioio.lib.spi.Log;
import nxr.tpad.lib.consts.TPadGrating;
import android.graphics.Bitmap;
import android.graphics.Color;

public class GratingDrawer {
	private Bitmap mainBitmap;
	private int mType;
	
	private String TAG = "Grating Drawer";

	private boolean isNoisy = false;

	public GratingDrawer(int type) {
		mType = type;
	}

	public Bitmap createGrating(int height, int width, int wavelength) {
		int[] tempLine = new int[width];
		mainBitmap = null;
		mainBitmap = Bitmap.createBitmap(width, height,Bitmap.Config.ARGB_8888);
		
		float spatialFreq = 1f/wavelength;
		
		float pixelVal = 0;
		
		switch (mType) {
		case TPadGrating.SINUSOID:

			for(int j=0; j<width; j++){
				
				pixelVal = (float) ((Math.sin(2*Math.PI*spatialFreq*j)+1)*1/2f);
				
				tempLine[j] = frictionToPixel(pixelVal);
			}
			
			
			for(int i=0; i<height; i++){
				mainBitmap.setPixels(tempLine, 0, width, 0, i, width, 1);
			}
			
			
			
			break;
			case TPadGrating.SQUARE:

			for(int i=0; i<height; i++){
				
				for(int j=0; j<width; j++){
					
					pixelVal = (float) (((int)(Math.sin(2*Math.PI*spatialFreq*j)+1)));
							
					if(isNoisy){						
						pixelVal = (float) (pixelVal*Math.random());
					}
					
					//Log.i(TAG, "Pixel Val:" + String.valueOf(pixelVal));
					
					tempLine[j] = frictionToPixel(pixelVal);
				}
				
				mainBitmap.setPixels(tempLine, 0, width, 0, i, width, 1);
			}
			
			
			
			break;
		}

		return mainBitmap;

	}
	
	private int frictionToPixel(float val){
		float[] hsv = {0, 0, val};
		return Color.HSVToColor(hsv);
		
	}
	
	public void destroy(){
		
		mainBitmap.recycle();
	}

}
