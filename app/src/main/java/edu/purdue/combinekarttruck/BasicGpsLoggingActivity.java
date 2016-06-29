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
import android.telephony.CellInfo;
import android.telephony.CellInfoCdma;
import android.telephony.CellInfoGsm;
import android.telephony.CellInfoLte;
import android.telephony.CellInfoWcdma;
import android.telephony.CellSignalStrengthCdma;
import android.telephony.CellSignalStrengthGsm;
import android.telephony.CellSignalStrengthLte;
import android.telephony.CellSignalStrengthWcdma;
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
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import edu.purdue.combinekarttruck.utils.LogFile;
import edu.purdue.combinekarttruck.utils.Utils;

/*
 * The activity for GPS data recording (also extended to record cell and Wifi signal strength).
 *
 * @author: Yaguang Zhang
 * Reference: Stephan Williams' LogAccelGpsActivity project.
 * Available at https://github.com/OATS-Group/android-logger
 */

public class BasicGpsLoggingActivity extends ActionBarActivity implements
		LocationListener {

	private final boolean DEBUG_FLAG = true;

	/* By default, all will be logged. May be overwritten, e.g., in the WifiSpeedTestClientActivity.
    *
    * However, for the server side app, only cell strength should be logged, while for the client
    * side app, only the Wifi info should be logged.
    */
	private final boolean LOG_GPS_FLAG = true;

	// For the rate test.
	private boolean LOG_RATE_FLAG = true;

	// ----------- Start of parameters set by the user -----------
	// Only try to initiate a new rate test when this is false.
	private String urlHost = DEBUG_FLAG? "http://192.168.1.80/":"http://192.168.1.2/";
	private String rateTestFileSize = "20MB";
	private String urlRateTestFile = urlHost + rateTestFileSize + ".zip.txt";
	private String urlHostAvaiTestFile = urlHost + "hostAvaiTest.txt";
	private float probabilyToInitiateRateTest = 1f;
	// ----------- End of parameters set by the user -----------
	private boolean isRateTestOnProgress = false;
	// For feedback info.
	private int numRateTestTrials = 0;

	private boolean LOG_WIFI_FLAG = true;
	private boolean LOG_CELL_FLAG = false;
	private boolean LOG_STATE_FLAG = false;

	// Set this to be true to only use GPS sensor for the location, instead of using a fused result.
	private boolean GPS_ONLY_FOR_LOC = !DEBUG_FLAG;

	private TelephonyManager mTelephonyManager;
	private WifiManager mWifiManager;

	private String loginId, logFilesPath;
	private LogFile mLogFileGps = new LogFile();
	private LogFile mLogFileState = new LogFile();
	private LogFile mLogFileCell = new LogFile();
	private LogFile mLogFileCellVerbose = new LogFile();
    // For the rate test.
	private LogFile mLogFileWifi = new LogFile();
	private LogFile mLogFileRate = new LogFile();

	// For cell log display on the screen.
	public String stringLoggedToCell;

	private LocationManager mLocationManager;
	private String textViewRateTestStr;
	private TextView textViewRateTest;
	private TextView textViewRateTestCounter;
	private TextView textViewTime;
	private SimpleDateFormat formatterUnderline = new SimpleDateFormat(
			"yyyy_MM_dd_HH_mm_ss",
			java.util.Locale.getDefault());
	private SimpleDateFormat formatterClock;

	// Preference file used to store the info.
	private SharedPreferences sharedPref;

	public void setLogStateFlag(boolean flag) {
		LOG_STATE_FLAG = flag;
	}

	public boolean getLogRateFlag() {
		return LOG_RATE_FLAG;
	}
	public boolean getLogWifiFlag() {
		return LOG_WIFI_FLAG;
	}
	public boolean getLogCellFlag() {
		return LOG_CELL_FLAG;
	}
	public boolean getLogStateFlag() {
		return LOG_STATE_FLAG;
	}

	public String getLoginId() {
		return loginId;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public String getLogFilesPath() {
		return logFilesPath;
	}

	public LogFile getmLogFileGps() {
		return mLogFileGps;
	}

	public LogFile getmLogFileState() {
		return mLogFileState;
	}

	public LogFile getmLogFileCell() {
		return mLogFileCell;
	}

	public LogFile getmLogFileWifi() {
		return mLogFileWifi;
	}

	public LogFile getmLogFileRate() {
		return mLogFileRate;
	}

	public String getStringLoggedToCell() {
		return stringLoggedToCell;
	}

	public void setStringLoggedToCell(String stringLoggedToCell) {
		this.stringLoggedToCell = stringLoggedToCell;
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
		if (logFilesPath == null) {
			logFilesPath = Environment.getExternalStorageDirectory()
					+ getPartialLogFilePath() + loginId;

			if (Environment.MEDIA_MOUNTED.equals(Environment
					.getExternalStorageState())) {
				File logFileDirFile = new File(logFilesPath);

				if (!logFileDirFile.exists()) {
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

		mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		try {
			/**
			 * We need to avoid fused location provider by setting GPS_ONLY_FOR_LOC = true, because
			 * the router is moving and that may cause trouble (jumps of locations).
			 */
			if (!GPS_ONLY_FOR_LOC) {
				mLocationManager.requestLocationUpdates(
						LocationManager.NETWORK_PROVIDER, 0, 0, this);
			}

			mLocationManager.requestLocationUpdates(
					LocationManager.GPS_PROVIDER, 0, 0, this);
		} catch (SecurityException e) {
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

	public SimpleDateFormat getFormatterUnderline() {
		return formatterUnderline;
	}

	@Override
	protected void onResume() {
		super.onResume();

		if(LOG_RATE_FLAG) {
			textViewRateTestStr = getString(R.string.init_rate_test);
		} else {
			textViewRateTest.setVisibility(View.GONE);
			textViewRateTestCounter.setVisibility(View.GONE);
		}

		if (LOG_WIFI_FLAG) {
			mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
		}

		if (LOG_CELL_FLAG) {
			mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		}

		createLogFiles();

		// Start the rate test textView.
		textViewRateTest = ((TextView) findViewById(R.id.textViewRateTestResult));
		textViewRateTestCounter = ((TextView) findViewById(R.id.textViewRateTestCounter));

		// Start the timer textView.
		textViewTime = ((TextView) findViewById(R.id.textViewTime));
		formatterClock = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",
				Locale.getDefault());

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
								textViewRateTest.setText(textViewRateTestStr);
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
	public void onPause() {
		super.onPause();

		TextView ckt = ((TextView) findViewById(R.id.textViewCktState));
		ckt.setText(getString(R.string.ckt_state_loading));

        closeLogFiles();

		try {
			mLocationManager.removeUpdates(this);
		} catch (SecurityException e) {
            Log.e("BasicGpsLogging", e.toString());
        }
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

		// Note: for simple time synchronization between multiple tablets, we are using the system
		// time as GPS time instead of the true GPS time from the sample: location.getTime().
		long cur_time = System.currentTimeMillis();

		long diffTime = cur_time-location.getTime();
		Log.i("Debug", "Comparison of GPS time and system time. \nGpsTime: " + location.getTime() + ", SystemTime: " + cur_time  + ", DiffTime: " + diffTime);

        LogFileWrite(LOG_GPS_FLAG, mLogFileGps, formatterClock.format(cur_time) + ", " + cur_time
                        + ", " + location.getLatitude() + ", "
                        + location.getLongitude() + ", " + location.getAltitude()
                        + ", " + location.getSpeed() + ", " + location.getBearing()
                        + ", " + location.getAccuracy() + "\n",
                "BasicActGpsWrite");

		// Rate test logger.
		if (LOG_RATE_FLAG) {
            LogFileWrite(LOG_RATE_FLAG, mLogFileRate, cur_time
                            + ", " + location.getLatitude() + ", "
                            + location.getLongitude()
                            + "\n",
                    "BasicRateChanged");

			// Try initiate the rate test if necessary.
			if(!isRateTestOnProgress) {
				Random r = new Random(cur_time);
				if (r.nextFloat() < probabilyToInitiateRateTest) {
					numRateTestTrials = numRateTestTrials + 1;
					textViewRateTestStr = "Started a new test!";
					textViewRateTestCounter.setText("Num of rate test trials: " + numRateTestTrials);

					// Block other initiations in the future.
					isRateTestOnProgress = true;
					Utils.toastStringTextAtCenterWithLargerSize(this, "Try initiating a new speed test");
					// Initiate the rate test.
					new Thread(new Runnable() {
						public void run() {
							try {
								// Test if the server is reachable.
								Object hostAvaiTestResult = Http.Get(urlHostAvaiTestFile);
								Log.i("HttpGetHostAvaiTest", ((String) hostAvaiTestResult));

								long startTime = System.currentTimeMillis();
								textViewRateTestStr = "Started at " + formatterClock.format(startTime);

								// Log at the start.
								LogFileWrite(LOG_RATE_FLAG, mLogFileRate, "Start: " + startTime + ", " + rateTestFileSize
												+ "\n",
										"BasicActRateWrite");

								Object result = Http.Get(urlRateTestFile);

								// Log at the end.
								long endTime = System.currentTimeMillis();
								long timeUsed = endTime - startTime;
								long downloadedSize = ((String) result).length();

								float rate = ((float) downloadedSize) / timeUsed * 1000;

								textViewRateTestStr = "Ended at " + formatterClock.format(endTime) + " (Used " + timeUsed/1000.0 + " seconds) with " + rate/1024/1024 + " MiB/s";
								Log.i("HttpGet", "HttpGet(Done): File length: " + rateTestFileSize + ", Downloaded object size: " + downloadedSize + ", timeUsed: " + timeUsed +", Rate: " + rate);

								LogFileWrite(LOG_RATE_FLAG, mLogFileRate, "End: " + endTime + ", "
												+ timeUsed + ", " + downloadedSize + ", " + rate
												+ "\n",
										"BasicActRateWrite");
							} catch (Exception e){
								String eMessage = e.toString();
								if(eMessage.contains("EHOSTUNREACH")) {
									textViewRateTestStr = "Server occupied/unreachable: Test aborted!";
								} else {
									textViewRateTestStr = "Unknown error: Test aborted!";
								}

								Log.e("InitRateTest", eMessage);
							} finally {
								// OK to initiate other rate tests now.
								isRateTestOnProgress = false;
							}
						}
					}).start();
				}
			}
		}

		// Wifi strength logger.
		if (LOG_WIFI_FLAG) {
			try {
				WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
				String connectedId = wifiInfo.getSSID();
				int RSSI = wifiInfo.getRssi();

				// Try to lock to the specified SSID.
				if (!connectedId.equals("\"" + Utils.getHostSsid() + "\"")) {
					if (Utils.isLockToHostSsid()) {
						Utils.reconnectToAccessPoint(Utils.getHostSsid(), Utils.getHostPasswordD(),
								Utils.isLockToHostSsid(), this);
						throw new IOException("Connected to a wrong access point!");
					} else {
						// TODO: Do nothing here?
					}
				}

				LogFileWrite(LOG_WIFI_FLAG, mLogFileWifi, formatterClock.format(cur_time) + ", " + cur_time
								+ ", " + location.getLatitude() + ", "
								+ location.getLongitude() + ", " + location.getAltitude()
								+ ", " + location.getSpeed() + ", " + location.getBearing()
								+ ", " + location.getAccuracy() + ", "
								+ connectedId + ", " + RSSI + "\n",
						"BasicActWifiWrite");
			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						"Error writing into the Wifi strength file!");
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e("BasicWifiChanged", e.toString());
			}
		}

		// Cell signal strength logger.
		if (LOG_CELL_FLAG) {
			ArrayList<Integer> gsmDbms = new ArrayList<>();
			ArrayList<Integer> wcdmaDbms = new ArrayList<>();
			ArrayList<Integer> cdmaDbms = new ArrayList<>();
			ArrayList<Integer> lteDbms = new ArrayList<>();

			ArrayList<String> gsmIds = new ArrayList<>();
			ArrayList<String> wcdmaIds = new ArrayList<>();
			ArrayList<String> cdmaIds = new ArrayList<>();
			ArrayList<String> lteIds = new ArrayList<>();

			ArrayList<String> otherCellInfo = new ArrayList<>();

			ArrayList<Long> gsmTimeStamps = new ArrayList<>();
			ArrayList<Long> wcdmaTimeStamps = new ArrayList<>();
			ArrayList<Long> cdmaTimeStamps = new ArrayList<>();
			ArrayList<Long> lteTimeStamps = new ArrayList<>();

			// Also log the cell signal strength.
			try {

				List<CellInfo> allCellInfo = mTelephonyManager.getAllCellInfo();
				cur_time = System.currentTimeMillis();
				long elapsed_time = android.os.SystemClock.elapsedRealtime();

				LogFileWrite(LOG_CELL_FLAG, mLogFileCellVerbose,
						"----- " + formatterClock.format(cur_time) + ", "
								+ cur_time + "," + elapsed_time + "\n",
						"BasicActCellVerboseWrite");

				boolean flagUnkownCellInfoFound = false;
				for (final CellInfo info : allCellInfo) {

					cur_time = System.currentTimeMillis();
					elapsed_time = android.os.SystemClock.elapsedRealtime();

					LogFileWrite(LOG_CELL_FLAG, mLogFileCellVerbose,
							formatterClock.format(cur_time) + ", " + cur_time + "," + elapsed_time
									+ ", " + info.toString() + "\n",
							"BasicActCellVerboseWrite");

					if (info instanceof CellInfoGsm) {
						final CellSignalStrengthGsm gsm = ((CellInfoGsm) info).getCellSignalStrength();
						// do what you need
						gsmDbms.add(gsm.getDbm());

						String cellId;
						if (info.isRegistered()) {
							cellId = "(Registered)" + ((CellInfoGsm) info).getCellIdentity().toString();
						} else {
							cellId = ((CellInfoGsm) info).getCellIdentity().toString();
						}
						gsmIds.add(cellId);

						gsmTimeStamps.add(info.getTimeStamp());

					} else if (info instanceof CellInfoWcdma) {
						final CellSignalStrengthWcdma wcdma = ((CellInfoWcdma) info).getCellSignalStrength();
						// do what you need
						wcdmaDbms.add(wcdma.getDbm());

						String cellId;
						if (info.isRegistered()) {
							cellId = "(Registered)" + ((CellInfoWcdma) info).getCellIdentity().toString();
						} else {
							cellId = ((CellInfoWcdma) info).getCellIdentity().toString();
						}
						wcdmaIds.add(cellId);

						wcdmaTimeStamps.add(info.getTimeStamp());
					} else if (info instanceof CellInfoCdma) {
						final CellSignalStrengthCdma cdma = ((CellInfoCdma) info).getCellSignalStrength();
						// do what you need
						cdmaDbms.add(cdma.getDbm());

						String cellId;
						if (info.isRegistered()) {
							cellId = "(Registered)" + ((CellInfoCdma) info).getCellIdentity().toString();
						} else {
							cellId = ((CellInfoCdma) info).getCellIdentity().toString();
						}
						cdmaIds.add(cellId);

						cdmaTimeStamps.add(info.getTimeStamp());
					} else if (info instanceof CellInfoLte) {
						final CellSignalStrengthLte lte = ((CellInfoLte) info).getCellSignalStrength();
						// do what you need
						lteDbms.add(lte.getDbm());

						String cellId;
						if (info.isRegistered()) {
							cellId = "(Registered)" + ((CellInfoLte) info).getCellIdentity().toString();
						} else {
							cellId = ((CellInfoLte) info).getCellIdentity().toString();
						}
						lteIds.add(cellId);

						lteTimeStamps.add(info.getTimeStamp());
					} else {
						otherCellInfo.add(info.toString());
						flagUnkownCellInfoFound = true;
					}
				}

				LogFileWrite(LOG_CELL_FLAG, mLogFileCellVerbose,
						"=====\n",
						"BasicActCellVerboseWrite");

				if (flagUnkownCellInfoFound) {
					Log.w("CellStrengthLogger", "Unknown type of cell signal!");
				}
			} catch (Exception e) {
				Log.e("CellStrengthLogger", "Unable to obtain cell signal information", e);
			}

			setStringLoggedToCell(formatterClock.format(cur_time) + ", " + cur_time
					+ ", " + location.getLatitude() + ", "
					+ location.getLongitude() + ", " + location.getSpeed() + ", "
					+ location.getBearing() + ", "
					+ gsmIds + ", " + gsmDbms + ", " + gsmTimeStamps + ", "
					+ cdmaIds + ", " + cdmaDbms + ", " + cdmaTimeStamps + ", "
					+ wcdmaIds + ", " + wcdmaDbms + ", " + wcdmaTimeStamps + ", "
					+ lteIds + ", " + lteDbms + ", " + lteTimeStamps + ","
					+ otherCellInfo + "\n");
			LogFileWrite(LOG_CELL_FLAG, mLogFileCell,
					getStringLoggedToCell(),
					"BasicActCellWrite");
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
		mLogFileGps = createLogFile(LOG_GPS_FLAG, mLogFileGps,
				"gps", getString(R.string.gps_log_file_head), "BasicGpsLogCreate");

		mLogFileState = createLogFile(LOG_STATE_FLAG, mLogFileState,
				"state", getString(R.string.gps_log_file_head), "BasicStateLogCreate");

		mLogFileRate = createLogFile(LOG_RATE_FLAG, mLogFileRate,
				"rate", getString(R.string.rate_log_file_head), "BasicRateLogCreate");

		mLogFileWifi = createLogFile(LOG_WIFI_FLAG, mLogFileWifi,
				"wifi", getString(R.string.wifi_log_file_head), "BasicWifiLogCreate");

		mLogFileCell = createLogFile(LOG_CELL_FLAG, mLogFileCell,
				"cell", getString(R.string.cell_log_file_head), "BasicCellLogCreate");
	}

	public LogFile createLogFile(boolean logFlag, LogFile logFile,
								 String fileType, String fileTitle, String errorTag) {
		if (logFlag) {
			if (logFile.getName() == null) {
				Date date = new Date();
				logFile.setName(fileType + "_" + formatterUnderline.format(date) + ".txt");
			}

			try {
				logFile.setFile(new File(getLogFilesPath(), logFile.getName()));

				logFile.setWriter(new FileWriter(logFile.getFile()));
				logFile.getWriter().write("% " + getLoginType() + " " + getLoginId() + ": "
						+ logFile.getName() + "\n"
						+ fileTitle);

			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						logFile.getName() + "\n" +
								getString(R.string.log_file_create_error)
				);
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e(errorTag, e.toString());
			}

			return logFile;
		} else {
			return new LogFile();
		}
	}

	public void closeLogFiles() {

		// Will test corresponding flags when close the file writers.
		mLogFileGps = closeLogFile(LOG_GPS_FLAG, mLogFileGps, "closeGpsLog");
		mLogFileState = closeLogFile(LOG_STATE_FLAG, mLogFileState, "closeStateLog");
		mLogFileCell = closeLogFile(LOG_CELL_FLAG, mLogFileCell, "closeCellLog");
		mLogFileCellVerbose = closeLogFile(LOG_CELL_FLAG, mLogFileCellVerbose, "closeCellVerboseLog");
		mLogFileWifi = closeLogFile(LOG_WIFI_FLAG, mLogFileWifi, "closeWifiLog");
		mLogFileRate = closeLogFile(LOG_RATE_FLAG, mLogFileRate, "closeRateLog");
	}

	public void LogFileWrite(boolean logFlag, LogFile logFile, String string, String errorTag) {
		if (logFlag) {
			try {
				logFile.getWriter().write(string);

				// Make sure that the data is recorded immediately so that the auto sync (e.g. for Goolge
				// Drive) works.
				logFile.getWriter().flush();
				logFile.getFile().setLastModified(System.currentTimeMillis());
			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						logFile.getName() + "\n" +
								getString(R.string.log_file_write_error));
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e(errorTag, e.toString());
			}
		}
	}

	public LogFile closeLogFile(boolean logFlag, LogFile logFile, String errorTag) {
		if (logFlag) {

			Date date = new Date();

			LogFileWrite(true, logFile,
					"% Stopped at " + formatterClock.format(date) + "\n",
					errorTag);

			try {

				logFile.getWriter().close();

				// Make the new file available for other apps.
				Intent mediaScanIntent = new Intent(
						Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
				Uri contentUri = Uri.fromFile(logFile.getFile());
				mediaScanIntent.setData(contentUri);
				this.sendBroadcast(mediaScanIntent);

			} catch (IOException e) {
				MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
						logFile.getName() + "\n" +
								getString(R.string.log_file_close_error)
				);
				Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
				Log.e(errorTag, e.toString());
			}

			return new LogFile();
		} else {
			return logFile;
		}
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
