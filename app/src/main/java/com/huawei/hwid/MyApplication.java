package com.huawei.hwid;

import android.app.Application;
import android.util.Log;

import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MyApplication extends Application {
    
    private static final String TAG = "MyApplication";
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 复制离线地图样式文件
        copyAssetsToFiles();
        
        // 百度地图SDK初始化 - 必须在Application中初始化
        try {
            SDKInitializer.setAgreePrivacy(this, true);
            SDKInitializer.initialize(this);
            SDKInitializer.setCoordType(CoordType.BD09LL);
            Log.d(TAG, "百度地图SDK初始化成功");
        } catch (Exception e) {
            Log.e(TAG, "百度地图SDK初始化失败: " + e.getMessage());
        }
    }
    
    private void copyAssetsToFiles() {
        try {
            String[] files = getAssets().list("cfg");
            if (files == null) return;
            
            File destDir = new File(getFilesDir(), "cfg");
            if (!destDir.exists()) {
                destDir.mkdirs();
            }
            
            for (String fileName : files) {
                copyAssetFile("cfg/" + fileName, new File(destDir, fileName));
            }
            Log.d(TAG, "离线地图样式文件复制完成");
        } catch (Exception e) {
            Log.e(TAG, "复制离线文件失败: " + e.getMessage());
        }
    }
    
    private void copyAssetFile(String assetPath, File destFile) {
        try {
            // 检查是否是目录
            String[] subFiles = getAssets().list(assetPath);
            if (subFiles != null && subFiles.length > 0) {
                // 是目录，递归复制
                if (!destFile.exists()) {
                    destFile.mkdirs();
                }
                for (String subFile : subFiles) {
                    copyAssetFile(assetPath + "/" + subFile, new File(destFile, subFile));
                }
            } else {
                // 是文件，复制
                if (destFile.exists()) return;
                InputStream in = getAssets().open(assetPath);
                FileOutputStream out = new FileOutputStream(destFile);
                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.close();
                in.close();
            }
        } catch (Exception e) {
            // ignore
        }
    }
}
