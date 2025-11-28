# 虚拟定位 App

## 功能
- 📍 设置虚拟GPS位置
- 🔍 地名搜索（OpenStreetMap）
- 🏙️ 快捷城市选择（北京、上海、广州、深圳）
- 💾 自动保存上次位置
- 🔔 后台持续运行
- 🛡️ Native 层反检测优化

## 使用步骤

### 1. 编译安装
用 Android Studio 打开 `mock-location` 文件夹，编译并安装到手机

### 2. 开启开发者选项
1. 手机【设置】→【关于手机】
2. 连续点击【版本号】7次，开启开发者模式

### 3. 设置模拟位置应用
1. 【设置】→【开发者选项】
2. 找到【选择模拟位置信息应用】
3. 选择【虚拟定位】

### 4. 使用
1. 打开App，输入经纬度或选择快捷城市
2. 点击【启动模拟】
3. 打开其他App（如地图），位置已更改

## 获取经纬度
- 百度坐标拾取：https://api.map.baidu.com/lbsapi/getpoint/
- 高德坐标拾取：https://lbs.amap.com/tools/picker

## ⚠️ 注意事项
- 仅供学习测试使用
- 部分App会检测模拟位置
- 请勿用于违规用途

## 项目结构
```
mock-location/
├── app/
│   ├── src/main/
│   │   ├── cpp/
│   │   │   ├── CMakeLists.txt
│   │   │   └── native_hook.cpp        # Native 层反检测
│   │   ├── java/.../
│   │   │   ├── MainActivity.java      # 主界面
│   │   │   ├── MockLocationService.java # 模拟位置服务
│   │   │   ├── NativeHook.java        # Native 接口
│   │   │   ├── GeoSearchHelper.java   # 地名搜索
│   │   │   ├── AntiDetectHelper.java  # 检测风险检查
│   │   │   └── SettingsHook.java      # 设置检查
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml
│   │   │   ├── drawable/              # 按钮样式
│   │   │   └── values/                # 主题、字符串
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## 反检测技术

1. **Native 层创建 Location** - 通过 JNI 直接操作，绕过 Java 层检测
2. **清除 Mock 标志** - 反射 + JNI 双重清除 mIsFromMockProvider
3. **真实 GPS 漂移** - 模拟 1-3 米随机漂移
4. **完整卫星信息** - NMEA 语句、DOP 值、信噪比
5. **随机化更新间隔** - 800-1200ms 随机，模拟真实 GPS
6. **Parcel 重建** - 通过序列化清除内部状态
