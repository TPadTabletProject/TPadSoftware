package com.example.pictureloader;

import java.util.Locale;
import nxr.tpad.lib.TPad;
import nxr.tpad.lib.views.FrictionMapView;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class PictureLoaderFragment extends Fragment {

	private TPad mTpad;

	private FrictionMapView fricView;
	
	private Bitmap localBit;

	public PictureLoaderFragment() {
		// Empty constructor required for fragment subclasses
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_layout, container, false);

		// int imageId = getResources().getIdentifier(planet.toLowerCase(Locale.getDefault()), "drawable", getActivity().getPackageName());

		// Link friction view to .xml file
		fricView = (FrictionMapView) rootView.findViewById(R.id.view1);

		// Load in the image stored in the drawables folder
		Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.demo);

		fricView.setTpad(mTpad);
		
		// Set the friction data bitmap to the test image
		fricView.setDataBitmap(defaultBitmap);

		return rootView;
	}

	public void setTpad(TPad tpad) {
		mTpad = tpad;
		// Link local tpad object to the FrictionMapView
		
	}
	
	public void setBitmap(Bitmap bit){
		
		fricView.setDataBitmap(bit);
	}
}
