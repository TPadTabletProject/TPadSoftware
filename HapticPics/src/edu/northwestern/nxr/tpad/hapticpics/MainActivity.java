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

	FrictionMapView fricWindow;
	TextView mTextView;
	private Bitmap mPhoto;
	Button mButton;

	TPad mTpad;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        mTpad = new TPadImpl(this);
        
        fricWindow = (FrictionMapView) findViewById(R.id.imageView1);
        fricWindow.setTpad(mTpad);
        
		Bitmap defaultBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.testimage);
		fricWindow.setDataBitmap(defaultBitmap);
		/*
		mButton = (Button) findViewById(R.id.captureRear);
		mButton.setOnClickListener(new OnClickListener() {
	         @Override
	         public void onClick(View v) {
	            dispatchTakePictureIntent(v);
	         }
	      	});
		*/
		//mTextView = (TextView) findViewById(R.id.textView1);
		/*
		mTextView.setOnClickListener(new OnClickListener() {
         @Override
         public void onClick(View v) {
            dispatchTakePictureIntent();
         }
      	});
		*/
    }
    
    static final int REQUEST_TAKE_PHOTO = 1;
    private static final int TAKE_PICTURE = 0;
    private Uri mUri;
    
    public void dispatchTakePictureIntent(View v) {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);//MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
        	
        	//new code
            Intent i = new Intent("android.media.action.IMAGE_CAPTURE");
            File f = new File(Environment.getExternalStorageDirectory(),  "photo.jpg");
            i.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
            mUri = Uri.fromFile(f);
            startActivityForResult(i, TAKE_PICTURE);
        	
        	/*
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(getApplicationContext(), "Error has occured: " + ex, Toast.LENGTH_SHORT).show();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                //takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
            	takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
            */
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
                 ((FrictionMapView)findViewById(R.id.imageView1)).setDataBitmap(mPhoto);
                } catch (Exception e) {
                     Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                   }
              }
        }
    	/*
    	Log.i("TPad", "Here");
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
        		Log.i("TPad", "step 2");
        		galleryAddPic();
        		setPic();
        		/*

            	Toast.makeText(getApplicationContext(), "Bundle: " + data.getExtras(), Toast.LENGTH_SHORT).show();
                //Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) data.getExtras().get("data");
                fricWindow.setDataBitmap(imageBitmap);
                //
        	
        }
    	*/
    }
    
    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",         /* suffix */
            storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        Log.i("TPad", "Path: " + mCurrentPhotoPath); 
        return image;
    }
    
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
    
    private void setPic() {
        // Get the dimensions of the View
        int targetW = fricWindow.getWidth();
        int targetH = fricWindow.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        Log.i("TPad", "Here: " + bitmap);
        fricWindow.setDataBitmap(bitmap);
        
    }
}
