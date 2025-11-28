package com.huawei.hwid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.location.provider.ProviderProperties;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.core.app.NotificationCompat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;
import android.os.Bundle;

public class MockLocationService extends Service {

    private static final String TAG = "MockLocationService";
    private static final String CHANNEL_ID = "mock_location_channel";
    private static final int NOTIFICATION_ID = 1;

    public static volatile boolean isRunning = false;

    private LocationManager locationManager;
    private Handler handler;
    private Runnable mockRunnable;
    private double latitude = 32.0603;
    private double longitude = 118.7969;
    
    private final Random random = new Random();
    
    // 悬浮窗
    private WindowManager windowManager;
    private View floatView;
    private TextView tvFloatInfo;
    
    // WakeLock 保持CPU运行
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        acquireWakeLock();
    }
    
    private void acquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MockLocation::WakeLock");
            wakeLock.acquire(24 * 60 * 60 * 1000L); // 24小时
            Log.d(TAG, "WakeLock acquired");
        } catch (Exception e) {
            Log.e(TAG, "Failed to acquire WakeLock", e);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        
        if (intent != null) {
            latitude = intent.getDoubleExtra("latitude", 32.0603);
            longitude = intent.getDoubleExtra("longitude", 118.7969);
        }

        // 先启动前台服务
        startForeground(NOTIFICATION_ID, createNotification());
        
        // 显示悬浮窗
        showFloatWindow();
        
        // 启动模拟
        try {
            startMocking();
            isRunning = true;
            Log.d(TAG, "Mock started at: " + latitude + ", " + longitude);
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException - 需要在开发者选项中设置模拟位置应用", e);
            isRunning = false;
            stopSelf();
        } catch (Exception e) {
            Log.e(TAG, "Error starting mock", e);
            isRunning = false;
            stopSelf();
        }

        return START_STICKY;
    }

    private void startMocking() throws SecurityException {
        // 移除旧的 provider
        removeTestProviders();
        
        // 添加所有 provider
        addTestProvider(LocationManager.GPS_PROVIDER);
        
        try {
            addTestProvider(LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.w(TAG, "Could not add network provider", e);
        }
        
        try {
            addTestProvider("fused");
        } catch (Exception e) {
            Log.w(TAG, "Could not add fused provider", e);
        }

        // 高频更新位置
        mockRunnable = new Runnable() {
            private int count = 0;
            
            @Override
            public void run() {
                if (isRunning) {
                    try {
                        setMockLocation(LocationManager.GPS_PROVIDER);
                        setMockLocation(LocationManager.NETWORK_PROVIDER);
                        try {
                            setMockLocation("fused");
                        } catch (Exception ignored) {}
                        
                        // 更新悬浮窗
                        count++;
                        if (count % 5 == 0) {
                            updateFloatWindow();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting location", e);
                    }
                    handler.postDelayed(this, 100); // 100ms 高频更新
                }
            }
        };
        handler.post(mockRunnable);
    }

    private void addTestProvider(String provider) throws SecurityException {
        try {
            locationManager.removeTestProvider(provider);
        } catch (Exception e) {
            // ignore
        }

        // 参数说明:
        // requiresNetwork: false - 不需要网络
        // requiresSatellite: false - 不需要卫星（设为false更像真实GPS）
        // requiresCell: false - 不需要基站
        // hasMonetaryCost: false - 无费用
        // supportsAltitude: true - 支持海拔
        // supportsSpeed: true - 支持速度
        // supportsBearing: true - 支持方向
        // powerRequirement: LOW
        // accuracy: FINE
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            locationManager.addTestProvider(
                    provider,
                    false,  // requiresNetwork
                    false,  // requiresSatellite  
                    false,  // requiresCell
                    false,  // hasMonetaryCost
                    true,   // supportsAltitude
                    true,   // supportsSpeed
                    true,   // supportsBearing
                    ProviderProperties.POWER_USAGE_LOW,
                    ProviderProperties.ACCURACY_FINE
            );
        } else {
            locationManager.addTestProvider(
                    provider,
                    false,  // requiresNetwork
                    false,  // requiresSatellite
                    false,  // requiresCell
                    false,  // hasMonetaryCost
                    true,   // supportsAltitude
                    true,   // supportsSpeed
                    true,   // supportsBearing
                    Criteria.POWER_LOW,
                    Criteria.ACCURACY_FINE
            );
        }
        
        locationManager.setTestProviderEnabled(provider, true);
        
        // 立即设置一次位置，确保 provider 有数据
        setMockLocation(provider);
        
        Log.d(TAG, "Added test provider: " + provider);
    }

    private void setMockLocation(String provider) {
        try {
            // 关键：使用 Bundle 创建 Location，绕过部分检测
            Location location = createRealLikeLocation(provider);
            
            // 百度BD09坐标转WGS84（GPS原始坐标）
            double[] wgs84 = bd09ToWgs84(latitude, longitude);
            location.setLatitude(wgs84[0]);
            location.setLongitude(wgs84[1]);
            location.setAltitude(15 + random.nextDouble() * 5); // 15-20米海拔
            location.setAccuracy(3.0f + random.nextFloat() * 2); // 3-5米精度
            location.setSpeed(0.1f + random.nextFloat() * 0.2f); // 微小速度，像静止但有GPS漂移
            location.setBearing(random.nextFloat() * 360); // 随机方向
            location.setTime(System.currentTimeMillis());
            location.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                location.setBearingAccuracyDegrees(10f);
                location.setVerticalAccuracyMeters(5f);
                location.setSpeedAccuracyMetersPerSecond(0.5f);
            }
            
            // 添加卫星数量等额外信息
            Bundle extras = new Bundle();
            extras.putInt("satellites", 8 + random.nextInt(6)); // 8-13颗卫星
            extras.putInt("satellitesInFix", 6 + random.nextInt(4)); // 6-9颗用于定位
            extras.putFloat("hdop", 0.8f + random.nextFloat() * 0.4f); // 水平精度因子
            extras.putFloat("vdop", 1.0f + random.nextFloat() * 0.5f); // 垂直精度因子
            extras.putFloat("pdop", 1.2f + random.nextFloat() * 0.6f); // 位置精度因子
            location.setExtras(extras);
            
            // 关键：多重方式清除 Mock 标志
            clearMockFlagAdvanced(location);
            
            // 核心：使用 Native 层清除 Mock 标志（模仿定位助手的 mark 方法）
            NativeHook.safeMarkLocation(location);

            locationManager.setTestProviderLocation(provider, location);
        } catch (Exception e) {
            // 静默
        }
    }
    
    // ========== WGS84 转 GCJ02 坐标转换 ==========
    private static final double PI = 3.14159265358979324;
    private static final double A = 6378245.0;
    private static final double EE = 0.00669342162296594323;
    
    private boolean outOfChina(double lat, double lng) {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }
    
    private double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * PI) + 40.0 * Math.sin(y / 3.0 * PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * PI) + 320 * Math.sin(y * PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }
    
    private double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * PI) + 40.0 * Math.sin(x / 3.0 * PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * PI) + 300.0 * Math.sin(x / 30.0 * PI)) * 2.0 / 3.0;
        return ret;
    }
    
    private double[] wgs84ToGcj02(double lat, double lng) {
        if (outOfChina(lat, lng)) {
            return new double[]{lat, lng};
        }
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        return new double[]{lat + dLat, lng + dLng};
    }
    
    // BD09 转 GCJ02（百度坐标转高德坐标）
    private static final double X_PI = 3.14159265358979324 * 3000.0 / 180.0;
    
    private double[] bd09ToGcj02(double bdLat, double bdLng) {
        double x = bdLng - 0.0065;
        double y = bdLat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * X_PI);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * X_PI);
        double gcjLng = z * Math.cos(theta);
        double gcjLat = z * Math.sin(theta);
        return new double[]{gcjLat, gcjLng};
    }
    
    // GCJ02 转 BD09（高德坐标转百度坐标）- 用于验证
    private double[] gcj02ToBd09(double gcjLat, double gcjLng) {
        double z = Math.sqrt(gcjLng * gcjLng + gcjLat * gcjLat) + 0.00002 * Math.sin(gcjLat * X_PI);
        double theta = Math.atan2(gcjLat, gcjLng) + 0.000003 * Math.cos(gcjLng * X_PI);
        double bdLng = z * Math.cos(theta) + 0.0065;
        double bdLat = z * Math.sin(theta) + 0.006;
        return new double[]{bdLat, bdLng};
    }
    
    // BD09 转 WGS84（百度坐标转GPS坐标）
    private double[] bd09ToWgs84(double bdLat, double bdLng) {
        // 先转GCJ02
        double[] gcj = bd09ToGcj02(bdLat, bdLng);
        // 再转WGS84
        return gcj02ToWgs84(gcj[0], gcj[1]);
    }
    
    // GCJ02 转 WGS84（高德坐标转GPS坐标）
    private double[] gcj02ToWgs84(double gcjLat, double gcjLng) {
        if (outOfChina(gcjLat, gcjLng)) {
            return new double[]{gcjLat, gcjLng};
        }
        double dLat = transformLat(gcjLng - 105.0, gcjLat - 35.0);
        double dLng = transformLng(gcjLng - 105.0, gcjLat - 35.0);
        double radLat = gcjLat / 180.0 * PI;
        double magic = Math.sin(radLat);
        magic = 1 - EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((A * (1 - EE)) / (magic * sqrtMagic) * PI);
        dLng = (dLng * 180.0) / (A / sqrtMagic * Math.cos(radLat) * PI);
        double wgsLat = gcjLat - dLat;
        double wgsLng = gcjLng - dLng;
        return new double[]{wgsLat, wgsLng};
    }
    
    /**
     * 创建一个更像真实 GPS 的 Location 对象
     */
    private Location createRealLikeLocation(String provider) {
        Location location;
        try {
            // 方法1: 通过 Bundle 反序列化创建，绕过 mock 标记
            Bundle bundle = new Bundle();
            bundle.putDouble("lat", latitude);
            bundle.putDouble("lng", longitude);
            bundle.putString("provider", provider);
            
            location = new Location(provider);
            
            // 尝试通过反射设置内部状态
            try {
                Field extrasField = Location.class.getDeclaredField("mExtras");
                extrasField.setAccessible(true);
                extrasField.set(location, null);
            } catch (Exception ignored) {}
            
        } catch (Exception e) {
            location = new Location(provider);
        }
        return location;
    }
    
    /**
     * 高级清除 Mock 标志，多种方式尝试
     */
    private void clearMockFlagAdvanced(Location location) {
        // 方法1: 直接设置 mIsFromMockProvider
        try {
            Field field = Location.class.getDeclaredField("mIsFromMockProvider");
            field.setAccessible(true);
            field.setBoolean(location, false);
        } catch (Exception ignored) {}
        
        // 方法2: 设置 mIsMock (某些ROM)
        try {
            Field field = Location.class.getDeclaredField("mIsMock");
            field.setAccessible(true);
            field.setBoolean(location, false);
        } catch (Exception ignored) {}
        
        // 方法3: 设置 mFieldsMask 清除 mock 位
        try {
            Field field = Location.class.getDeclaredField("mFieldsMask");
            field.setAccessible(true);
            int mask = field.getInt(location);
            // 清除第8位 (HAS_MOCK_PROVIDER_BIT = 0x100)
            mask = mask & ~0x100;
            field.setInt(location, mask);
        } catch (Exception ignored) {}
        
        // 方法4: 通过 setIsFromMockProvider 方法（如果存在）
        try {
            Method method = Location.class.getDeclaredMethod("setIsFromMockProvider", boolean.class);
            method.setAccessible(true);
            method.invoke(location, false);
        } catch (Exception ignored) {}
        
        // 方法5: 通过 makeComplete 方法
        try {
            Method method = Location.class.getDeclaredMethod("makeComplete");
            method.setAccessible(true);
            method.invoke(location);
        } catch (Exception ignored) {}
        
        // 方法6: 设置 mHasIsFromMockProviderMask
        try {
            Field field = Location.class.getDeclaredField("mHasIsFromMockProviderMask");
            field.setAccessible(true);
            field.setBoolean(location, false);
        } catch (Exception ignored) {}
        
        // 方法7: 清除 mFlags
        try {
            Field field = Location.class.getDeclaredField("mFlags");
            field.setAccessible(true);
            field.setInt(location, 0);
        } catch (Exception ignored) {}
    }

    private void removeTestProviders() {
        String[] providers = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, "fused"};
        for (String provider : providers) {
            try {
                locationManager.setTestProviderEnabled(provider, false);
                locationManager.removeTestProvider(provider);
            } catch (Exception e) {
                // ignore
            }
        }
    }
    
    // ========== 悬浮窗 ==========
    
    private void showFloatWindow() {
        if (floatView != null) return;
        
        // 检查权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Log.w(TAG, "No overlay permission");
            return;
        }
        
        try {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            
            floatView = LayoutInflater.from(this).inflate(R.layout.float_window, null);
            tvFloatInfo = floatView.findViewById(R.id.tv_float_status);
            updateFloatWindow();
            
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                            WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = 20;
            params.y = 100;
            
            // 拖动
            floatView.setOnTouchListener(new View.OnTouchListener() {
                private int initialX, initialY;
                private float initialTouchX, initialTouchY;
                
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = params.x;
                            initialY = params.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            params.x = initialX + (int) (event.getRawX() - initialTouchX);
                            params.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(floatView, params);
                            return true;
                    }
                    return false;
                }
            });
            
            windowManager.addView(floatView, params);
            Log.d(TAG, "Float window shown");
        } catch (Exception e) {
            Log.e(TAG, "Failed to show float window", e);
        }
    }
    
    private void updateFloatWindow() {
        if (tvFloatInfo != null) {
            handler.post(() -> {
                tvFloatInfo.setText(String.format("📍 %.4f, %.4f", latitude, longitude));
            });
        }
    }
    
    private void removeFloatWindow() {
        if (floatView != null && windowManager != null) {
            try {
                windowManager.removeView(floatView);
            } catch (Exception e) {
                Log.e(TAG, "Failed to remove float window", e);
            }
            floatView = null;
        }
    }

    private void stopMocking() {
        isRunning = false;
        
        if (handler != null && mockRunnable != null) {
            handler.removeCallbacks(mockRunnable);
        }
        
        removeTestProviders();
        removeFloatWindow();
        
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        
        Log.d(TAG, "Mock stopped");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "位置服务",
                    NotificationManager.IMPORTANCE_MIN  // 最低优先级，更隐蔽
            );
            channel.setDescription("位置服务运行中");
            channel.setShowBadge(false);
            channel.setSound(null, null);  // 无声音
            channel.enableVibration(false);  // 无震动
            channel.enableLights(false);  // 无灯光
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("W定位")
                .setContentText("运行中")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service onDestroy");
        stopMocking();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
