package edu.purdue.combinekarttruck;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import edu.purdue.combinekarttruck.utils.WifiAdmin;

/**
 *
 * Automatically connect to the Hotspot set up by the server side app.
 *
 * Created by Zyglabs on 7/12/15.
 */
public class WifiSpeedTestClientActivity extends BasicGpsLoggingActivity {

    private boolean FORCE_CONN_TO_HOTSPOT = true;
    HotspotAutoReconnector mHotspotAutoRec;

    private String SSID = "OpenAgSpeedTestServer";
    private String PASSWORD = "ecemsee288";

    public String getSSID() {
        return SSID;
    }

    public String getPASSWORD() {
        return PASSWORD;
    }

    private WifiAdmin mWifiAdmin;
    private WifiConfiguration mWifiCon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLogStateFlag(true);
        setLOG_CELL_FLAG(false);
        setLOG_WIFI_FLAG(true);


    }

    @Override
    protected void onStart(){
        super.onStart();

        // Create Wifi manager if necessary.
        if(mWifiAdmin == null) {
            mWifiAdmin = new WifiAdmin(this);
        }

        // Connect to the specified Wifi.
        WifiConfiguration netConfig = mWifiAdmin.getDefaultHotspotConfig(SSID, PASSWORD);
        mWifiAdmin.addNetwork(netConfig);
        mWifiAdmin.getmWifiManager().saveConfiguration();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (FORCE_CONN_TO_HOTSPOT) {

            mHotspotAutoRec = new HotspotAutoReconnector();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            // intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
            registerReceiver(mHotspotAutoRec, intentFilter);
            super.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (FORCE_CONN_TO_HOTSPOT) {
            unregisterReceiver(mHotspotAutoRec);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Remove the hotspot.
        mWifiAdmin.removeWifi(mWifiCon);
        mWifiCon = null;

    }

    public class HotspotAutoReconnector extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
             // Check currently connected Wifi's SSID.
            if(mWifiCon != null &&
                    !BasicGpsLoggingActivity.getCurrentSsid(context).equals(SSID))
            {
                // Only connections to the hotspot will be treated as valid.
                mWifiAdmin.addNetwork(mWifiCon);
            }
        }
    }

}
