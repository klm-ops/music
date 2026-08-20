package com.app.music;

import android.content.Context;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * VHAL 物理按键信号读取器。
 *
 * 车机方控（方向盘/面板的 PREV/NEXT 物理按键）信号由车端写入 VHAL，
 * 本类通过反射访问 android.car 的 CarPropertyManager 读取该信号，
 * 避免直接依赖 android.car 库导致在非车机设备上编译/运行异常。
 *
 * 所有 get 调用均包裹 try-catch 并打印错误日志，
 * 防止开机后车端尚未写入信号时 get 抛异常导致应用崩溃。
 */
public final class VhalSignalReader {

    private static final String TAG = "VhalSignal";

    // 车端物理按键信号属性ID（厂商扩展段），需按实际 VHAL 定义填写
    // TODO: 替换为实际属性 ID
    private static final int PROPERTY_ID_KEY_SIGNAL = 0;
    private static final int AREA_ID_GLOBAL = 0;

    // 信号取值（需与车端 VHAL 写入的编码保持一致）
    public static final int SIGNAL_NONE = -1; // 无信号 / 读取失败
    public static final int SIGNAL_PREV = 1;  // 上一曲 / 上翻 / 低频搜台
    public static final int SIGNAL_NEXT = 2;  // 下一曲 / 下翻 / 高频搜台

    private final Object car;
    private final Object propertyManager;
    private final Method getIntPropertyMethod;

    private VhalSignalReader(Object car, Object propertyManager, Method getIntPropertyMethod) {
        this.car = car;
        this.propertyManager = propertyManager;
        this.getIntPropertyMethod = getIntPropertyMethod;
    }

    /**
     * 建立 VHAL 连接并获取 CarPropertyManager。
     *
     * @return 连接成功返回读取器实例，失败返回 null（不抛异常）
     */
    public static VhalSignalReader connect(Context context) {
        Object car = null;
        try {
            if (context == null) {
                Log.w(TAG, "连接VHAL失败: context为空");
                return null;
            }
            Class<?> carClass = Class.forName("android.car.Car");
            Method createCar = carClass.getMethod("createCar", Context.class);
            car = createCar.invoke(null, context);
            if (car == null) {
                Log.w(TAG, "连接VHAL失败: Car.createCar返回null");
                return null;
            }
            Method getCarManager = carClass.getMethod("getCarManager", String.class);
            Object propertyManager = getCarManager.invoke(car, "property");
            if (propertyManager == null) {
                Log.w(TAG, "连接VHAL失败: CarPropertyManager为空");
                disconnectQuietly(car);
                return null;
            }
            Method getIntProperty = propertyManager.getClass().getMethod(
                    "getIntProperty", int.class, int.class);
            Log.d(TAG, "VHAL CarPropertyManager连接成功");
            return new VhalSignalReader(car, propertyManager, getIntProperty);
        } catch (Throwable t) {
            Log.e(TAG, "连接VHAL失败(设备可能不支持android.car或车端尚未就绪)", t);
            disconnectQuietly(car);
            return null;
        }
    }

    /**
     * 读取当前车端物理按键信号。
     *
     * @return SIGNAL_PREV / SIGNAL_NEXT / SIGNAL_NONE（失败时返回 SIGNAL_NONE）
     */
    public int readKeySignal() {
        try {
            if (propertyManager == null || getIntPropertyMethod == null) {
                return SIGNAL_NONE;
            }
            Object rawValue = getIntPropertyMethod.invoke(
                    propertyManager, PROPERTY_ID_KEY_SIGNAL, AREA_ID_GLOBAL);
            int signal = rawValue instanceof Number ? ((Number) rawValue).intValue() : SIGNAL_NONE;
            if (signal == SIGNAL_PREV || signal == SIGNAL_NEXT) {
                return signal;
            }
            return SIGNAL_NONE;
        } catch (Throwable t) {
            Log.e(TAG, "读取VHAL按键信号异常(车端可能尚未写入信号)", t);
            return SIGNAL_NONE;
        }
    }

    /** 释放 VHAL 连接，失败不抛异常。 */
    public void close() {
        disconnectQuietly(car);
    }

    private static void disconnectQuietly(Object car) {
        if (car == null) {
            return;
        }
        try {
            car.getClass().getMethod("disconnect").invoke(car);
        } catch (Throwable t) {
            Log.w(TAG, "断开VHAL连接异常", t);
        }
    }
}
