/**
 * 
 * This is OpenTraining, an Android application for planning your your fitness training.
 * Copyright (C) 2012-2014 Christian Skubich
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package de.skubware.opentraining.activity.create_exercise;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.MenuItem.OnMenuItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;
import de.skubware.opentraining.R;
import de.skubware.opentraining.activity.start_training.SwipeDismissListViewTouchListener;
import de.skubware.opentraining.db.IDataProvider;

public class ImageFragment extends Fragment{
	/** Tag for logging*/
	private final String TAG = "ImageFragment";

	
	/** Uri of the image that is returned by the Intent */
	private Uri mTempImageUri = null;

	private ListView mImageListView;
	private ExerciseImageListAdapter mListAdapter;
	private Map<String, Bitmap> mNameImageMap = new HashMap<String, Bitmap>();
	
	public ImageFragment() {
	}
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }
    
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	    super.onCreateOptionsMenu(menu, inflater);
	    inflater.inflate(R.menu.create_exercise_image_fragment_menu, menu);
	    
		// configure menu_item_take_photo
		MenuItem menu_item_take_photo = (MenuItem) menu.findItem(R.id.menu_item_take_photo);
		menu_item_take_photo.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				dispatchTakePictureIntent();			    
				return true;
			}
		});
		
		// configure menu_item_chose_photo
		MenuItem menu_item_chose_photo = (MenuItem) menu.findItem(R.id.menu_item_chose_photo);
		menu_item_chose_photo.setOnMenuItemClickListener(new OnMenuItemClickListener() {
			public boolean onMenuItemClick(MenuItem item) {
				Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
				photoPickerIntent.setType("image/*");
				startActivityForResult(photoPickerIntent, CreateExerciseActivity.CHOSE_PICTURE);
				return true;
			}
		});
	}
	
	String mCurrentPhotoPath;

	/**
	 * Generates a unique filename and creates an empty file to use.
	 * 
	 * 
	 * @param internal For use within this app an internal folder is the right choice.
	 * If the Uri should be passed to another app(e.g. camera app) an external folder has to be created.
	 * Otherwise external apps cannot acces the folder.
	 * 
	 * @return
	 * @throws IOException
	 */
	private Uri createImageFile(boolean internal) throws IOException {
	    // Create an image file name
	    String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
	    String imageFileName = "JPEG_" + timeStamp + "_";
	    File customImageFolder;
	    if(internal){
	    	customImageFolder = new File(getActivity().getFilesDir().toString() + "/"
					+ IDataProvider.CUSTOM_IMAGES_FOLDER);   
	    }else{
	    	customImageFolder = new File(Environment.getExternalStoragePublicDirectory(
	    	          Environment.DIRECTORY_PICTURES), "OpenTraining");
	    }
		if(!customImageFolder.exists()){
			customImageFolder.mkdirs();
			Log.d(TAG, "Folder for custom exercise images does not exist, will create it now.");
		}

	    File image = File.createTempFile(
	        imageFileName,  /* prefix */
	        ".jpg",         /* suffix */
	        customImageFolder      /* directory */
	    );

	    // Save a file: path for use with ACTION_VIEW intents
	    mCurrentPhotoPath = "file:" + image.getAbsolutePath();
	    return Uri.fromFile(image);
	}
	
	private void dispatchTakePictureIntent() {
		Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
			try {
				mTempImageUri = createImageFile(false);
			} catch (IOException ex) {
				// Error occurred while creating the File
				Log.e(TAG, "Error creating image file", ex);
			}
			takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, mTempImageUri);
			startActivityForResult(takePictureIntent, CreateExerciseActivity.TAKE_PICTURE);
		}else{
			Log.e(TAG, "No camera activity for handling camera intent");
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View layout = inflater.inflate(R.layout.fragment_create_exercise_image_fragment, container, false);


		mImageListView = (ListView) layout.findViewById(R.id.listview_exercise_images);
		
		// set list adapter and empty list element
		mImageListView.setEmptyView(layout.findViewById(android.R.id.empty));
		mListAdapter = new ExerciseImageListAdapter(getActivity(), mNameImageMap);
		mImageListView.setAdapter(mListAdapter);
		
		
		SwipeDismissListViewTouchListener touchListener = new SwipeDismissListViewTouchListener(
				mImageListView,
				new SwipeDismissListViewTouchListener.OnDismissCallback() {
					@Override
					public void onDismiss(ListView listView,
							int[] reverseSortedPositions) {
						for (int position : reverseSortedPositions) {
							mListAdapter.remove(position);
						}
						mListAdapter.notifyDataSetChanged();
					}
				});
		mImageListView.setOnTouchListener(touchListener);	
		
		
		return layout;
	}
	

	

	@Override
	public void onActivityResult(int requestCode, int resultCode,
			Intent data) {
		Log.v(TAG, "onActivityResult(), requestCode=" + requestCode
				+ ", resultCode=" + resultCode);
		super.onActivityResult(requestCode, resultCode, data);
		
        if(resultCode != Activity.RESULT_OK){  
			Toast.makeText(getActivity(), getString(R.string.did_not_provide_image),
					Toast.LENGTH_SHORT).show();
			Log.i(TAG, getString(R.string.did_not_provide_image));
			return;
		}
		
        Uri selectedImage = null;
        Bitmap bitmap = null;
		switch (requestCode) {
		 case CreateExerciseActivity.CHOSE_PICTURE:
		            selectedImage = data.getData();
		            String[] filePathColumn = {MediaStore.Images.Media.DATA};

		            Cursor cursor = getActivity().getContentResolver().query(
		                               selectedImage, filePathColumn, null, null, null);
		            cursor.moveToFirst();

		            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
		            String filePath = cursor.getString(columnIndex);
		            cursor.close();


		            bitmap = BitmapFactory.decodeFile(filePath);
		           
		        break;
		case CreateExerciseActivity.TAKE_PICTURE:
			bitmap = BitmapFactory.decodeFile(mTempImageUri.getPath());
			break;
		default: 
			Log.e(TAG, "Unknown return code");
			return;
		}
		
		if(bitmap == null){
			Toast.makeText(getActivity(), getString(R.string.error_did_not_return_image),
					Toast.LENGTH_SHORT).show();
			
			Log.e(TAG, getString(R.string.error_did_not_return_image));
			return;
		}
		
		 // now save bitmap to app-private folder 
        FileOutputStream out = null;
        try {
            mTempImageUri = createImageFile(true);
            
        	out = new FileOutputStream(mTempImageUri.getPath());
        	bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (FileNotFoundException fnf) {
        	Log.e(TAG, "Copying image failed, file not found", fnf);
        } catch (IOException e) {
        	Log.e(TAG, "Copying image failed, IOException", e);
		} finally {
        	try {
        		out.close();
        	} catch (Exception ex) {
        		Log.e(TAG, "Stream closing failed", ex);
        	}
        }

        
        
		
		
		// continue processing bitmap
		String imageName = (new File(mTempImageUri.getPath())).getName();
		Log.v(TAG, "Added image " + imageName);
		this.mNameImageMap.put(imageName, bitmap);
		mListAdapter.notifyDataSetChanged();
		
		//Toast.makeText(getActivity(), selectedImage.toString(),
		//		Toast.LENGTH_LONG).show();
		
	}


	
	
	public Uri getImage(){
		return mTempImageUri;
	}



}