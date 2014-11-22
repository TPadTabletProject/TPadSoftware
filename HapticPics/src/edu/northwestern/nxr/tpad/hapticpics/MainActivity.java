package edu.northwestern.nxr.tpad.hapticpics;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import nxr.tpad.lib.TPad;
import nxr.tpad.lib.TPadImpl;
import nxr.tpad.lib.views.FrictionMapView;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	FrictionMapView fricWindow; //Image -> Haptics converter
	TextView mTextView;
	private Bitmap mPhoto;
	Button mButton;
	TPad mTpad; //TPad object for linking FrictionMapView to the screen
	
	//Android camera data
    static final int REQUEST_TAKE_PHOTO = 1;
    private static final int TAKE_PICTURE = 0;
    private Uri mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        //TPad is an interface, TPadImpl is the implementation of it
        mTpad = new TPadImpl(this);
        
        //FrictionMapView operates similarly to an ImageView, first you connect it to the layout object 
        fricWindow = (FrictionMapView) findViewById(R.id.frictionView1);
        fricWindow.setTpad(mTpad); //Set the TPad object to send data to
        
        //Initialize the app with a test image in the FrictionMapView
		Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.testimage);
		fricWindow.setDataBitmap(defaultBitmap);
    }
    
    /*
     * Dispatch an intent to take the photo
     */
    public void dispatchTakePictureIntent(View v) {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            Intent i = new Intent("android.media.action.IMAGE_CAPTURE");
            File f = new File(Environment.getExternalStorageDirectory(),  "photo.jpg");
            i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
            mUri = Uri.fromFile(f);
            startActivityForResult(i, TAKE_PICTURE);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
        case TAKE_PICTURE:
            if (resultCode == Activity.RESULT_OK) {
                getContentResolver().notifyChange(mUri, null);
                ContentResolver cr = getContentResolver();
                try {
                    mPhoto = android.provider.MediaStore.Images.Media.getBitmap(cr, mUri);
                    //Reset the bitmap of the FrictionMapView
                    ((FrictionMapView)findViewById(R.id.frictionView1)).setDataBitmap(mPhoto);
                } catch (Exception e) {
                     Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                   }
              }
        }
    }
}
