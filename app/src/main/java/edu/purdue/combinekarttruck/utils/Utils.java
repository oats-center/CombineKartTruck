package edu.purdue.combinekarttruck.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings.Secure;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import edu.purdue.combinekarttruck.R;

/**
 * Created by Zyglabs on 2/4/15.
 */
public class Utils {

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
                        context.getString(R.string.pptracker_temp_image_create_error));

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
        int duration = Toast.LENGTH_SHORT;

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

}
