package edu.purdue.combinekarttruck;

/*
 * The activity for truck.
 *
 * @author: Yaguang Zhang
 *
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import edu.purdue.combinekarttruck.utils.Utils;

public class TruckActivity extends BasicLoggingActivity {
	// For the taking a picture function.
	static final int REQUEST_TAKE_PHOTO = 1;
	private File tempImageFile;
	private File photoFile;
	private Uri tempImageUri;

	private Boolean FLAG_HIDE_PHOTO_FUNCTION = true;

	@Override
	public String getPartialLogFilePath() {
		return this.getSharedPref().getString(Utils.SAVED_FOLDER_PATH,
				null) + getString(R.string.gps_log_file_path_truck);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		actionBarActivityOnCreate(savedInstanceState);

		setContentView(R.layout.activity_truck);
		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}

		setLogStateFlag(false);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Hide the views for taking a picture if necessary.
		if(FLAG_HIDE_PHOTO_FUNCTION) {
			findViewById(R.id.ButtonTakePicture).setVisibility(View.INVISIBLE);
			findViewById(R.id.imageViewTicket).setVisibility(View.INVISIBLE);
		}
	}

	@Override
	public String getLoginType() {
		return getString(R.string.vehicle_truck);
	}

	@Override
	public void setBackgroundColor() {
		findViewById(R.id.textViewVehicleTypeLabel).getRootView()
				.setBackgroundColor(
						getResources().getColor(
								MainLoginActivity.COLOR_ACTIVITY_TRUCK));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.truck, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_truck,
					container, false);
			return rootView;
		}
	}

	public void takePicture(View view) {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		// Ensure that there's a camera activity to handle the intent
		if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
			// Create an image file name
			String imageFileName = "temp_ticket_"
					+ getFormatterUnderline().format(new Date()) + "_";
			File storageDir = Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			tempImageFile = null;
			try {
				tempImageFile = File.createTempFile(imageFileName, /* prefix */
						".jpg", /* suffix */
						storageDir /* directory */
				);
			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						getString(R.string.truck_temp_image_create_error));
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("TruckTakePicureCreateTempFile", e.toString());
			}

			// Continue only if the File was successfully created
			if (tempImageFile != null) {
				tempImageUri = Uri.fromFile(tempImageFile);
				takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
						tempImageUri);
				startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
			} else {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						"Image file creation failed.");
			}
		} else {
			MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
					"No camera activity available.");
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (android.os.Environment.getExternalStorageState().equals(
				android.os.Environment.MEDIA_MOUNTED)) {

			switch (requestCode) {
			case REQUEST_TAKE_PHOTO:
				if (resultCode == Activity.RESULT_OK) {

					// Save the photo.
					if (tempImageFile != null) {
						String imageFileName = "ticket_"
								+ getFormatterUnderline().format(new Date())
								+ ".jpg";
						photoFile = new File(getLogFilesPath(), imageFileName);

						try {
							copyFile(tempImageFile, photoFile);
							Toast.makeText(this, photoFile.toString(),
									Toast.LENGTH_LONG).show();
						} catch (IOException e) {
							photoFile.delete();
							Log.e("TruckSavePhoto", e.toString());
						}
					}

					// Make the new photo available for other apps.
					Intent mediaScanIntent = new Intent(
							Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
					Uri contentUri = Uri.fromFile(photoFile);
					mediaScanIntent.setData(contentUri);
					this.sendBroadcast(mediaScanIntent);

					getContentResolver().notifyChange(tempImageUri, null);
					ImageView imageView = (ImageView) findViewById(R.id.imageViewTicket);
					ContentResolver cr = getContentResolver();
					Bitmap bitmap;
					try {
						bitmap = android.provider.MediaStore.Images.Media
								.getBitmap(cr, tempImageUri);
						imageView.setImageBitmap(bitmap);
					} catch (Exception e) {
						MainLoginActivity
								.toastStringTextAtCenterWithLargerSize(
										this,
										getString(R.string.truck_image_file_load_error));
						Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT)
								.show();
						Log.e("Camera", e.toString());
					}
				}
			}
		}
	}

	public static void copyFile(File source, File destination)
			throws IOException {
		byte[] buffer = new byte[100000];

		BufferedInputStream bufferedInputStream = null;
		BufferedOutputStream bufferedOutputStream = null;
		try {
			bufferedInputStream = new BufferedInputStream(new FileInputStream(
					source));
			bufferedOutputStream = new BufferedOutputStream(
					new FileOutputStream(destination));
			int size;
			while ((size = bufferedInputStream.read(buffer)) > -1) {
				bufferedOutputStream.write(buffer, 0, size);
			}
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				if (bufferedInputStream != null) {
					bufferedInputStream.close();
				}
				if (bufferedOutputStream != null) {
					bufferedOutputStream.flush();
					bufferedOutputStream.close();
				}
			} catch (IOException ioe) {
				throw ioe;
			}
		}
	}
}
