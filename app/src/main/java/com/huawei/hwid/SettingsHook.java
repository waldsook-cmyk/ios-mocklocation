package com.huawei.hwid;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

/**
 * 尝试通过 ContentProvider 代理来隐藏某些设置
 * 注意：这种方式在非 Root 环境下效果有限
 */
public class SettingsHook {

    private static final String TAG = "SettingsHook";

    /**
     * 检查并报告当前的检测风险
     */
    public static void checkAndLogRisks(Context context) {
        Log.i(TAG, "=== 检测风险检查 ===");
        
        // 检查开发者选项
        try {
            int devEnabled = Settings.Global.getInt(
                    context.getContentResolver(),
                    Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
            Log.i(TAG, "开发者选项: " + (devEnabled == 1 ? "已开启 ⚠️" : "已关闭 ✓"));
        } catch (Exception e) {
            Log.e(TAG, "无法检查开发者选项", e);
        }

        // 检查 ADB
        try {
            int adbEnabled = Settings.Global.getInt(
                    context.getContentResolver(),
                    Settings.Global.ADB_ENABLED, 0);
            Log.i(TAG, "ADB调试: " + (adbEnabled == 1 ? "已开启 ⚠️" : "已关闭 ✓"));
        } catch (Exception e) {
            Log.e(TAG, "无法检查ADB状态", e);
        }

        // 检查模拟位置应用
        try {
            String mockApp = Settings.Secure.getString(
                    context.getContentResolver(),
                    "mock_location");
            Log.i(TAG, "模拟位置应用: " + (mockApp != null ? mockApp + " ⚠️" : "未设置 ✓"));
        } catch (Exception e) {
            Log.e(TAG, "无法检查模拟位置应用", e);
        }

        Log.i(TAG, "=== 检查完成 ===");
    }

    /**
     * 获取绕过建议
     */
    public static String getBypassTips() {
        return "【绕过检测建议】\n\n" +
                "1. 先启动本应用的模拟位置\n" +
                "2. 再打开钉钉（不要提前打开）\n" +
                "3. 打卡完成后立即关闭模拟\n" +
                "4. 如果被检测，尝试：\n" +
                "   - 清除钉钉数据重新登录\n" +
                "   - 打卡后关闭USB调试\n" +
                "   - 使用飞行模式关闭WiFi\n\n" +
                "⚠️ 本应用使用 Native 层优化\n" +
                "可提高绕过成功率";
    }
}
