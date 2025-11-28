package com.huawei.hwid;

import android.location.Location;
import android.util.Log;

/**
 * Native 层 Hook 接口
 * 模仿定位助手的 mark(Location) 方法，在 Native 层清除 Mock 标志
 */
public class NativeHook {

    private static final String TAG = "NativeHook";
    private static boolean libraryLoaded = false;

    static {
        try {
            System.loadLibrary("mocklocation");
            libraryLoaded = true;
            Log.i(TAG, "Native library loaded successfully");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library", e);
            libraryLoaded = false;
        }
    }

    /**
     * 检查 native 库是否加载成功
     */
    public static boolean isLibraryLoaded() {
        return libraryLoaded;
    }

    /**
     * 设置模拟位置
     */
    public static native void setMockLocation(double latitude, double longitude);

    /**
     * 停止模拟
     */
    public static native void stopMock();

    /**
     * 检查是否启用
     */
    public static native boolean isMockEnabled();

    /**
     * 核心方法：标记 Location 对象
     * 模仿定位助手的 mark(Location) 方法
     * 在 Native 层清除所有 Mock 标志，使位置看起来像真实 GPS
     */
    public static native void markLocation(Location location);

    /**
     * 在 Native 层创建 Location 对象
     * 这种方式创建的 Location 可能绕过某些检测
     */
    public static native Location createNativeLocation(String provider);

    /**
     * 获取系统属性
     */
    public static native String getSystemProperty(String key);

    /**
     * 安全地标记 Location（带异常处理）
     */
    public static void safeMarkLocation(Location location) {
        if (!libraryLoaded || location == null) {
            return;
        }
        try {
            markLocation(location);
        } catch (Exception e) {
            Log.e(TAG, "Failed to mark location", e);
        }
    }
}
