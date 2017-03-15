package edu.purdue.combinekarttruck;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.WindowManager;
import android.widget.Toast;

/**
 * Created by Zyglabs on 3/15/2017.
 */

public class AutoRunLoginService extends Service {
    @Override
    public IBinder onBind(Intent intent) {
        // We don't provide binding, so return null
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        final KeyguardManager.KeyguardLock kl = km .newKeyguardLock("MyKeyguardLock");
        kl.disableKeyguard();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK
                | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "MyWakeLock");
        wakeLock.acquire();

        Toast.makeText(this, "CKT auto GPS logging service starting", Toast.LENGTH_SHORT).show();

        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = this.registerReceiver(null, ifilter);
        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        if (isCharging) {
            Toast.makeText(this, "CKT: device is being charged; starting GPS logging", Toast
                    .LENGTH_LONG).show();

            Intent loginIntent = new Intent(this, MainLoginActivity.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(loginIntent);
        } else {
            Toast.makeText(this, "CKT: device is NOT being charged", Toast
                    .LENGTH_LONG).show();
        }

        SystemClock.sleep(5000);
        wakeLock.release();

        stopSelf();
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "CKT auto GPS logging service service done", Toast.LENGTH_SHORT)
                .show();
    }
}
