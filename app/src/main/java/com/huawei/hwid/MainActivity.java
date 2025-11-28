package com.huawei.hwid;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private EditText etLatitude, etLongitude, etSearch;
    private Button btnStart, btnStop, btnSearch, btnSave;
    private TextView tvStatus;
    private android.widget.ImageButton btnSettings;
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private PoiSearch mPoiSearch;
    private LinearLayout layoutFavorites;
    private HorizontalScrollView scrollFavorites;
    private SharedPreferences prefs;

    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    private double currentLat = 32.0603;
    private double currentLng = 118.7969;
    private String currentName = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initBaiduMap();
        loadSavedLocation();
        loadFavorites();
        checkPermissions();
    }

    private void initViews() {
        etLatitude = findViewById(R.id.et_latitude);
        etLongitude = findViewById(R.id.et_longitude);
        etSearch = findViewById(R.id.et_search);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);
        btnSearch = findViewById(R.id.btn_search);
        btnSave = findViewById(R.id.btn_save);
        tvStatus = findViewById(R.id.tv_status);
        mMapView = findViewById(R.id.bmapView);
        layoutFavorites = findViewById(R.id.layout_favorites);
        scrollFavorites = findViewById(R.id.scroll_favorites);

        prefs = getSharedPreferences("mock_location", MODE_PRIVATE);

        btnStart.setOnClickListener(v -> startMocking());
        btnStop.setOnClickListener(v -> stopMocking());
        btnSearch.setOnClickListener(v -> performSearch());
        btnSave.setOnClickListener(v -> saveCurrentLocation());
        
        btnSettings = findViewById(R.id.btn_settings);
        btnSettings.setVisibility(View.GONE);
        
        findViewById(R.id.btn_locate).setVisibility(View.GONE);
        
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch();
                return true;
            }
            return false;
        });
    }
    
    private void locateToCurrentPosition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "需要定位权限", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
            return;
        }
        
        Toast.makeText(this, "正在获取当前位置...", Toast.LENGTH_SHORT).show();
        
        android.location.LocationManager lm = (android.location.LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            android.location.Location loc = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (loc == null) {
                loc = lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER);
            }
            if (loc != null) {
                double[] bdCoord = wgs84ToBd09(loc.getLatitude(), loc.getLongitude());
                currentLat = bdCoord[0];
                currentLng = bdCoord[1];
                etLatitude.setText(String.format("%.6f", currentLat));
                etLongitude.setText(String.format("%.6f", currentLng));
                
                LatLng ll = new LatLng(currentLat, currentLng);
                mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(ll, 17));
                Toast.makeText(this, "已定位到当前位置", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "无法获取位置，请确保GPS已开启", Toast.LENGTH_SHORT).show();
            }
        } catch (SecurityException e) {
            Toast.makeText(this, "定位权限被拒绝", Toast.LENGTH_SHORT).show();
        }
    }
    
    private double[] wgs84ToBd09(double lat, double lng) {
        double[] gcj = wgs84ToGcj02(lat, lng);
        double x = gcj[1], y = gcj[0];
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * Math.PI * 3000.0 / 180.0);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * Math.PI * 3000.0 / 180.0);
        double bdLng = z * Math.cos(theta) + 0.0065;
        double bdLat = z * Math.sin(theta) + 0.006;
        return new double[]{bdLat, bdLng};
    }
    
    private double[] wgs84ToGcj02(double lat, double lng) {
        double a = 6378245.0;
        double ee = 0.00669342162296594323;
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - ee * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((a * (1 - ee)) / (magic * sqrtMagic) * Math.PI);
        dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI);
        return new double[]{lat + dLat, lng + dLng};
    }
    
    private double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }
    
    private double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }
    
    private void initBaiduMap() {
        mBaiduMap = mMapView.getMap();
        mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
        mBaiduMap.setTrafficEnabled(false);
        mMapView.showZoomControls(false);
        try {
            mMapView.removeViewAt(1);
        } catch (Exception e) {}
        
        LatLng center = new LatLng(currentLat, currentLng);
        MapStatus.Builder builder = new MapStatus.Builder();
        builder.target(center).zoom(16);
        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));
        
        mBaiduMap.setOnMapStatusChangeListener(new BaiduMap.OnMapStatusChangeListener() {
            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus) {}
            @Override
            public void onMapStatusChangeStart(MapStatus mapStatus, int i) {}
            @Override
            public void onMapStatusChange(MapStatus mapStatus) {}
            @Override
            public void onMapStatusChangeFinish(MapStatus mapStatus) {
                LatLng target = mapStatus.target;
                currentLat = target.latitude;
                currentLng = target.longitude;
                etLatitude.setText(String.format("%.6f", currentLat));
                etLongitude.setText(String.format("%.6f", currentLng));
            }
        });
        
        mPoiSearch = PoiSearch.newInstance();
        mPoiSearch.setOnGetPoiSearchResultListener(new OnGetPoiSearchResultListener() {
            @Override
            public void onGetPoiResult(PoiResult poiResult) {
                if (poiResult == null || poiResult.getAllPoi() == null || poiResult.getAllPoi().isEmpty()) {
                    Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<PoiInfo> poiList = poiResult.getAllPoi();
                
                // 显示搜索结果列表让用户选择
                String[] names = new String[poiList.size()];
                for (int i = 0; i < poiList.size(); i++) {
                    PoiInfo poi = poiList.get(i);
                    names[i] = poi.name + "\n" + (poi.address != null ? poi.address : "");
                }
                
                new AlertDialog.Builder(MainActivity.this)
                    .setTitle("选择地点")
                    .setItems(names, (dialog, which) -> {
                        PoiInfo poi = poiList.get(which);
                        currentLat = poi.location.latitude;
                        currentLng = poi.location.longitude;
                        currentName = poi.name;
                        etLatitude.setText(String.format("%.6f", currentLat));
                        etLongitude.setText(String.format("%.6f", currentLng));
                        mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(poi.location, 17));
                        Toast.makeText(MainActivity.this, "已定位到: " + poi.name, Toast.LENGTH_SHORT).show();
                    })
                    .show();
            }
            @Override
            public void onGetPoiDetailResult(PoiDetailResult poiDetailResult) {}
            @Override
            public void onGetPoiDetailResult(PoiDetailSearchResult poiDetailSearchResult) {}
            @Override
            public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {}
        });
    }

    private void performSearch() {
        String keyword = etSearch.getText().toString().trim();
        if (keyword.isEmpty()) {
            Toast.makeText(this, "请输入搜索关键词", Toast.LENGTH_SHORT).show();
            return;
        }
        mPoiSearch.searchInCity(new PoiCitySearchOption()
                .city("全国")
                .keyword(keyword)
                .pageNum(0)
                .pageCapacity(10));
    }

    private void loadSavedLocation() {
        currentLat = prefs.getFloat("latitude", 32.0603f);
        currentLng = prefs.getFloat("longitude", 118.7969f);
        etLatitude.setText(String.format("%.6f", currentLat));
        etLongitude.setText(String.format("%.6f", currentLng));
    }

    private void saveCurrentLocation() {
        String name = currentName.isEmpty() ? 
            String.format("%.4f, %.4f", currentLat, currentLng) : currentName;
        
        Set<String> favorites = new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));
        String entry = name + "|" + currentLat + "|" + currentLng;
        favorites.add(entry);
        prefs.edit().putStringSet("favorites", favorites).apply();
        
        Toast.makeText(this, "已保存: " + name, Toast.LENGTH_SHORT).show();
        loadFavorites();
    }

    private void loadFavorites() {
        layoutFavorites.removeAllViews();
        Set<String> favorites = prefs.getStringSet("favorites", new HashSet<>());
        
        if (favorites.isEmpty()) {
            scrollFavorites.setVisibility(View.GONE);
            return;
        }
        
        scrollFavorites.setVisibility(View.VISIBLE);
        
        for (String entry : favorites) {
            String[] parts = entry.split("\\|");
            if (parts.length >= 3) {
                String name = parts[0];
                double lat = Double.parseDouble(parts[1]);
                double lng = Double.parseDouble(parts[2]);
                
                Button btn = new Button(this);
                btn.setText(name);
                btn.setTextSize(12);
                btn.setAllCaps(false);
                
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(0, 0, 16, 0);
                btn.setLayoutParams(params);
                
                btn.setOnClickListener(v -> {
                    currentLat = lat;
                    currentLng = lng;
                    currentName = name;
                    etLatitude.setText(String.format("%.6f", lat));
                    etLongitude.setText(String.format("%.6f", lng));
                    LatLng ll = new LatLng(lat, lng);
                    mBaiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLngZoom(ll, 17));
                });
                
                btn.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(this)
                            .setTitle("删除收藏")
                            .setMessage("确定删除 " + name + "?")
                            .setPositiveButton("删除", (d, w) -> {
                                Set<String> favs = new HashSet<>(prefs.getStringSet("favorites", new HashSet<>()));
                                favs.remove(entry);
                                prefs.edit().putStringSet("favorites", favs).apply();
                                loadFavorites();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                    return true;
                });
                
                layoutFavorites.addView(btn);
            }
        }
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void startMocking() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("建议开启悬浮窗")
                    .setMessage("悬浮窗可以显示状态。是否去开启？")
                    .setPositiveButton("去开启", (d, w) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("跳过", (d, w) -> doStartMocking())
                    .show();
            return;
        }
        doStartMocking();
    }
    
    private void doStartMocking() {
        String latStr = etLatitude.getText().toString().trim();
        String lngStr = etLongitude.getText().toString().trim();

        if (latStr.isEmpty() || lngStr.isEmpty()) {
            Toast.makeText(this, "请先选择位置", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double latitude = Double.parseDouble(latStr);
            double longitude = Double.parseDouble(lngStr);

            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
                Toast.makeText(this, "坐标超出范围", Toast.LENGTH_SHORT).show();
                return;
            }

            prefs.edit()
                    .putFloat("latitude", (float) latitude)
                    .putFloat("longitude", (float) longitude)
                    .apply();

            Intent intent = new Intent(this, MockLocationService.class);
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }

            // 延迟更新UI，等服务启动
            new android.os.Handler().postDelayed(() -> {
                updateUI(MockLocationService.isRunning);
                if (MockLocationService.isRunning) {
                    Toast.makeText(this, "✓ 模拟定位已启动", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "启动失败，请检查开发者选项中的模拟位置应用设置", Toast.LENGTH_LONG).show();
                }
            }, 500);

        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效坐标", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopMocking() {
        try {
            stopService(new Intent(this, MockLocationService.class));
        } catch (Exception e) {}
        
        MockLocationService.isRunning = false;
        updateUI(false);
        Toast.makeText(this, "已停止", Toast.LENGTH_SHORT).show();
    }

    private void updateUI(boolean running) {
        runOnUiThread(() -> {
            if (running) {
                tvStatus.setText("● 运行中");
                tvStatus.setBackgroundResource(R.drawable.status_running);
                btnStart.setEnabled(false);
                btnStart.setAlpha(0.5f);
                btnStop.setEnabled(true);
                btnStop.setAlpha(1.0f);
            } else {
                tvStatus.setText("○ 未启动");
                tvStatus.setBackgroundResource(R.drawable.status_bg);
                btnStart.setEnabled(true);
                btnStart.setAlpha(1.0f);
                btnStop.setEnabled(false);
                btnStop.setAlpha(0.5f);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        updateUI(MockLocationService.isRunning);
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        if (mPoiSearch != null) {
            mPoiSearch.destroy();
        }
    }
}
