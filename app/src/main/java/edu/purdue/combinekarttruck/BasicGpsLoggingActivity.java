package edu.purdue.combinekarttruck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.ScanResult;
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
import android.text.TextUtils;
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
import java.util.List;

import edu.purdue.combinekarttruck.utils.LogFile;

/*
 * The activity for GPS data recording only.
 *
 * @author: Yaguang Zhang
 * Reference: Stephan Williams' LogAccelGpsActivity project.
 * Available at https://github.com/OATS-Group/android-logger
 */

public class BasicGpsLoggingActivity extends ActionBarActivity implements
        LocationListener {

    private final boolean DEBUG_FLAG = false;

    /* By default, all will be logged.
    *
    * However, for the server side app, only cell strength should be logged, while for the client
    * side app, only the Wifi info should be logged.
    */
    private final boolean LOG_GPS_FLAG = true;

    private boolean LOG_STATE_FLAG = true;
    private boolean LOG_CELL_FLAG = true;
    private boolean LOG_WIFI_FLAG = true;

    private boolean GPS_ONLY_FOR_LOC = !DEBUG_FLAG;

    private TelephonyManager mTelephonyManager;
    private WifiManager mWifiManager;
    private WifiScanReceiver mWifiScanReceiver;
    private String wifis[];

    private String loginId, logFilesPath;
    private LogFile mLogFileGps = new LogFile();
    private LogFile mLogFileState = new LogFile();
    private LogFile mLogFileCell = new LogFile();
    private LogFile mLogFileCellVerbose = new LogFile();
    private LogFile mLogFileWifiDisc = new LogFile();
    private LogFile mLogFileWifiConn = new LogFile();

    // For cell log display on the screen.
    public String stringLoggedToCell;

    // For Wifi connection/disconnection detection.
    private boolean mWifiConnectedFlag = false;

    private LocationManager mLocationManager;

    private TextView textViewTime;
    private SimpleDateFormat formatterUnderline = new SimpleDateFormat(
            "yyyy_MM_dd_HH_mm_ss",
            java.util.Locale.getDefault());
    private SimpleDateFormat formatterClock;

    // Preference file used to store the info.
    private SharedPreferences sharedPref;

    public boolean isLOG_STATE_FLAG() {
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

    public void setLogFilesPath(String logFilesPath) {
        this.logFilesPath = logFilesPath;
    }

    public void setLogStateFlag(boolean flag) {
        LOG_STATE_FLAG = flag;
    }

    public boolean isLOG_WIFI_FLAG() {
        return LOG_WIFI_FLAG;
    }

    public void setLOG_WIFI_FLAG(boolean LOG_WIFI_FLAG) {
        this.LOG_WIFI_FLAG = LOG_WIFI_FLAG;
    }

    public boolean isLOG_CELL_FLAG() {
        return LOG_CELL_FLAG;
    }

    public void setLOG_CELL_FLAG(boolean LOG_CELL_FLAG) {
        this.LOG_CELL_FLAG = LOG_CELL_FLAG;
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

    public LogFile getmLogFileWifiDisc() {
        return mLogFileWifiDisc;
    }

    public LogFile getmLogFileWifiConn() {
        return mLogFileWifiConn;
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

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        /** Todo: May want to improve the GPS performance using fused location provider.
         * https://developer.android.com/training/location/receive-location-updates.html
         */
        if (!GPS_ONLY_FOR_LOC) {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0, 0, this);
        }

        mLocationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 0, 0, this);
    }

    public String getLoginType() {
        return getString(R.string.vehicle_kart);
    }

    public String getPartialLogFilePath() {
        return getString(R.string.gps_log_file_path_kart);
    }

    public String getLogFilePath() {
        return logFilesPath;
    }

    public SimpleDateFormat getFormatterUnderline() {
        return formatterUnderline;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (LOG_CELL_FLAG) {
            mTelephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        }

        if (LOG_WIFI_FLAG) {
            mWifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            mWifiScanReceiver = new WifiScanReceiver();
            registerReceiver(mWifiScanReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            super.onResume();
        }

        createLogFiles();

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
    protected void onPause() {
        super.onPause();

        if (LOG_WIFI_FLAG) {
            unregisterReceiver(mWifiScanReceiver);
        }

        closeLogFiles();

        TextView ckt = ((TextView) findViewById(R.id.textViewCktState));
        ckt.setText(getString(R.string.ckt_state_loading));

        mLocationManager.removeUpdates(this);
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

        LogFileWrite(LOG_GPS_FLAG, mLogFileGps, formatterClock.format(cur_time) + ", " + cur_time
                        + ", " + location.getLatitude() + ", "
                        + location.getLongitude() + ", " + location.getAltitude()
                        + ", " + location.getSpeed() + ", " + location.getBearing()
                        + ", " + location.getAccuracy() + "\n",
                "BasicActGpsWrite");

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

        // Only log Wifi info without scheduling any new scan if connected to any Wifi access point.
        if (mWifiConnectedFlag) {
            // Wifi connected.

            // All available Wifi info.
            LogFileWriteAllWifiInfo(LOG_WIFI_FLAG, mWifiConnectedFlag,
                    mLogFileWifiConn, "BasicActWifiConnWrite");

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

        mLogFileCell = createLogFile(LOG_CELL_FLAG, mLogFileCell,
                "cell", getString(R.string.cell_log_file_head), "BasicCellLogCreate");

        mLogFileCellVerbose = createLogFile(LOG_CELL_FLAG, mLogFileCellVerbose,
                "cell_verbose", getString(R.string.cell_verbose_log_file_head), "BasicCellVerboseLogCreate");

        mLogFileWifiDisc = createLogFile(LOG_WIFI_FLAG, mLogFileWifiDisc,
                "wifi_disc", getString(R.string.wifi_disc_log_file_head), "BasicWifiDiscLogCreate");

        mLogFileWifiConn = createLogFile(LOG_WIFI_FLAG, mLogFileWifiConn,
                "wifi_conn", getString(R.string.wifi_conn_log_file_head), "BasicWifiConnLogCreate");

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
        mLogFileWifiDisc = closeLogFile(LOG_WIFI_FLAG, mLogFileWifiDisc, "closeWifiDiscLog");
        mLogFileWifiConn = closeLogFile(LOG_WIFI_FLAG, mLogFileWifiConn, "closeWifiConnLog");
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

    private class WifiScanReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager conMan = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = conMan.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                mWifiConnectedFlag = true;
            } else {
                mWifiConnectedFlag = false;
            }

            // Log all available Wifi info and schedule a new scan if not connected to any Wifi
            // access point.
            if (!mWifiConnectedFlag) {
                LogFileWriteAllWifiInfo(LOG_WIFI_FLAG, mWifiConnectedFlag,
                        mLogFileWifiDisc, "BasicActWifiDiscWrite");
                mWifiManager.startScan();
            }
        }
    }

    public void LogFileWriteAllWifiInfo(boolean logFlag, boolean logConnectedWifiSSID,
                                        LogFile logFile, String errorTag) {

        List<ScanResult> wifiScanList = mWifiManager.getScanResults();
        wifis = new String[wifiScanList.size()];

        long cur_time = System.currentTimeMillis();
        long elapsed_time = android.os.SystemClock.elapsedRealtime();

        for (int i = 0; i < wifiScanList.size(); i++) {
            wifis[i] = i + ", " + (wifiScanList.get(i)).SSID + ", "
                    + (wifiScanList.get(i)).BSSID + ", "
                    + (wifiScanList.get(i)).level + ", "
                    + (wifiScanList.get(i)).frequency + ", "
                    + (wifiScanList.get(i)).timestamp;

//                Log.i("ZygLabs", wifis[i]);
        }

        String string;

        if (logConnectedWifiSSID) {
            string = "------\n"
                    + formatterClock.format(cur_time) + ", " + cur_time + ", " + elapsed_time + "," + getCurrentSsid(this)
                    + "\n------\n";
            for (int i = 0; i < wifis.length; i++) {
                string = string + wifis[i] + "\n";
            }
        } else {
            string = "------\n"
                    + formatterClock.format(cur_time) + ", " + cur_time + ", " + elapsed_time
                    + "\n------\n";
            for (int i = 0; i < wifis.length; i++) {
                string = string + wifis[i] + "\n";
            }
        }

        LogFileWrite(logFlag, logFile,
                string, errorTag);
    }

    public static String getCurrentSsid(Context context) {
        String ssid = null;
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo.isConnected()) {
            final WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            final WifiInfo connectionInfo = wifiManager.getConnectionInfo();
            if (connectionInfo != null && !TextUtils.isEmpty(connectionInfo.getSSID())) {
                ssid = connectionInfo.getSSID();
            }
        }
        return ssid;
    }
}
