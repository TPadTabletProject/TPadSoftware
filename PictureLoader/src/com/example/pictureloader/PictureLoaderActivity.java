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

package com.example.pictureloader;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;

import nxr.tpad.lib.TPad;
import nxr.tpad.lib.TPadImpl;
import android.app.Activity;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.pictureloader.R;

public class PictureLoaderActivity extends Activity {

	private String[] mOptionTitles;
	private DrawerLayout mDrawerLayout;
	private ListView mDrawerList;

	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
	
	private static final int REQ_CODE_PICK_IMAGE = 2;
	//Android camera data
    private static final int REQUEST_TAKE_PHOTO = 1;
    private static final int TAKE_PICTURE = 0;
    
    private Uri mUri;
    
    private View decorView;
    

	private PictureLoaderFragment mFragment;

	// TPad object defined in TPadLib
	private static TPad mTpad;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Set the content of the screen to the .xml file that is in the layout folder
		setContentView(R.layout.pictureloader_main);

		mOptionTitles = getResources().getStringArray(R.array.options_array);
		
		decorView = getWindow().getDecorView();
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerList = (ListView) findViewById(R.id.left_drawer);

		// Set the adapter for the list view
		mDrawerList.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mOptionTitles));
		// Set the list's click listener
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

		// set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
       

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
                ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        if (savedInstanceState == null) {
            selectItem(0);
        }
		       
        
		// Load new tpad object from TPad Implementation Library
		mTpad = new TPadImpl(this);
		
		mFragment = new PictureLoaderFragment();
		Bundle args = new Bundle();
		mFragment.setArguments(args);

		// Insert the fragment by replacing any existing fragment
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.content_frame, mFragment).commit();
		mFragment.setTpad(mTpad);
		
		
		decorView.setOnSystemUiVisibilityChangeListener
		        (new View.OnSystemUiVisibilityChangeListener() {
		    @Override
		    public void onSystemUiVisibilityChange(int visibility) {
		        // Note that system bars will only be "visible" if none of the
		        // LOW_PROFILE, HIDE_NAVIGATION, or FULLSCREEN flags are set.
		        if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
		        	decorView.setSystemUiVisibility(
		                    View.SYSTEM_UI_FLAG_VISIBLE);
		        	
		        } else {
		            // TODO: The system bars are NOT visible. Make any desired
		            // adjustments to your UI, such as hiding the action bar or
		            // other navigational controls.
		        }
		    }
		});
		
		
		
		
	}

	
    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
        

        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mOptionTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);
        
        switch(position) {
        case 0:
           	goFullscreen();
        	break;
		case 1:
			selectPicture();
        	break;
		case 2:
			dispatchTakePictureIntent();
			break;
		
    	default:
    		
    		break;
}
        
        
    }
        

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getActionBar().setTitle(mTitle);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during
     * onPostCreate() and onConfigurationChanged()...
     */

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggls
        mDrawerToggle.onConfigurationChanged(newConfig);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
          return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }


	public void selectPicture(){
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");

		Intent chooser = Intent.createChooser(intent, "Choose a Visual Layer");
		startActivityForResult(chooser, REQ_CODE_PICK_IMAGE);
		
	}
	
	 /*
     * Dispatch an intent to take the photo
     */
    public void dispatchTakePictureIntent() {
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
    
    public void goFullscreen(){
    	
    		decorView.setSystemUiVisibility(
    			View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE);
    }
    
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		// Parse the activity result
		switch (requestCode) {
		
		case TAKE_PICTURE:
            if (resultCode == Activity.RESULT_OK) {
                getContentResolver().notifyChange(mUri, null);
                ContentResolver cr = getContentResolver();
                try {
                    Bitmap mPhoto = android.provider.MediaStore.Images.Media.getBitmap(cr, mUri);
                    //Reset the bitmap of the FrictionMapView
                    mFragment.setBitmap(mPhoto);
                    
                    mPhoto.recycle();
                } catch (Exception e) {
                     Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                   }
              }
        
		case REQ_CODE_PICK_IMAGE:
			if (resultCode == RESULT_OK && data != null) {
				Uri bitmapUri = data.getData();

				try {

					// set the display bitmap based on the bitmap we just got
					// back from our intent
					// Bitmap b = Media.getBitmap(getContentResolver(), bitmapUri);
					Bitmap b = getBitmapFromUri(bitmapUri);
				
					mFragment.setBitmap(b);
					

					b.recycle();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				break;

			}
		}
	}

	private Bitmap getBitmapFromUri(Uri uri) throws IOException {
		ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
		FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
		Bitmap image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
		parcelFileDescriptor.close();
		return image;
	}
	
    
	@Override
	protected void onDestroy() {
		mTpad.disconnectTPad();
		super.onDestroy();
	}

}