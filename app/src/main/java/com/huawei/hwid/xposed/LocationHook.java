package com.huawei.hwid.xposed;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import java.lang.reflect.Method;

/**
 * Xposed Hook 模块 - 用于在虚拟环境中 Hook 位置相关 API
 * 这个类会被虚拟化框架加载，Hook 目标应用的位置获取
 */
public class LocationHook {

    private static double mockLatitude = 0;
    private static double mockLongitude = 0;
    private static boolean enabled = false;

    /**
     * 设置模拟位置
     */
    public static void setMockLocation(double lat, double lng) {
        mockLatitude = lat;
        mockLongitude = lng;
        enabled = true;
    }

    /**
     * 停止模拟
     */
    public static void disable() {
        enabled = false;
    }

    /**
     * 创建伪造的 Location 对象
     */
    public static Location createFakeLocation(String provider) {
        Location location = new Location(provider);
        location.setLatitude(mockLatitude);
        location.setLongitude(mockLongitude);
        location.setAltitude(30 + Math.random() * 20);
        location.setAccuracy(5 + (float)(Math.random() * 10));
        location.setSpeed((float)(Math.random() * 0.5));
        location.setBearing((float)(Math.random() * 360));
        location.setTime(System.currentTimeMillis());
        location.setElapsedRealtimeNanos(System.nanoTime());
        
        // 清除 Mock 标志
        try {
            Method method = Location.class.getDeclaredMethod("setIsFromMockProvider", boolean.class);
            method.setAccessible(true);
            method.invoke(location, false);
        } catch (Exception ignored) {}

        // 添加卫星信息
        Bundle extras = new Bundle();
        extras.putInt("satellites", 8 + (int)(Math.random() * 5));
        location.setExtras(extras);

        return location;
    }

    /**
     * 检查是否启用
     */
    public static boolean isEnabled() {
        return enabled;
    }

    public static double getLatitude() {
        return mockLatitude;
    }

    public static double getLongitude() {
        return mockLongitude;
    }
}
