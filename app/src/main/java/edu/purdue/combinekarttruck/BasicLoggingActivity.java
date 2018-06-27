package edu.purdue.combinekarttruck;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
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
import android.util.JsonWriter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import com.og.tracerouteping.network.TracerouteContainer;
import com.og.tracerouteping.network.TracerouteWithPing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
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

public class BasicLoggingActivity extends ActionBarActivity implements
        LocationListener {
    public static final String tag = "BasicLoggingActivity";

    // Set this to true to log logcat messages to a .txt file for debugging.
    private static final boolean LOG_LOGCAT_FLAG = true;
    private Process pWriteLogcat = null;
    private String filenameLogcat = null;
    private String filePathLogcat = null;
    private LogFile mLogFileLogCat = new LogFile();

    // Determines urlHost.
    private static final boolean DEBUG_FLAG = false;
    // Set this to be true to only use GPS sensor for the location, instead of using a fused result.
    private static final boolean GPS_ONLY_FOR_LOC = true;//!DEBUG_FLAG;
    /* By default, all will be logged. May be overwritten, e.g., in the WifiSpeedTestClientActivity.
    *
    * However, for the server side app, only cell strength should be logged, while for the client
    * side app, only the Wifi info should be logged.
    */
    private final boolean LOG_GPS_FLAG = true;
    // For cell log display on the screen.
    public String stringLoggedToCell;
    public Thread threadTimerUpdateTexts, threadTimerRateTest, threadBatteryDisc;
    // For the rate test.
    private boolean LOG_RATE_FLAG = true;
    private boolean LOG_RATE_ABORTED_FLAG = LOG_RATE_FLAG && true;
    // For sensors.
    private boolean LOG_SENSORS_FLAG = false;
    // For other loggers.
    private boolean LOG_WIFI_FLAG = false;
    private boolean LOG_CELL_FLAG = false;
    private boolean LOG_STATE_FLAG = false;
    private boolean LOG_TRACEROUTE_FLAG = true;

    // ----------- Start of parameters set by the user -----------
    // Only try to initiate a new rate test when this is false.
    private String urlHost = DEBUG_FLAG ? "http://192.168.1.80/" : "http://192.168.0.1/";
    private String rateTestFileSize = "50MB"; //"20MB";
    private String urlRateTestFile = urlHost + rateTestFileSize + ".zip.txt";
    private String urlHostAvaiTestFile = urlHost + "hostAvaiTest.txt";
    private float probabilyToInitiateRateTest = 1f;
    // ----------- End of parameters set by the user -----------
    private boolean isRateTestOnProgress = false;
    // For feedback info.
    private int numRateTestTrials = 0;
    private String lastRateTestResult = "NotAvailable";

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
    private LogFile mLogFileRateAborted = new LogFile();

    private LogFile mLogFileSensors = new LogFile();
    private LogFile mLogFileTraceRoute = new LogFile();
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
    // For logging data from motion sensors.
    private SensorManager mSensorManager;
    private List<Sensor> availableSensors;
    // Automatically stop logging when the device is being charged at first but stops.
    private final long TIME_BEFORE_AUTO_STOP_LOGGING = 10000; // In milisecond.
    private long timer_auto_stop_logging_start = 0;
    private IntentFilter batteryChargedFilter;
    private Intent batteryStatus;
    private static boolean isCharging;

    private String mTracerouteHost = "192.168.0.1"; // Use an IP to avoid bugs in TraceroutePing
    private TracerouteWithPing mTraceroute;
    private ArrayList<TracerouteContainer> mTraces = new ArrayList<>();
    private boolean mTracerouteRun = false;
    private int mTracerouteMaxTtl = 6;

    private SensorEventListener mSensorListener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor arg0, int arg1) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            long curSysTime = System.currentTimeMillis();
            String curValues = "[]";

            if (event.values.length >= 1) {
                curValues = "[" + event.values[0];

                if (event.values.length >= 2) {
                    for (int i = 1; i < event.values.length; i++) {
                        curValues = curValues + ", " + event.values[i];
                    }
                }

                curValues = curValues + "]";
            }

            LogFileWrite(LOG_SENSORS_FLAG, mLogFileSensors,
                    formatterClock.format(curSysTime) + ", " + curSysTime + ", " + event.sensor
                            .getName() + ", " + event.sensor.getStringType() + ", " + event
                            .timestamp + ", " + curValues + "\n",
                    "BasicActSensorsWrite");
        }
    };

    public static boolean getDebugFlag() {
        return DEBUG_FLAG;
    }

    public static boolean getGpsOnlyForLoc() {
        return GPS_ONLY_FOR_LOC;
    }

    public static boolean deviceBeingCharged(Intent batteryStatus) {
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;
        return isCharging;
    }

    public static boolean deviceBeingCharged(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);

        return deviceBeingCharged(batteryStatus);
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

    public void setLogStateFlag(boolean flag) {
        LOG_STATE_FLAG = flag;
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
        getWindow().addFlags(WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY);
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
                        Utils.toastStringTextAtCenterWithLargerSize(this, "ERROR: Write to " +
                                "external storage permission denied!");
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

        if (LOG_SENSORS_FLAG) {
            // Get all available sensors.
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            availableSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        }

        // Log the logcat messages if necessary.
        if(LOG_LOGCAT_FLAG){
            try {
                // We will only log the errors.
                filenameLogcat = createLogFile(true, mLogFileLogCat,
                        "logcatErrors", "Messages from logcat:", "logLogcat")
                        .getName();
                filePathLogcat = new File(getLogFilesPath(), filenameLogcat).getPath();
                // Flush logcat history.
                Runtime.getRuntime().exec("logcat -c");
                pWriteLogcat = Runtime.getRuntime()
                        .exec("logcat -v long -f " +
                                filePathLogcat);
            }
            catch(Exception e){
                Log.e("logcatThread", e.toString());
            }
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

        if (LOG_RATE_FLAG) {
            textViewRateTestStr = getString(R.string.init_rate_test);

            // Try to initiate the rate test (only when it's not doing a rate test) after waiting 0~
            // rateTestWaitTime milliseconds.
            final long rateTestWaitTime = 1000;
            threadTimerRateTest = new Thread() {
                @Override
                public void run() {
                    try {
                        while (!isInterrupted()) {
                            Random r = new Random(System.currentTimeMillis());
                            double randomDouble = r.nextDouble();
                            long timeToWait = (long) Math.floor(randomDouble * rateTestWaitTime);
                            Thread.sleep(timeToWait);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Try initiate the rate test if necessary.
                                    if (!isRateTestOnProgress) {
                                        // Block other initiations in the future.
                                        isRateTestOnProgress = true;

                                        Random r = new Random(System.currentTimeMillis());
                                        if (r.nextFloat() < probabilyToInitiateRateTest) {
                                            numRateTestTrials = numRateTestTrials + 1;
                                            textViewRateTestStr = "Starting a new test...";
                                            textViewRateTestCounter.setText("Num of rate test trials:" +
                                                    " " + numRateTestTrials + "\nLast result: " +
                                                    lastRateTestResult);

                                            // Utils.toastStringTextAtCenterWithLargerSize(this, "Try
                                            // initiating a new speed test");
                                            // Initiate the rate test.
                                            new Thread(new Runnable() {
                                                public void run() {
                                                    long startTime = System.currentTimeMillis();
                                                    try {
                                                        // Test if the server is reachable.
                                                        Object hostAvaiTestResult = Http.GetTest
                                                                (urlHostAvaiTestFile);
                                                        Log.i("HttpGetHostAvaiTest", ((String)
                                                                hostAvaiTestResult));

                                                        startTime = System.currentTimeMillis();
                                                        textViewRateTestStr = "Started at " +
                                                                formatterClock.format(startTime);

                                                        // Log at the start.
                                                        LogFileWrite(LOG_RATE_FLAG, mLogFileRate,
                                                                "Start: " + startTime + ", " +
                                                                        rateTestFileSize
                                                                        + "\n",
                                                                "BasicActRateWrite");

                                                        Object result = Http.Get(urlRateTestFile);

                                                        // Log at the end.
                                                        long endTime = System.currentTimeMillis();
                                                        long timeUsed = endTime - startTime;
                                                        long downloadedSize = ((String) result)
                                                                .length();

                                                        float rate = ((float) downloadedSize) /
                                                                timeUsed * 1000;

                                                        lastRateTestResult = rate / 1024 / 1024 + " " +
                                                                "MiB/s";
                                                        textViewRateTestStr = "Ended at " +
                                                                formatterClock.format(endTime) + " " +
                                                                "(Used " + timeUsed / 1000.0 + " " +
                                                                "seconds) with " + lastRateTestResult;
                                                        Log.i("HttpGet", "HttpGet(Done): File length:" +
                                                                " " + rateTestFileSize + ", " +
                                                                "Downloaded object size: " +
                                                                downloadedSize + ", timeUsed: " +
                                                                timeUsed + ", Rate: " + rate);

                                                        LogFileWrite(LOG_RATE_FLAG, mLogFileRate,
                                                                "End: " + endTime + ", "
                                                                        + timeUsed + ", " +
                                                                        downloadedSize + ", " + rate
                                                                        + "\n",
                                                                "BasicActRateWrite");

                                                    } catch (Exception e) {
                                                        long endTime = System.currentTimeMillis();
                                                        long timeUsed = endTime - startTime;

                                                        String eLabel;
                                                        String eMessage = e.toString();
                                                        if (eMessage.contains("ConnectTimeout")) {
                                                            eLabel = "ConnectTimeout";
                                                            textViewRateTestStr = "Connect timed " +
                                                                    "out...";
                                                        } else if (eMessage.contains("EHOSTUNREACH")) {
                                                            eLabel = "ServerUnreachable";
                                                            textViewRateTestStr = "Server " +
                                                                    "unreachable...";
                                                        } else if (eMessage.contains("Not Extended")) {
                                                            eLabel = "ServerOccupied";
                                                            textViewRateTestStr = "Server occupied...";
                                                        } else {
                                                            eLabel = "UnknownError:" + eMessage;
                                                            textViewRateTestStr = "Unknown error: " +
                                                                    eMessage;
                                                        }

                                                        Log.e("InitRateTest", eMessage);

                                                        LogFileWrite(LOG_RATE_ABORTED_FLAG,
                                                                mLogFileRateAborted, startTime + ", "
                                                                        + endTime + ", "
                                                                        + timeUsed + ", " + eLabel
                                                                        + "\n",
                                                                "BasicActRateWrite");

                                                    } finally {
                                                        // OK to initiate other rate tests now.
                                                        isRateTestOnProgress = false;
                                                    }
                                                }
                                            }).start();
                                        }
                                    }
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        Log.e("BasicGpsLogTimer", e.toString());
                    }
                }
            };
            threadTimerRateTest.start();
        } else {
            if (textViewRateTest!=null) {
                textViewRateTest.setVisibility(View.GONE);
            }
            if (textViewRateTestCounter!=null) {
                textViewRateTestCounter.setVisibility(View.GONE);
            }

        }

        // Start the timer textView.
        textViewTime = ((TextView) findViewById(R.id.textViewTime));
        formatterClock = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss",
                Locale.getDefault());

        // Update some of the text on the screen around every 0.1s.
        threadTimerUpdateTexts = new Thread() {
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
                                if (LOG_RATE_FLAG) {
                                    textViewRateTest.setText(textViewRateTestStr);
                                }
                            }
                        });
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    Log.e("BasicGpsLogTimer", e.toString());
                }
            }
        };
        threadTimerUpdateTexts.start();

        setBackgroundColor();

        if (LOG_SENSORS_FLAG) {
            // Register sensors.
            for (Sensor s : availableSensors) {
                if (s.getMinDelay() == 5000) {
                    mSensorManager.registerListener(mSensorListener, s, SensorManager
                            .SENSOR_DELAY_GAME);
                } else {
                    mSensorManager.registerListener(mSensorListener, s, SensorManager
                            .SENSOR_DELAY_FASTEST);
                }
            }
        }

        // Auto stop logging.
        batteryChargedFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryStatus = this.registerReceiver(null, batteryChargedFilter);
        final Context batteryContext = this;
        if ( deviceBeingCharged(batteryStatus) &&
                sharedPref.getBoolean(
                        getString(R.string.shared_preference_being_charged_on_login),
                        false)
                ) {
            // The device was being charged on the login page and is also being charged now. We will
            // stop the logger automatically if the charger is disconnected after this.

            // Set a thread to check the battery status.
            threadBatteryDisc = new Thread() {
                @Override
                public void run() {
                    try {
                        while (!isInterrupted()) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (deviceBeingCharged(batteryContext)) {
                                        // Reset the counter to no-need-to-auto-stop state.
                                        timer_auto_stop_logging_start = 0;
                                    } else {
                                        if (timer_auto_stop_logging_start == 0) {
                                            // Start counting.
                                            timer_auto_stop_logging_start = System
                                                    .currentTimeMillis();
                                        } else {
                                            // Counting already started.
                                            if (System.currentTimeMillis()
                                                    - timer_auto_stop_logging_start >
                                                    TIME_BEFORE_AUTO_STOP_LOGGING) {
                                                // Not charging for a long time. Auto stop logging.
                                                threadBatteryDisc.interrupt();

                                                // OK for the login (main) page to exit.
                                                SharedPreferences.Editor sharedPrefEditor =
                                                        sharedPref
                                                                .edit();
                                                sharedPrefEditor.putInt(getString(R.string
                                                        .shared_preference_ok_to_exit), Utils
                                                        .OK_TO_EXIT_CONFIRMED);
                                                sharedPrefEditor.commit();

                                                // Stop the logging activity.
                                                MediaPlayer MPStopLogging = playerStopLogging();
                                                try {
                                                    Thread.sleep(MPStopLogging.getDuration());
                                                } catch (Exception e) {
                                                    Log.e("MPStopLogging", e.toString());
                                                }
                                                onBackPressed();
                                            } else {
                                                // Still counting. Nothing to do.
                                            }
                                        }
                                    }
                                }
                            });
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                        Log.e("threadBatteryDisc", e.toString());
                    }
                }
            };
            threadBatteryDisc.start();
        }

        // Play a clip of sound effect to bring attention from the user.
        final MediaPlayer mediaPlayerStartLogging = MediaPlayer.create(getApplicationContext(),
                R.raw.initiating_gps_logging);
        new Thread() {
            @Override
            public void run() {
                try {
                    mediaPlayerStartLogging.start();
                } catch (Exception e) {
                    Log.e("mediaPlayerStartLogging", e.toString());
                }
            }
        }.start();

        if (LOG_TRACEROUTE_FLAG) {
            mTracerouteRun = true;
            mTraceroute = new TracerouteWithPing(this);
            mTraceroute.executeTraceroute(mTracerouteHost, mTracerouteMaxTtl);
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
    // Make sure the buttons in the action bar work as expected.
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause() {
        super.onPause();

        if(!sharedPref.getBoolean(
                getString(R.string.shared_preference_being_charged_on_login),
                false)) {
            // Not charged at the login page. Do not need to ignore the plugged in signal.
            onBackPressed();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        mTracerouteRun = false;

        if (LOG_SENSORS_FLAG) {
            // Unregister sensors.
            mSensorManager.unregisterListener(mSensorListener);
        }
        threadTimerUpdateTexts.interrupt();
        if(getLogRateFlag()) {
            threadTimerRateTest.interrupt();
        }

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
    protected void onDestroy() {
        super.onDestroy();

        playerStopLogging();

        // Close  the logcat messages if necessary.
        if(LOG_LOGCAT_FLAG){
            pWriteLogcat.destroy();

            // Delete the logcat file if it is empty.
            File logcatFile = new File(filePathLogcat);
            if (logcatFile.length() == 0) {
                try {
                    logcatFile.delete();
                } catch (Exception e) {
                    Log.e("DeleteEmptyLogcatFile", "onDestroy: " + e.toString());
                }
            }
        }
    }

    public MediaPlayer playerStopLogging() {
        // Play a clip of sound effect to bring attention from the user.
        final MediaPlayer mediaPlayerStopLogging = MediaPlayer.create(getApplicationContext(), R.raw
                .stopping_gps_logging);
        try {
            mediaPlayerStopLogging.start();
            return mediaPlayerStopLogging;
        } catch (Exception e) {
            Log.e("mediaPlayerStopLogging", e.toString());
            return null;
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        /**
         * First thing: get the difference between the system time and GPS time.
         */
        // Update: we will record both the system time and the GPS time, just in case.
        //
        // Note: for simple time synchronization between multiple tablets, we are using the system
        // time as GPS time instead of the true GPS time from the sample: location.getTime().
        long cur_sys_time = System.currentTimeMillis();

        // As of July 2015, GPS time is 17 seconds ahead of UTC because of the leap second added to
        // UTC June 30, 2015. If the system time is perfectly synched, we should see a difference of
        // around -17000 ms.
        long diff_time = cur_sys_time - location.getTime();

        /**
         * Show the information on the screen.
         */
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

        Log.i("Debug", "Comparison of GPS time and system time. \nGpsTime: " + location.getTime()
                + ", SystemTime: " + cur_sys_time + ", DiffTime: " + diff_time);

        /**
         * Log in to files.
         */
        // GPS logger.
        LogFileWrite(LOG_GPS_FLAG, mLogFileGps, formatterClock.format(cur_sys_time) + ", " +
                        location.getTime()
                        + ", " + location.getLatitude() + ", "
                        + location.getLongitude() + ", " + location.getAltitude()
                        + ", " + location.getSpeed() + ", " + location.getBearing()
                        + ", " + location.getAccuracy()
                        + ", " + cur_sys_time + ", " + diff_time
                        + "\n",
                "BasicActGpsWrite");

        // Rate test logger.
        if (LOG_RATE_FLAG) {
            LogFileWrite(LOG_RATE_FLAG, mLogFileRate, location.getTime()
                            + ", " + location.getLatitude() + ", "
                            + location.getLongitude()
                            + ", " + cur_sys_time + ", " + diff_time
                            + "\n",
                    "BasicRateChanged");
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

                LogFileWrite(LOG_WIFI_FLAG, mLogFileWifi, formatterClock.format(cur_sys_time) +
                                ", " + location.getTime()
                                + ", " + location.getLatitude() + ", "
                                + location.getLongitude() + ", " + location.getAltitude()
                                + ", " + location.getSpeed() + ", " + location.getBearing()
                                + ", " + location.getAccuracy() + ", "
                                + connectedId + ", " + RSSI
                                + ", " + cur_sys_time + ", " + diff_time
                                + "\n",
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
                long elapsed_time = android.os.SystemClock.elapsedRealtime();

                LogFileWrite(LOG_CELL_FLAG, mLogFileCellVerbose,
                        "----- " + formatterClock.format(location.getTime()) + ", "
                                + location.getTime() + "," + elapsed_time + "\n",
                        "BasicActCellVerboseWrite");

                boolean flagUnkownCellInfoFound = false;
                for (final CellInfo info : allCellInfo) {

                    elapsed_time = android.os.SystemClock.elapsedRealtime();

                    LogFileWrite(LOG_CELL_FLAG, mLogFileCellVerbose,
                            formatterClock.format(location.getTime()) + ", " + location.getTime()
                                    + "," + elapsed_time
                                    + ", " + info.toString() + "\n",
                            "BasicActCellVerboseWrite");

                    if (info instanceof CellInfoGsm) {
                        final CellSignalStrengthGsm gsm = ((CellInfoGsm) info)
                                .getCellSignalStrength();
                        // do what you need
                        gsmDbms.add(gsm.getDbm());

                        String cellId;
                        if (info.isRegistered()) {
                            cellId = "(Registered)" + ((CellInfoGsm) info).getCellIdentity()
                                    .toString();
                        } else {
                            cellId = ((CellInfoGsm) info).getCellIdentity().toString();
                        }
                        gsmIds.add(cellId);

                        gsmTimeStamps.add(info.getTimeStamp());

                    } else if (info instanceof CellInfoWcdma) {
                        final CellSignalStrengthWcdma wcdma = ((CellInfoWcdma) info)
                                .getCellSignalStrength();
                        // do what you need
                        wcdmaDbms.add(wcdma.getDbm());

                        String cellId;
                        if (info.isRegistered()) {
                            cellId = "(Registered)" + ((CellInfoWcdma) info).getCellIdentity()
                                    .toString();
                        } else {
                            cellId = ((CellInfoWcdma) info).getCellIdentity().toString();
                        }
                        wcdmaIds.add(cellId);

                        wcdmaTimeStamps.add(info.getTimeStamp());
                    } else if (info instanceof CellInfoCdma) {
                        final CellSignalStrengthCdma cdma = ((CellInfoCdma) info)
                                .getCellSignalStrength();
                        // do what you need
                        cdmaDbms.add(cdma.getDbm());

                        String cellId;
                        if (info.isRegistered()) {
                            cellId = "(Registered)" + ((CellInfoCdma) info).getCellIdentity()
                                    .toString();
                        } else {
                            cellId = ((CellInfoCdma) info).getCellIdentity().toString();
                        }
                        cdmaIds.add(cellId);

                        cdmaTimeStamps.add(info.getTimeStamp());
                    } else if (info instanceof CellInfoLte) {
                        final CellSignalStrengthLte lte = ((CellInfoLte) info)
                                .getCellSignalStrength();
                        // do what you need
                        lteDbms.add(lte.getDbm());

                        String cellId;
                        if (info.isRegistered()) {
                            cellId = "(Registered)" + ((CellInfoLte) info).getCellIdentity()
                                    .toString();
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

            setStringLoggedToCell(formatterClock.format(location.getTime()) + ", " + location
                    .getTime()
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

    public void tracerouteAddTrace(TracerouteContainer trace) {
        mTraces.add(trace);
    }

    public void tracerouteComplete(boolean complete) {
        Log.d(tag, mTraces.toString());
        StringWriter data = new StringWriter();
        JsonWriter writer = new JsonWriter(data);

        try {
            writer.beginObject();
            writer.name("complete").value(complete);
            writer.name("route");
            writer.beginArray();
            for (TracerouteContainer trace : mTraces) {
                trace.toJson(writer);
            }
            writer.endArray();
            writer.endObject();
        } catch (IOException e) {
            Log.e(tag, e.getStackTrace().toString());
            mTraces.clear();
            return;
        }

        if (mTracerouteRun) { // closeLogFile returns a new (but immediately closed) log file
            LogFileWrite(LOG_TRACEROUTE_FLAG, mLogFileTraceRoute,
                    data.toString() + "\n", "TracerouteWrite");
        }

        mTraces.clear();

        if (mTracerouteRun) {
            mTraceroute = new TracerouteWithPing(this);
            mTraceroute.executeTraceroute(mTracerouteHost, mTracerouteMaxTtl);
        }
    }

    public void createLogFiles() {
        mLogFileGps = createLogFile(LOG_GPS_FLAG, mLogFileGps,
                "gps", getString(R.string.gps_log_file_head), "BasicGpsLogCreate");

        mLogFileState = createLogFile(LOG_STATE_FLAG, mLogFileState,
                "state", getString(R.string.gps_log_file_head), "BasicStateLogCreate");

        mLogFileRate = createLogFile(LOG_RATE_FLAG, mLogFileRate,
                "rate", getString(R.string.rate_log_file_head), "BasicRateLogCreate");

        mLogFileRateAborted = createLogFile(LOG_RATE_ABORTED_FLAG, mLogFileRateAborted,
                "abortedRate", getString(R.string.rate_aborted_log_file_head),
                "BasicRateAbortedLogCreate");

        mLogFileWifi = createLogFile(LOG_WIFI_FLAG, mLogFileWifi,
                "wifi", getString(R.string.wifi_log_file_head), "BasicWifiLogCreate");

        mLogFileCell = createLogFile(LOG_CELL_FLAG, mLogFileCell,
                "cell", getString(R.string.cell_log_file_head), "BasicCellLogCreate");

        mLogFileSensors = createLogFile(LOG_SENSORS_FLAG, mLogFileSensors,
                "sensors", LOG_SENSORS_FLAG ? availableSensors.toString() + "\n" : "",
                "BasicSensorsLogCreate");

        mLogFileTraceRoute = createLogFile(LOG_TRACEROUTE_FLAG, mLogFileTraceRoute, "traceroute",
                getString(R.string.traceroute_log_file_head), "TraceRouteCreate");
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
        mLogFileCellVerbose = closeLogFile(LOG_CELL_FLAG, mLogFileCellVerbose,
                "closeCellVerboseLog");
        mLogFileWifi = closeLogFile(LOG_WIFI_FLAG, mLogFileWifi, "closeWifiLog");
        mLogFileRate = closeLogFile(LOG_RATE_FLAG, mLogFileRate, "closeRateLog");
        mLogFileRateAborted = closeLogFile(LOG_RATE_ABORTED_FLAG, mLogFileRateAborted,
                "closeRateAbortedLog");
        mLogFileSensors = closeLogFile(LOG_SENSORS_FLAG, mLogFileSensors, "closeSensorsLog");
        mLogFileTraceRoute = closeLogFile(LOG_TRACEROUTE_FLAG, mLogFileTraceRoute, "closeTracerouteLog");
    }

    public void LogFileWrite(boolean logFlag, LogFile logFile, String string, String errorTag) {
        if (logFlag) {
            try {
                logFile.getWriter().write(string);

                // Make sure that the data is recorded immediately so that the auto sync (e.g.
                // for Goolge
                // Drive) works.
                logFile.getWriter().flush();
                logFile.getFile().setLastModified(System.currentTimeMillis());
            } catch (IOException e) {
                MainLoginActivity.toastStringTextAtCenterWithLargerSize(this,
                        logFile.getName() + "\n" +
                                getString(R.string.log_file_write_error));
                Toast.makeText(this, e.toString(), Toast.LENGTH_LONG).show();
                Log.e(errorTag, e.toString());
            } catch (Exception e) {
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

    private String closeExtraLogFile(boolean logFlag, String logFileName, File mFile, FileWriter
            mLog) {
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

//    public static class PowerConnectionReceiver extends BroadcastReceiver {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
//            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
//                    status == BatteryManager.BATTERY_STATUS_FULL;
////            int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
////            boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
////            boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
//        }
//    }
}
