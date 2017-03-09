package edu.purdue.combinekarttruck;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.util.Log;

/**
 * Automatically start the MainLoginActivity on device startup.
 *
 * Created by Zyglabs on 3/9/2017.
 */

public class RunMainLoginActivityOnStartup extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i("AutoRunCkt", "Boot completion notification received.");

        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent i = new Intent(context, MainLoginActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }

}
