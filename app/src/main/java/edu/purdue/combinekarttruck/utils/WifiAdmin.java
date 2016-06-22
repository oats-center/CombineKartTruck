package edu.purdue.combinekarttruck.utils;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by Zyglabs on 7/10/15.
 * <p/>
 * A helper to setup hotspot.
 * <p/>
 * Refs:
 * http://blog.csdn.net/yuanbohx/article/details/8109042
 * http://stackoverflow.com/questions/25766425/android-programmatically-turn-on-wifi-hotspot
 */
public class WifiAdmin {
    // 定义WifiManager对象
    private WifiManager mWifiManager;
    // 定义WifiInfo对象
    private WifiInfo mWifiInfo;
    // 扫描出的网络连接列表
    private List<ScanResult> mWifiList;
    // 网络连接列表
    private List<WifiConfiguration> mWifiConfiguration;
    // 定义一个WifiLock
    WifiLock mWifiLock;

    public WifiManager getmWifiManager() {
        return mWifiManager;
    }

    // 构造器
    public WifiAdmin(Context context) {
        // 取得WifiManager对象
        mWifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        // 取得WifiInfo对象
        mWifiInfo = mWifiManager.getConnectionInfo();
    }

    // 打开WIFI
    public void openWifi() {
        if (!mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(true);
        }
    }

    // 关闭WIFI
    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    // 检查当前WIFI状态
    public int checkState() {
        return mWifiManager.getWifiState();
    }

    // 锁定WifiLock
    public void acquireWifiLock() {
        mWifiLock.acquire();
    }

    // 解锁WifiLock
    public void releaseWifiLock() {
        // 判断时候锁定
        if (mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    // 创建一个WifiLock
    public void creatWifiLock(String string) {
        mWifiLock = mWifiManager.createWifiLock(string);
    }

    // 得到配置好的网络
    public List<WifiConfiguration> getConfiguration() {
        return mWifiConfiguration;
    }

    // 指定配置好的网络进行连接
    public void connectConfiguration(int index) {
        // 索引大于配置好的网络索引返回
        if (index > mWifiConfiguration.size()) {
            return;
        }
        // 连接配置好的指定ID的网络
        mWifiManager.enableNetwork(mWifiConfiguration.get(index).networkId,
                true);
    }

    public void startScan() {
        mWifiManager.startScan();
        // 得到扫描结果
        mWifiList = mWifiManager.getScanResults();
        // 得到配置好的网络连接
        mWifiConfiguration = mWifiManager.getConfiguredNetworks();
    }

    // 得到网络列表
    public List<ScanResult> getWifiList() {
        return mWifiList;
    }

    // 查看扫描结果
    public StringBuilder lookUpScan() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mWifiList.size(); i++) {
            stringBuilder
                    .append("Index_" + new Integer(i + 1).toString() + ":");
            // 将ScanResult信息转换成一个字符串包
            // 其中把包括：BSSID、SSID、capabilities、frequency、level
            stringBuilder.append((mWifiList.get(i)).toString());
            stringBuilder.append("/n");
        }
        return stringBuilder;
    }

    // 得到MAC地址
    public String getMacAddress() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();
    }

    // 得到接入点的BSSID
    public String getBSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();
    }

    // 得到IP地址
    public int getIPAddress() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();
    }

    // 得到连接的ID
    public int getNetworkId() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
    }

    // 得到WifiInfo的所有信息包
    public String getWifiInfo() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();
    }

    // 添加一个网络并连接
    public void addNetwork(WifiManager wm, WifiConfiguration wcg) {

        int wcgID = wm.addNetwork(wcg);

        if (!wm.isWifiEnabled()) {
            //---wifi is turned off---
            //---turn on wifi---
            wm.setWifiEnabled(true);
        }

        boolean b = wm.disconnect();
        wm.saveConfiguration();
        boolean c = wm.enableNetwork(wcgID, true);
        wm.reconnect();
        // TODO: Lock to the Wifi access point specified!
        wm.saveConfiguration();

        System.out.println("a(wcgID)--" + wcgID);
        System.out.println("b(disc)--" + b);
        System.out.println("c(enable)--" + c);
    }

    public int addNetwork(WifiConfiguration wcg) {

        int wcgID = mWifiManager.addNetwork(wcg);

        if (!mWifiManager.isWifiEnabled()) {
            //---wifi is turned off---
            //---turn on wifi---
            mWifiManager.setWifiEnabled(true);
        }

        mWifiManager.disconnect();
        mWifiManager.enableNetwork(wcgID, true);
        mWifiManager.saveConfiguration();

        return wcgID;
    }

    // 断开指定ID的网络
    public void disconnectWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }

//然后是一个实际应用方法，只验证过没有密码的情况：

    public WifiConfiguration CreateWifiInfo(String SSID, String Password, int Type) {
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";

        WifiConfiguration tempConfig = this.IsExsits(SSID);
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId);
        }

        if (Type == 1) //WIFICIPHER_NOPASS
        {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == 2) //WIFICIPHER_WEP
        {
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + Password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        }
        if (Type == 3) //WIFICIPHER_WPA
        {
            config.preSharedKey = "\"" + Password + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            //config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.status = WifiConfiguration.Status.ENABLED;
        } //分为三种情况：1没有密码2用wep加密3用wpa加密
        mWifiManager.saveConfiguration();
        return config;
    }

    private WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }

    public void removeWifi(WifiConfiguration wifiCon) {
        if (wifiCon != null) {
            mWifiManager.removeNetwork(wifiCon.networkId);
        }
        mWifiManager.saveConfiguration();
    }

    // Used for setting up the Hotspot.
    public WifiConfiguration createWifiAccessPoint(String SSID, String PASSWORD) {
        WifiConfiguration netConfig = null;

        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
        Method[] wmMethods = mWifiManager.getClass().getDeclaredMethods();
        boolean methodFound = false;
        for (Method method : wmMethods) {
            if (method.getName().equals("setWifiApEnabled")) {
                methodFound = true;
                netConfig = getDefaultHotspotConfig(SSID, PASSWORD);

                mWifiManager.saveConfiguration();

                try {
                    boolean apstatus = (Boolean) method.invoke(mWifiManager, netConfig, true);
                    Log.i("WifiSpeedTestServer", "Creating a Wi-Fi Network \"" + netConfig.SSID + "\"");
//                    for (Method isWifiApEnabledmethod : wmMethods) {
//                        if (isWifiApEnabledmethod.getName().equals("isWifiApEnabled")) {
//                            while (!(Boolean) isWifiApEnabledmethod.invoke(mWifiManager)) ;
//                            for (Method method1 : wmMethods) {
//                                if (method1.getName().equals("getWifiApState")) {
//                                    int apstate;
//                                    apstate = (Integer) method1.invoke(mWifiManager);
//                                    netConfig = (WifiConfiguration) method1.invoke(mWifiManager);
//                                    Log.i("WifiSpeedTestServer", "\nSSID:" + netConfig.SSID + "\nPassword:" + netConfig.preSharedKey + "\n");
//                                }
//                            }
//                        }
//                    }

                    if (apstatus) {
                        System.out.println("SUCCESS");
                        //statusView.append("\nAccess Point Created!");
                        //finish();
                        //Intent searchSensorsIntent = new Intent(this,SearchSensors.class);
                        //startActivity(searchSensorsIntent);
                    } else {
                        System.out.println("FAILED");
                        //statusView.append("\nAccess Point Creation failed!");
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!methodFound) {
            Log.i("WifiSpeedTestServer", "Your phone's API does not contain setWifiApEnabled method to configure an access point");
        }

        return netConfig;
    }

    public WifiConfiguration closeWifiAccessPoint(WifiConfiguration netConfig) {

        // Turn off wifi.
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }

        if(netConfig != null) {
            Method[] wmMethods = mWifiManager.getClass().getDeclaredMethods();
            boolean methodFound = false;
            for (Method method : wmMethods) {
                if (method.getName().equals("setWifiApEnabled")) {
                    methodFound = true;

                    try {
                        boolean apstatus = (Boolean) method.invoke(mWifiManager, netConfig, false);
                        Log.i("WifiSpeedTestServer", "Turning of the Wi-Fi Network \"" + netConfig.SSID + "\"");

                        if (apstatus) {
                            System.out.println("SUCCESS");
                            //statusView.append("\nAccess Point Created!");
                            //finish();
                            //Intent searchSensorsIntent = new Intent(this,SearchSensors.class);
                            //startActivity(searchSensorsIntent);
                        } else {
                            System.out.println("FAILED");
                            //statusView.append("\nAccess Point Creation failed!");
                        }
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
            if (!methodFound) {
                Log.i("WifiSpeedTestServer", "Your phone's API does not contain setWifiApEnabled method to configure an access point");
            }
        } else {
            Log.i("WifiSpeedTestServer", "The Wi-Fi Network to be closed is null!");
        }

        return null;
    }

    public WifiConfiguration getDefaultHotspotConfig(String SSID, String PASSWORD){
        WifiConfiguration netConfig= new WifiConfiguration();

        netConfig.SSID = "\"" + SSID + "\"";
        netConfig.hiddenSSID = false;
        netConfig.preSharedKey = PASSWORD;
        netConfig.wepKeys[0] = "\"" + PASSWORD + "\"";
        netConfig.wepTxKeyIndex = 0;
        netConfig.status = WifiConfiguration.Status.ENABLED;

        netConfig.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        netConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        netConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        netConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        netConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

        return netConfig;
    }
}
