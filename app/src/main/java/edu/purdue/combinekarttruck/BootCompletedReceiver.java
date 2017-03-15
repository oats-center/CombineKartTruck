package edu.purdue.combinekarttruck;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;
import android.widget.Toast;

import static android.R.attr.duration;

/**
 * Automatically start the MainLoginActivity on device startup.
 *
 * Created by Zyglabs on 3/9/2017.
 */

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("AutoRunCkt", "Boot completion notification received.");

        Intent serviceIntent = new Intent(context, AutoRunLoginService.class);
        context.startService(serviceIntent);
    }

}
