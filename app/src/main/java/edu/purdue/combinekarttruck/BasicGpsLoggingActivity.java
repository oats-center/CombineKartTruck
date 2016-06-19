package edu.purdue.combinekarttruck;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/*
 * The activity for GPS data recording (also extended to record cell and Wifi signal strength).
 *
 * @author: Yaguang Zhang
 * Reference: Stephan Williams' LogAccelGpsActivity project.
 * Available at https://github.com/OATS-Group/android-logger
 */

public class BasicGpsLoggingActivity extends ActionBarActivity implements
		LocationListener {

	public boolean getLogWifiFlag() {
		return LOG_WIFI_FLAG;
	}

	public boolean getLogSignalFlag() {
		return LOG_SIGNAL_FLAG;
	}

	private boolean LOG_WIFI_FLAG = true;
	private WifiManager wifiManager;

	// Note: signal here actually means cell signal, compared to the Wifi Signal above.
	private boolean LOG_SIGNAL_FLAG = true;
	private TelephonyManager telephonyManager;

	private boolean LOG_STATE_FLAG = true;

	private String loginId, logFilePath, logFileNameGps, logFileNameState, logFileNameSignal, logFileNameWifi;

	private File mFileGps, mFileState, mFileSignal, mFileWifi;
	private FileWriter mLogGps, mLogState, mLogSignal, mLogWifi;

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

	public File getMFileGps() {
		return mFileGps;
	}

	public File getMFileState() {
		return mFileState;
	}

	public FileWriter getMLogGPS() {
		return mLogGps;
	}

	public FileWriter getMLogState() {
		return mLogState;
	}

	public FileWriter getMLogSignal() {
		return mLogSignal;
	}

	public FileWriter getMLogWifi() {
		return mLogWifi;
	}

	public SimpleDateFormat getFormatterClock() {
		return formatterClock;
	}

	public SharedPreferences getSharedPref() {
		return sharedPref;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

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

				if(!logFileDirFile.exists()) {
					if (!logFileDirFile.mkdirs()) {
						Utils.toastStringTextAtCenterWithLargerSize(this, "ERROR: Write to external storage permission denied!");
					}
				}

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
		try {
			mLocationManager.requestLocationUpdates(
					LocationManager.NETWORK_PROVIDER, 0, 0, this);
			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, this);
		} catch(SecurityException e) {
			Log.e("BasicGpsLogging", e.toString());
		}
	}

	public String getLoginType() {
		return getString(R.string.vehicle_kart);
	}

	public String getPartialLogFilePath() {
		return this.getSharedPref().getString(Utils.SAVED_FOLDER_PATH,
				null);
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

		if(LOG_WIFI_FLAG){
			wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		}

		if(LOG_SIGNAL_FLAG) {
			telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		}

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
					Log.e("BasicGpsLogTimer", e.toString());
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

		try {
			mLocationManager.removeUpdates(this);
		} catch(SecurityException e) {
			Log.e("BasicGpsLogging", e.toString());
		}

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

			// Make sure that the data is recorded immediately so that the auto sync (e.g. for Goolge
			// Drive) works.
			mLogGps.flush();
			mFileGps.setLastModified(cur_time);
		} catch (IOException e) {
			MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
					"Error writing into the GPS log file!");
			Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
			Log.e("BasicGpsLogLocChanged", e.toString());
		}

		// Wifi strength logger.
		if(LOG_WIFI_FLAG){
			try {
				WifiInfo wifiInfo = wifiManager.getConnectionInfo();
				String connectedId = wifiInfo.getSSID();
				int RSSI = wifiInfo.getRssi();

				// Try to lock to the specified SSID.
				if (!connectedId.equals("\""+Utils.getHostSsid()+"\"")){
					if(Utils.isLockToHostSsid()){
						Utils.reconnectToAccessPoint(Utils.getHostSsid(), Utils.getHostPasswordD(),
								Utils.isLockToHostSsid(), this);
						throw new IOException("Connected to a wrong access point!");
					} else {

					}
				}

				mLogWifi.write(formatterClock.format(cur_time) + ", " + cur_time
						+ ", " + location.getLatitude() + ", "
						+ location.getLongitude() + ", " + location.getAltitude()
						+ ", " + location.getSpeed() + ", " + location.getBearing()
						+ ", " + location.getAccuracy() + ", "
						+ connectedId + ", " + RSSI + "\n");

				// Make sure that the data is recorded immediately so that the auto sync (e.g. for Google
				// Drive) works.
				mLogWifi.flush();
				mFileWifi.setLastModified(cur_time);

			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						"Error writing into the Wifi strength file!");
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("BasicWifiChanged", e.toString());
			}
		}

		// Cell signal strength logger.
		if(LOG_SIGNAL_FLAG) {
			ArrayList<Integer> gsmDbms = new ArrayList<>();
			ArrayList<Integer> cdmaDbms = new ArrayList<>();
			ArrayList<Integer> lteDbms = new ArrayList<>();

			ArrayList<String> gsmIds = new ArrayList<>();
			ArrayList<String> cdmaIds = new ArrayList<>();
			ArrayList<String> lteIds = new ArrayList<>();

			ArrayList<Long> gsmTimeStamps = new ArrayList<>();
			ArrayList<Long> cdmaTimeStamps = new ArrayList<>();
			ArrayList<Long> lteTimeStamps = new ArrayList<>();

			// Also log the signal strength.
			try {

				for (final CellInfo info : telephonyManager.getAllCellInfo()) {
					if (info instanceof CellInfoGsm) {
						final CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
						// do what you need
						gsmDbms.add(gsm.getDbm());
						gsmIds.add(((CellInfoGsm) info).getCellIdentity().toString());
						gsmTimeStamps.add(info.getTimeStamp());

					} else if (info instanceof CellInfoCdma) {
						final CellSignalStrengthCdma cdma = ((CellInfoCdma) info).getCellSignalStrength();
						// do what you need
						cdmaDbms.add(cdma.getDbm());
						cdmaIds.add(((CellInfoCdma) info).getCellIdentity().toString());
						cdmaTimeStamps.add(info.getTimeStamp());
					} else if (info instanceof CellInfoLte) {
						final CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
						// do what you need
						lteDbms.add(lte.getDbm());
						lteIds.add(((CellInfoLte) info).getCellIdentity().toString());
						lteTimeStamps.add(info.getTimeStamp());
					} else {
						throw new Exception("Unknown type of cell signal!");
					}
				}
			} catch (Exception e) {
				Log.e("SignalStrengthLogger", "Unable to obtain cell signal information", e);
			}

			try {
				mLogSignal.write(formatterClock.format(cur_time) + ", " + cur_time
						+ ", " + location.getLatitude() + ", "
						+ location.getLongitude() + ", " + location.getAltitude()
						+ ", " + location.getSpeed() + ", " + location.getBearing()
						+ ", " + location.getAccuracy() + ", "
						+ gsmIds + ", " + gsmDbms + ", " + gsmTimeStamps + ", "
						+ cdmaIds + ", " + cdmaDbms + ", " + cdmaTimeStamps + ", "
						+ lteIds + ", " + lteDbms + ", " + lteTimeStamps + "\n");

				// Make sure that the data is recorded immediately so that the auto sync (e.g. for Google
				// Drive) works.
				mLogSignal.flush();
				mFileSignal.setLastModified(cur_time);
			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						"Error writing into the signal strength file!");
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("BasicSignalChanged", e.toString());
			}
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
			Log.e("BasicGpsLogCreateLog", e.toString());
		}

		// Create the log file for states.
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
						"Error: Couldn't create the state log file!");
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("BasicStateLogCreate", e.toString());
			}
		}

		// Create the log file for Wifi strength..
		if (LOG_WIFI_FLAG) {
			if (logFileNameWifi == null) {
				formatterUnderline = new SimpleDateFormat(
						"yyyy_MM_dd_HH_mm_ss", java.util.Locale.getDefault());
				Date date = new Date();
				logFileNameWifi = "wifi_" + formatterUnderline.format(date)
						+ ".txt";
			}

			try {
				mFileWifi = new File(logFilePath, logFileNameWifi);

				mLogWifi = new FileWriter(mFileWifi);
				mLogWifi.write("% " + getLoginType() + " " + loginId + ": "
						+ logFileNameWifi + "\n");
				mLogWifi.write("% " + getLoginType() + " " + loginId + ": "
						+ logFileNameGps + "\n"
						+ getString(R.string.wifi_log_file_head));

			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						"Error: Couldn't create the wifi log file!");
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("BasicWifiLogCreate", e.toString());
			}
		}

		// Create the log file for cell signal strength..
		if (LOG_SIGNAL_FLAG) {
			if (logFileNameSignal == null) {
				formatterUnderline = new SimpleDateFormat(
						"yyyy_MM_dd_HH_mm_ss", java.util.Locale.getDefault());
				Date date = new Date();
				logFileNameSignal = "signal_" + formatterUnderline.format(date)
						+ ".txt";
			}

			try {
				mFileSignal = new File(logFilePath, logFileNameSignal);

				mLogSignal = new FileWriter(mFileSignal);
				mLogSignal.write("% " + getLoginType() + " " + loginId + ": "
						+ logFileNameSignal + "\n");
				mLogSignal.write("% " + getLoginType() + " " + loginId + ": "
						+ logFileNameGps + "\n"
						+ getString(R.string.signal_log_file_head));

			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						"Error: Couldn't create the signal log file!");
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("BasicSignalLogCreate", e.toString());
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
			Log.e("BasicGpsLogOnStopWrite", e.toString());
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
			Log.e("BasicGpsLogOnStopClose", e.toString());
		}

		logFileNameState = closeExtraLogFile(LOG_STATE_FLAG, logFileNameState, mFileState, mLogState);
		logFileNameWifi = closeExtraLogFile(LOG_WIFI_FLAG, logFileNameWifi, mFileWifi, mLogWifi);
		logFileNameSignal = closeExtraLogFile(LOG_SIGNAL_FLAG, logFileNameSignal, mFileSignal, mLogSignal);
	}

	private String closeExtraLogFile(boolean logFlag, String logFileName, File mFile, FileWriter mLog) {
		if (logFlag) {

			try {

				mLog.close();

				// Make the new file available for other apps.
				Intent mediaScanIntent = new Intent(
						Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				Uri contentUri = Uri.fromFile(mFile);
				mediaScanIntent.setData(contentUri);
				this.sendBroadcast(mediaScanIntent);

			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						getString(R.string.gps_log_file_close_error));
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("BasicLogOnStopClose", e.toString());
			}

			return null;
		} else {
			return logFileName;
		}
	}
}
