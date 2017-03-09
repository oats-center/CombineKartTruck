package edu.purdue.combinekarttruck.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings.Secure;
import android.support.v4.app.ActivityCompat;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;

import java.io.File;
import java.io.IOException;

import edu.purdue.combinekarttruck.R;

/**
 * Created by Zyglabs on 2/4/15.
 *
 * Updated: 2016-06-16 Added locking Wifi SSID function.
 */
public class Utils {
    // Information for the hotspot to be locked to.
    //TODO: Not working!
    private static String HOST_SSID = "Gozer-WiFi";
    private static String HOST_PASSWORD = "1ubuntu9@";
    private static String HOST_TYPE = "WPA2";
    private static boolean LOCK_TO_HOST_SSID = false;

    public static String getHostSsid() {
        return HOST_SSID;
    }
    public static String getHostPasswordD() {
        return HOST_PASSWORD;
    }
    public static String getHostType() {
        return HOST_TYPE;
    }
    public static boolean isLockToHostSsid() {
        return LOCK_TO_HOST_SSID;
    }

    // For locking Wifi connected to a hotspot with a specific SSID.
    private static WifiConfiguration wifiConfig = null;
    private static WifiManager wifiManager = null;

    // For automatically stop logging and exit the app when disconnect the charger.
    public final static int OK_TO_EXIT_NOT_SET = 0, OK_TO_EXIT_READY = 1, OK_TO_EXIT_CONFIRMED = 2;

    public static boolean reconnectToAccessPoint(String ssid, String password, boolean flagToConfirm, Context context){
        if(flagToConfirm){
            // Initiate wifiConfig if it's necessary.
            if(wifiConfig == null){
                wifiConfig = new WifiConfiguration();
                wifiConfig.SSID = String.format("\"%s\"", ssid);
                if(password == "") {
                    wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                }else{
                    wifiConfig.preSharedKey = String.format("\"%s\"", password);
                }
            }
            // Initiate wifiConfig if it's necessary.
            if(wifiManager == null){
                wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            }

            // Reconnect.
            int netId = wifiManager.addNetwork(wifiConfig);
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            return wifiManager.reconnect();
        } else {
            return false;
        }
    }

    public final static String SAVED_FOLDER_PATH = "savedFolderPath";

    /**
    * Initialize device-dependent folder path stored in the SharedPreferences.
    * */
    public static void initDevDepFolderPath (Context context, SharedPreferences sharedPref){
        // Get saved folder path.
        String folderPath = sharedPref.getString(SAVED_FOLDER_PATH,
                null);

        if(folderPath == null){
            // The app is run for the first time. Need to initialize the folder path to save files.

//            final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
//            String deviceId = tm.getDeviceId();
            String deviceId = Secure.getString(context.getContentResolver(),
                    Secure.ANDROID_ID);

            String appName = context.getResources().getString (R.string.app_name);

            // Folder name is formed by both the appName and deviceId.
            folderPath = '/' + appName + '_' + deviceId + '/';

            // Store the folderPath.
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(SAVED_FOLDER_PATH, folderPath);
            editor.commit();
        }
    }

    public static File takePicture(Activity activity, String imageFileName, int request) {
        Context context = activity.getApplicationContext();
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        File tempImageFile;
        Uri tempImageUri;

        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {

            File storageDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            tempImageFile = null;
            try {
                tempImageFile = File.createTempFile(imageFileName, /* prefix */
                        ".jpg", /* suffix */
                        storageDir /* directory */
                );
            } catch (IOException e) {
                toastStringTextAtCenterWithLargerSize(context,
                        context.getString(R.string.temp_image_create_error));

                Toast.makeText(context, e.toString(), Toast.LENGTH_LONG).show();

            }

            // Continue only if the File was successfully created
            if (tempImageFile != null) {
                tempImageUri = Uri.fromFile(tempImageFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        tempImageUri);
                activity.startActivityForResult(takePictureIntent, request);
                return tempImageFile;
            } else {
                toastStringTextAtCenterWithLargerSize(context,
                        "Image file creation failed.");
            }
        } else {
            toastStringTextAtCenterWithLargerSize(context,
                    "No camera activity available.");
        }
        return null;
    }

    /**
     * The function to show message using a modified toast (Centered at the
     * screen with larger font size).
     *
     * @param stringText
     */
    public static void toastStringTextAtCenterWithLargerSize(Context context,
                                                             String stringText) {
        CharSequence text = stringText;
        int duration = 1000;

        Toast toast = Toast.makeText(context, text, duration);

        LinearLayout linearLayout = null;
        linearLayout = (LinearLayout) toast.getView();
        View child = linearLayout.getChildAt(0);
        TextView messageTextView = null;
        messageTextView = (TextView) child;
        messageTextView.setTextSize(30);

        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */

    private static String[] PERMISSIONS= {
            // Location
            Manifest.permission.ACCESS_FINE_LOCATION,
            // Internet
            Manifest.permission.INTERNET,
            // Storage
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,

            // Others
            Manifest.permission.CAMERA,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.CHANGE_NETWORK_STATE,
            Manifest.permission.WAKE_LOCK

    };


    public static void verifyPermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS,
                    22
            );
        }
    }
}
