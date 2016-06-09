package edu.purdue.combinekarttruck;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

/*
 * The activity for GPS data recording only.
 * 
 * @author: Yaguang Zhang
 * Reference: Stephan Williams' LogAccelGpsActivity project.
 * Available at https://github.com/OATS-Group/android-logger
 */

public class BasicGpsLoggingActivity extends ActionBarActivity implements
		LocationListener {
	private boolean LOG_STATE_FLAG = true;

	private String loginId, logFilePath, logFileNameGps, logFileNameState;

	private File mFileGps, mFileState;
	private FileWriter mLogGps, mLogState;

	private LocationManager mLocationManager;
	private TextView textViewTime;
	private SimpleDateFormat formatterUnderline;
	private SimpleDateFormat formatterClock;

	// Preference file used to store the info.
	private SharedPreferences sharedPref;

	public void setLogStateFlag(boolean flag) {
		LOG_STATE_FLAG = flag;
	}

	public String getLogFileString() {
		return logFilePath;
	}

	public FileWriter getMLogGPS() {
		return mLogGps;
	}

	public FileWriter getMLogState() {
		return mLogState;
	}

	public SimpleDateFormat getFormatterClock() {
		return formatterClock;
	}

	public SharedPreferences getSharedPref() {
		return sharedPref;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		actionBarActivityOnCreate(savedInstanceState);

		setContentView(R.layout.activity_basic_gps_logging);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
	}

	public void actionBarActivityOnCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onStart() {
		super.onStart();

		/**
		 * Initialization.
		 */

		// Set the sharedPref if we haven't done so.
		if (sharedPref == null) {
			sharedPref = this.getSharedPreferences(
					getString(R.string.shared_preference_file_key),
					Context.MODE_PRIVATE);
		}

		// Load the history ID record if it's initializing.
		if (loginId == null) {
			loginId = sharedPref.getString(getString(R.string.saved_last_id),
					null);
		}

		// Create directories if necessary.
		if (logFilePath == null) {
			logFilePath = Environment.getExternalStorageDirectory()
					+ getPartialLogFilePath() + loginId;

			if (Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				File logFileDirFile = new File(logFilePath);

				logFileDirFile.mkdirs();

				if (!logFileDirFile.isDirectory()) {
					MainLoginActivity.toastStringTextAtCenterWithLargerSize(
							this,
							getString(R.string.gps_log_file_dir_create_error));
				}
			} else {
				MainLoginActivity
						.toastStringTextAtCenterWithLargerSize(
								this,
								getString(R.string.gps_log_file_external_storage_error));
			}
		}

		createLogFiles();

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		mLocationManager.requestLocationUpdates(
				LocationManager.NETWORK_PROVIDER, 0, 0, this);
		mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
				0, 0, this);
	}

	public String getLoginType() {
		return getString(R.string.vehicle_kart);
	}

	public String getPartialLogFilePath() {
		return getString(R.string.gps_log_file_path_kart);
	}

	public String getLogFilePath() {
		return logFilePath;
	}

	public SimpleDateFormat getFormatterUnderline() {
		return formatterUnderline;
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Start the timer textView.
		textViewTime = ((TextView) findViewById(R.id.textViewTime));
		formatterClock = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",
				java.util.Locale.getDefault());

		Thread threadTimer = new Thread() {
			@Override
			public void run() {
				try {
					while (!isInterrupted()) {
						Thread.sleep(100);
						runOnUiThread(new Runnable() {
							@Override
							public void run() {
								Date date = new Date();
								textViewTime.setText("Time: "
										+ formatterClock.format(date));
							}
						});
					}
				} catch (Exception e) {
					Log.e("BasicGpsLoggingTimerThreadInterrupted", e.toString());
				}
			}
		};
		threadTimer.start();

		setBackgroundColor();

	}

	public void setBackgroundColor() {
		findViewById(R.id.textViewVehicleTypeLabel).getRootView()
				.setBackgroundColor(
						getResources().getColor(
								MainLoginActivity.COLOR_BASIC_GPS_LOGGING));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_activity_login, menu);
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
			View rootView = inflater.inflate(
					R.layout.fragment_basic_gps_logging, container, false);
			return rootView;
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		TextView ckt = ((TextView) findViewById(R.id.textViewCktState));
		ckt.setText(getString(R.string.ckt_state_loading));

		mLocationManager.removeUpdates(this);

		closeLogFiles();
	}

	@Override
	public void onLocationChanged(Location location) {
		TextView ckt = ((TextView) findViewById(R.id.textViewCktState));

		TextView latGps = ((TextView) findViewById(R.id.textViewLatGps));
		TextView lonGps = ((TextView) findViewById(R.id.textViewLonGps));
		TextView altitudeGps = ((TextView) findViewById(R.id.textViewAltitudeGps));
		TextView speedGps = ((TextView) findViewById(R.id.textViewSpeedGps));
		TextView bearingGps = ((TextView) findViewById(R.id.textViewBearingGps));
		TextView accuracyGps = ((TextView) findViewById(R.id.textViewAccuracyGps));

		ckt.setText(getString(R.string.ckt_state_recording));

		latGps.setText("Lat: " + location.getLatitude());
		lonGps.setText("Lon: " + location.getLongitude());
		altitudeGps.setText("Altitude: " + location.getAltitude());
		speedGps.setText("Speed: " + location.getSpeed());
		bearingGps.setText("Bearing: " + location.getBearing());
		accuracyGps.setText("Accuracy: " + location.getAccuracy());

		long cur_time = System.currentTimeMillis();

		try {
			mLogGps.write(formatterClock.format(cur_time) + ", " + cur_time
					+ ", " + location.getLatitude() + ", "
					+ location.getLongitude() + ", " + location.getAltitude()
					+ ", " + location.getSpeed() + ", " + location.getBearing()
					+ ", " + location.getAccuracy() + "\n");
		} catch (IOException e) {
			MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
					getString(R.string.gps_log_file_write_error));
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			Log.e("BasicGpsLoggingLocationChangedWrite", e.toString());
		}
	}

	@Override
	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	public void createLogFiles() {
		if (logFileNameGps == null) {
			formatterUnderline = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss",
					java.util.Locale.getDefault());
			Date date = new Date();
			logFileNameGps = "gps_" + formatterUnderline.format(date) + ".txt";
		}

		try {
			mFileGps = new File(logFilePath, logFileNameGps);

			mLogGps = new FileWriter(mFileGps);
			mLogGps.write("% " + getLoginType() + " " + loginId + ": "
					+ logFileNameGps + "\n"
					+ getString(R.string.gps_log_file_head));

		} catch (IOException e) {
			MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
					getString(R.string.gps_log_file_create_error));
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			Log.e("BasicGpsLoggingCreateLogAndWrite", e.toString());
		}

		if (LOG_STATE_FLAG) {
			if (logFileNameState == null) {
				formatterUnderline = new SimpleDateFormat(
						"yyyy_MM_dd_HH_mm_ss", java.util.Locale.getDefault());
				Date date = new Date();
				logFileNameState = "state_" + formatterUnderline.format(date)
						+ ".txt";
			}

			try {
				mFileState = new File(logFilePath, logFileNameState);

				mLogState = new FileWriter(mFileState);
				mLogState.write("% " + getLoginType() + " " + loginId + ": "
						+ logFileNameState + "\n");

			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						getString(R.string.gps_log_file_create_error));
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("BasicStateLoggingCreateLogAndWrite", e.toString());
			}
		}
	}

	public void closeLogFiles() {
		try {
			Date date = new Date();
			mLogGps.write("% Stopped at " + formatterClock.format(date) + "\n");

			if (LOG_STATE_FLAG) {
				mLogState.write("% Stopped at " + formatterClock.format(date)
						+ "\n");
			}
		} catch (IOException e) {
			MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
					getString(R.string.gps_log_file_write_error));
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			Log.e("BasicGpsLoggingOnStropWrite", e.toString());
		}

		logFileNameGps = null;

		try {

			mLogGps.close();

			// Make the new file available for other apps.
			Intent mediaScanIntent = new Intent(
					Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
			Uri contentUri = Uri.fromFile(mFileGps);
			mediaScanIntent.setData(contentUri);
			this.sendBroadcast(mediaScanIntent);

		} catch (IOException e) {
			MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
					getString(R.string.gps_log_file_close_error));
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			Log.e("BasicGpsLoggingOnStropCloseLog", e.toString());
		}

		if (LOG_STATE_FLAG) {
			logFileNameState = null;
			try {

				mLogState.close();

				// Make the new file available for other apps.
				Intent mediaScanIntent = new Intent(
						Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				Uri contentUri = Uri.fromFile(mFileState);
				mediaScanIntent.setData(contentUri);
				this.sendBroadcast(mediaScanIntent);

			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						getString(R.string.gps_log_file_close_error));
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("BasicStateLoggingOnStropCloseLog", e.toString());
			}
		}
	}
}
