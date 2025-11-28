# ============ 基础配置 ============
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# ============ 保留项 ============

# 保留 Native 方法
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留 NativeHook 类
-keep class com.huawei.hwid.NativeHook { *; }

# 保留 Service
-keep class com.huawei.hwid.MockLocationService { *; }

# 保留 Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# 保留 R 文件
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 保留枚举
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留注解
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# ============ 第三方库 ============

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# ============ 混淆优化 ============

# 移除日志
-assumenosideeffects class android.util.Log {
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# 优化
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*
