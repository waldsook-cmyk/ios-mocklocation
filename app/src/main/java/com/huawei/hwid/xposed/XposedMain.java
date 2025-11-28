package com.huawei.hwid.xposed;

import android.app.Application;
import android.content.Context;
import android.location.GnssStatus;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.telephony.CellInfo;
import android.telephony.CellLocation;
import android.telephony.NeighboringCellInfo;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Xposed 主入口 - Hook 所有位置相关 API
 * 
 * 需要配合 VirtualXposed / 太极 / SandVXposed 使用
 * 或者集成 VirtualApp 框架
 */
public class XposedMain {

    private static final String TAG = "MockLocation-Xposed";

    /**
     * Hook 入口点 - 被 Xposed 框架调用
     */
    public static void handleLoadPackage(Object lpparam) throws Throwable {
        // 这里需要使用 de.robv.android.xposed.XposedHelpers
        // 由于我们要做免 Root 方案，需要集成虚拟化框架
    }

    /**
     * 需要 Hook 的方法列表
     */
    public static class HookTargets {
        
        // ========== LocationManager ==========
        // getLastKnownLocation(String provider) -> Location
        // requestLocationUpdates(...) -> void (需要拦截 LocationListener 回调)
        // getProviders(boolean enabledOnly) -> List<String>
        // isProviderEnabled(String provider) -> boolean
        // addGpsStatusListener(GpsStatus.Listener) -> boolean
        // registerGnssStatusCallback(GnssStatus.Callback) -> boolean

        // ========== Location ==========
        // isFromMockProvider() -> boolean (返回 false)
        // isMock() -> boolean (返回 false)

        // ========== TelephonyManager ==========
        // getCellLocation() -> CellLocation (返回假基站)
        // getAllCellInfo() -> List<CellInfo> (返回空或假数据)
        // getNeighboringCellInfo() -> List<NeighboringCellInfo>

        // ========== WifiManager ==========
        // getScanResults() -> List<ScanResult> (返回空或假 WiFi)
        // getConnectionInfo() -> WifiInfo

        // ========== Settings.Secure ==========
        // getString(resolver, "mock_location") -> null
        // getInt(resolver, "development_settings_enabled") -> 0

        // ========== 钉钉专用检测 ==========
        // 检测 /system/xposed.prop
        // 检测 /data/data/de.robv.android.xposed.installer
        // 检测 /data/app/de.robv.android.xposed.installer
        // 检测 Build.TAGS 是否包含 "test-keys"
    }

    /**
     * 模拟的基站信息
     */
    public static class FakeCellInfo {
        public static int LAC = 9527;  // 位置区域码
        public static int CID = 12345; // 基站ID
        public static int MCC = 460;   // 中国
        public static int MNC = 0;     // 中国移动

        public static void updateForLocation(double lat, double lng) {
            // 根据经纬度生成合理的基站信息
            LAC = 9000 + (int)(lat * 10) % 1000;
            CID = 10000 + (int)(lng * 10) % 10000;
        }
    }
}
