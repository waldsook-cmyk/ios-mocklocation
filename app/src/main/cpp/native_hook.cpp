#include <jni.h>
#include <string>
#include <android/log.h>
#include <sys/system_properties.h>
#include <unistd.h>
#include <cstring>
#include <cstdlib>
#include <ctime>

#define LOG_TAG "MockLocation-Native"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 缓存的 JavaVM 指针
static JavaVM* g_jvm = nullptr;

// 全局变量
static double g_mockLatitude = 0.0;
static double g_mockLongitude = 0.0;
static bool g_mockEnabled = false;

extern "C" {

JNIEXPORT jint JNI_OnLoad(JavaVM* vm, void* reserved) {
    LOGI("Native library loaded");
    g_jvm = vm;
    srand(time(nullptr));
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL
Java_com_huawei_hwid_NativeHook_setMockLocation(JNIEnv* env, jclass clazz, jdouble lat, jdouble lng) {
    g_mockLatitude = lat;
    g_mockLongitude = lng;
    g_mockEnabled = true;
    LOGI("Mock set: %.6f, %.6f", lat, lng);
}

JNIEXPORT void JNICALL
Java_com_huawei_hwid_NativeHook_stopMock(JNIEnv* env, jclass clazz) {
    g_mockEnabled = false;
}

JNIEXPORT jboolean JNICALL
Java_com_huawei_hwid_NativeHook_isMockEnabled(JNIEnv* env, jclass clazz) {
    return g_mockEnabled ? JNI_TRUE : JNI_FALSE;
}

/**
 * 在 Native 层创建 Location 对象
 * 关键：通过 JNI 直接操作可以绕过某些 Java 层的检测
 */
/**
 * 核心方法：在 Native 层清除 Location 的 Mock 标志
 * 模仿定位助手的 mark(Location) 方法
 * 
 * 关键技术：通过 JNI 直接操作对象字段，绕过 Java 层的访问控制
 */
static void clearMockFlags(JNIEnv* env, jobject location) {
    if (!location) return;
    
    jclass locClass = env->GetObjectClass(location);
    
    // ========== 方法1: 清除所有可能的 boolean 标志字段 ==========
    const char* boolFieldNames[] = {
        "mIsFromMockProvider",      // Android 6-11
        "mIsMock",                  // 某些 ROM
        "mMock",                    // 某些 ROM  
        "mHasIsFromMockProviderMask", // Android 12+
        "mIsFromMockProvider",      // 重复尝试
    };
    
    for (const char* fieldName : boolFieldNames) {
        jfieldID field = env->GetFieldID(locClass, fieldName, "Z");
        if (field && !env->ExceptionCheck()) {
            env->SetBooleanField(location, field, JNI_FALSE);
            LOGI("Cleared bool field: %s", fieldName);
        }
        env->ExceptionClear();
    }
    
    // ========== 方法2: 清除 mFieldsMask 中的 mock 位 ==========
    // Android 12+ 使用 mFieldsMask 位掩码
    jfieldID maskField = env->GetFieldID(locClass, "mFieldsMask", "I");
    if (maskField && !env->ExceptionCheck()) {
        jint mask = env->GetIntField(location, maskField);
        // HAS_MOCK_PROVIDER_BIT = 0x100 (第8位)
        // 还有其他可能的位: 0x200, 0x400 等
        mask = mask & ~0x100 & ~0x200 & ~0x400;
        env->SetIntField(location, maskField, mask);
        LOGI("Cleared mFieldsMask: 0x%x", mask);
    }
    env->ExceptionClear();
    
    // ========== 方法3: 清除 mFlags 字段 ==========
    jfieldID flagsField = env->GetFieldID(locClass, "mFlags", "I");
    if (flagsField && !env->ExceptionCheck()) {
        jint flags = env->GetIntField(location, flagsField);
        // 保留其他标志，只清除 mock 相关的位
        flags = flags & ~0x100 & ~0x200;
        env->SetIntField(location, flagsField, flags);
        LOGI("Cleared mFlags: 0x%x", flags);
    }
    env->ExceptionClear();
    
    // ========== 方法4: 调用 setter 方法 ==========
    jmethodID setMockMethod = env->GetMethodID(locClass, "setIsFromMockProvider", "(Z)V");
    if (setMockMethod && !env->ExceptionCheck()) {
        env->CallVoidMethod(location, setMockMethod, JNI_FALSE);
        LOGI("Called setIsFromMockProvider(false)");
    }
    env->ExceptionClear();
    
    // Android 12+ 的 setMock 方法
    jmethodID setMock = env->GetMethodID(locClass, "setMock", "(Z)V");
    if (setMock && !env->ExceptionCheck()) {
        env->CallVoidMethod(location, setMock, JNI_FALSE);
        LOGI("Called setMock(false)");
    }
    env->ExceptionClear();
    
    // ========== 方法5: 尝试 reset 方法 ==========
    jmethodID resetMethod = env->GetMethodID(locClass, "reset", "()V");
    if (resetMethod && !env->ExceptionCheck()) {
        // 不调用 reset，因为会清除所有数据
        // 只是检测是否存在
        LOGI("reset method exists but not called");
    }
    env->ExceptionClear();
    
    // ========== 方法6: 设置 Provider 为真实的 GPS ==========
    jmethodID setProvider = env->GetMethodID(locClass, "setProvider", "(Ljava/lang/String;)V");
    if (setProvider && !env->ExceptionCheck()) {
        jstring gpsProvider = env->NewStringUTF("gps");
        env->CallVoidMethod(location, setProvider, gpsProvider);
        env->DeleteLocalRef(gpsProvider);
        LOGI("Set provider to 'gps'");
    }
    env->ExceptionClear();
}

/**
 * 标记 Location 对象 - 模仿定位助手的 mark(Location) 方法
 * 清除 mock 标志，使其看起来像真实 GPS 位置
 */
JNIEXPORT void JNICALL
Java_com_huawei_hwid_NativeHook_markLocation(JNIEnv* env, jclass clazz, jobject location) {
    if (!location) return;
    clearMockFlags(env, location);
    LOGI("Location marked (mock flags cleared)");
}

JNIEXPORT jobject JNICALL
Java_com_huawei_hwid_NativeHook_createNativeLocation(JNIEnv* env, jclass clazz, jstring provider) {
    if (!g_mockEnabled) return nullptr;

    // 获取 Location 类和方法
    jclass locClass = env->FindClass("android/location/Location");
    if (!locClass) return nullptr;

    jmethodID ctor = env->GetMethodID(locClass, "<init>", "(Ljava/lang/String;)V");
    jobject location = env->NewObject(locClass, ctor, provider);
    if (!location) return nullptr;

    // 微小随机漂移 (约1-3米)
    double latOffset = ((rand() % 60) - 30) / 1000000.0;
    double lngOffset = ((rand() % 60) - 30) / 1000000.0;

    // 设置位置
    jmethodID setLat = env->GetMethodID(locClass, "setLatitude", "(D)V");
    jmethodID setLng = env->GetMethodID(locClass, "setLongitude", "(D)V");
    jmethodID setAlt = env->GetMethodID(locClass, "setAltitude", "(D)V");
    jmethodID setAcc = env->GetMethodID(locClass, "setAccuracy", "(F)V");
    jmethodID setSpd = env->GetMethodID(locClass, "setSpeed", "(F)V");
    jmethodID setBrg = env->GetMethodID(locClass, "setBearing", "(F)V");
    jmethodID setTime = env->GetMethodID(locClass, "setTime", "(J)V");
    jmethodID setElapsed = env->GetMethodID(locClass, "setElapsedRealtimeNanos", "(J)V");

    env->CallVoidMethod(location, setLat, g_mockLatitude + latOffset);
    env->CallVoidMethod(location, setLng, g_mockLongitude + lngOffset);
    env->CallVoidMethod(location, setAlt, 30.0 + (rand() % 40));
    env->CallVoidMethod(location, setAcc, 5.0f + (rand() % 100) / 10.0f);
    env->CallVoidMethod(location, setSpd, (rand() % 30) / 100.0f);
    env->CallVoidMethod(location, setBrg, (float)(rand() % 360));

    // 时间戳
    struct timespec ts;
    clock_gettime(CLOCK_REALTIME, &ts);
    jlong timeMs = ts.tv_sec * 1000LL + ts.tv_nsec / 1000000LL;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    jlong elapsedNanos = ts.tv_sec * 1000000000LL + ts.tv_nsec;

    env->CallVoidMethod(location, setTime, timeMs);
    env->CallVoidMethod(location, setElapsed, elapsedNanos);

    // 关键：清除所有 mock 标志
    clearMockFlags(env, location);

    // 添加 extras
    jmethodID setExtras = env->GetMethodID(locClass, "setExtras", "(Landroid/os/Bundle;)V");
    jclass bundleClass = env->FindClass("android/os/Bundle");
    jmethodID bundleCtor = env->GetMethodID(bundleClass, "<init>", "()V");
    jobject extras = env->NewObject(bundleClass, bundleCtor);

    jmethodID putInt = env->GetMethodID(bundleClass, "putInt", "(Ljava/lang/String;I)V");
    jmethodID putFloat = env->GetMethodID(bundleClass, "putFloat", "(Ljava/lang/String;F)V");

    int satellites = 7 + rand() % 6;
    env->CallVoidMethod(extras, putInt, env->NewStringUTF("satellites"), satellites);
    env->CallVoidMethod(extras, putInt, env->NewStringUTF("satellitesInFix"), satellites);
    env->CallVoidMethod(extras, putFloat, env->NewStringUTF("hdop"), 0.8f + (rand() % 12) / 10.0f);
    env->CallVoidMethod(extras, putFloat, env->NewStringUTF("vdop"), 1.0f + (rand() % 15) / 10.0f);

    env->CallVoidMethod(location, setExtras, extras);

    return location;
}

/**
 * 检查系统属性 - 用于检测是否有检测相关的属性
 */
JNIEXPORT jstring JNICALL
Java_com_huawei_hwid_NativeHook_getSystemProperty(JNIEnv* env, jclass clazz, jstring key) {
    const char* keyStr = env->GetStringUTFChars(key, nullptr);
    char value[PROP_VALUE_MAX] = {0};
    
    __system_property_get(keyStr, value);
    
    env->ReleaseStringUTFChars(key, keyStr);
    return env->NewStringUTF(value);
}

} // extern "C"
