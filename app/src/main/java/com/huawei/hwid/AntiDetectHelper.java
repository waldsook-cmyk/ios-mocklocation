package com.huawei.hwid;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 反检测助手
 * 帮助绕过钉钉、游戏等 App 的位置检测
 */
public class AntiDetectHelper {

    private static final String TAG = "AntiDetectHelper";

    /**
     * 检查当前环境是否容易被检测
     * 返回检测风险等级和建议
     */
    public static DetectionRisk checkDetectionRisk(Context context) {
        DetectionRisk risk = new DetectionRisk();
        
        // 检查1: 开发者选项是否开启（钉钉会检测）
        if (isDeveloperOptionsEnabled(context)) {
            risk.developerOptionsEnabled = true;
            risk.riskLevel++;
            risk.suggestions.add("开发者选项已开启，部分App会检测到");
        }

        // 检查2: USB调试是否开启
        if (isAdbEnabled(context)) {
            risk.adbEnabled = true;
            risk.riskLevel++;
            risk.suggestions.add("USB调试已开启，建议打卡后关闭");
        }

        // 检查3: 是否有模拟位置应用设置
        if (hasMockLocationApp(context)) {
            risk.mockLocationAppSet = true;
            risk.riskLevel++;
            risk.suggestions.add("已设置模拟位置应用");
        }

        // 检查4: Android 版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            risk.riskLevel++;
            risk.suggestions.add("Android 12+ 反射限制较严格");
        }

        return risk;
    }

    private static boolean isDeveloperOptionsEnabled(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isAdbEnabled(Context context) {
        try {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.ADB_ENABLED, 0) == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean hasMockLocationApp(Context context) {
        try {
            String mockApp = Settings.Secure.getString(context.getContentResolver(),
                    "mock_location");
            return mockApp != null && !mockApp.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检测风险信息
     */
    public static class DetectionRisk {
        public int riskLevel = 0;
        public boolean developerOptionsEnabled = false;
        public boolean adbEnabled = false;
        public boolean mockLocationAppSet = false;
        public java.util.ArrayList<String> suggestions = new java.util.ArrayList<>();

        public String getRiskLevelText() {
            if (riskLevel == 0) return "低风险";
            if (riskLevel <= 2) return "中等风险";
            return "高风险";
        }

        public String getSuggestionsText() {
            if (suggestions.isEmpty()) return "环境良好";
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < suggestions.size(); i++) {
                sb.append(i + 1).append(". ").append(suggestions.get(i));
                if (i < suggestions.size() - 1) sb.append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * 钉钉专用：获取打卡建议
     */
    public static String getDingTalkTips() {
        return "【钉钉打卡建议】\n" +
                "1. 先开启模拟位置，再打开钉钉\n" +
                "2. 打卡完成后立即关闭模拟\n" +
                "3. 位置选择公司附近，不要太精确\n" +
                "4. 避免频繁切换位置\n" +
                "5. 打卡后关闭USB调试更安全";
    }
}
