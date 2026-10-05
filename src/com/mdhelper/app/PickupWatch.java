package com.mdhelper.app;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.Locale;

/**
 * 폰을 들어 올리면 메시지 알리미에 알려 줌(방송 com.mdhelper.PICKUP) — 확인 안 한 알림이 새로 왔으면 매크로가 진동으로 알려 줌
 * (2026-10-05 사용자 요청 "받은 알림이 있으면 휴대폰을 들기만 해도 진동으로"). 폰마다 있는 센서가 달라서 차례로 찾음:
 * 1 들어 올림 동작(android.sensor.pick_up_gesture — 센서 칩이 알아보고 그때만 폰을 깨움, 배터리를 거의 안 씀)
 * 2 이름에 pick·lift가 든 제조사 센서(폰을 깨우는 것만)
 * 3 깨우기·흘끗 보기 동작(wake_gesture·glance_gesture)
 * 4 기울기 감지(tilt_detector — 중력 방향이 35° 넘게 바뀌면) + 그때 폰이 평평하지 않고 가려지지 않았는지(주머니·내려놓음이 아님)
 * 없으면 0 — 매크로가 '화면이 켜질 때(들어 올려 깨우기)'로 대신함.
 * 폰을 쓰는 중(화면 켜짐 + 잠금 풀림)에는 알리지 않고, 3초 안에 다시 오면 한 번만. 매크로가 켜 달라고 할 때만(SET_FILTER pick=true) 켬.
 */
final class PickupWatch {
    static final String ACTION = "com.mdhelper.PICKUP";
    static final int TYPE_TILT = 22, TYPE_WAKE = 23, TYPE_GLANCE = 24, TYPE_PICK_UP = 25;   // 숨은 상수(SDK에 없음)

    private static SensorManager sm;
    private static Sensor sensor;
    private static int kind = -1;                  // -1 아직 안 찾음, 0 없음, 1~4 위 차례
    private static boolean running;
    private static long last;
    private static Context app;
    private static HandlerThread thread;
    private static Handler h;

    private static final TriggerEventListener TRIG = new TriggerEventListener() {
        @Override
        public void onTrigger(TriggerEvent e) {
            event();
            rearm();                               // 한 번 오면 저절로 꺼지는 센서 — 다시 켬
        }
    };

    private static final SensorEventListener LISTEN = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent e) {
            event();
        }

        @Override
        public void onAccuracyChanged(Sensor s, int a) {
        }
    };

    /** 이 폰에서 쓸 센서를 찾음(한 번만). 결과: kind·sensor */
    static synchronized int probe(Context c) {
        if (kind >= 0) return kind;
        sm = c.getApplicationContext().getSystemService(SensorManager.class);
        kind = 0;
        if (sm == null) return kind;
        Sensor s = sm.getDefaultSensor(TYPE_PICK_UP);
        if (s != null) return pick(s, 1);
        for (Sensor x : sm.getSensorList(Sensor.TYPE_ALL)) {
            String t = (x.getStringType() + " " + x.getName()).toLowerCase(Locale.ROOT);
            if (x.isWakeUpSensor() && (t.contains("pick_up") || t.contains("pickup") || t.contains("pick up") || t.contains("lift")))
                return pick(x, 2);
        }
        s = sm.getDefaultSensor(TYPE_WAKE);
        if (s == null) s = sm.getDefaultSensor(TYPE_GLANCE);
        if (s != null) return pick(s, 3);
        s = sm.getDefaultSensor(TYPE_TILT);
        if (s != null && s.isWakeUpSensor()) return pick(s, 4);
        return kind;
    }

    private static int pick(Sensor s, int k) {
        sensor = s;
        kind = k;
        return k;
    }

    static String name() {
        return sensor == null ? "" : sensor.getName();
    }

    static synchronized boolean running() {
        return running;
    }

    /** 매크로 설정(pick)과 알림 접근 연결에 맞춰 켜거나 끔 */
    static synchronized void sync(Context c) {
        boolean want = Forward.prefs(c).getBoolean("pick", false) && NotiService.instance != null;
        if (want) start(c);
        else stop();
    }

    private static void start(Context c) {
        if (running) return;
        app = c.getApplicationContext();
        if (probe(app) == 0) return;
        thread = new HandlerThread("pickup");
        thread.start();
        h = new Handler(thread.getLooper());
        boolean ok;
        if (sensor.getReportingMode() == Sensor.REPORTING_MODE_ONE_SHOT) ok = sm.requestTriggerSensor(TRIG, sensor);
        else ok = sm.registerListener(LISTEN, sensor, SensorManager.SENSOR_DELAY_NORMAL, h);
        running = ok;
        if (!ok) {
            thread.quitSafely();
            thread = null;
        }
        Log.i("MDHelper", "pickup: " + (ok ? "watching " : "could not watch ") + sensor.getName() + " (kind " + kind + ")");
    }

    static synchronized void stop() {
        if (!running) return;
        try {
            if (sensor.getReportingMode() == Sensor.REPORTING_MODE_ONE_SHOT) sm.cancelTriggerSensor(TRIG, sensor);
            else sm.unregisterListener(LISTEN);
        } catch (Exception ignored) {
        }
        if (thread != null) thread.quitSafely();
        thread = null;
        running = false;
    }

    private static synchronized void rearm() {
        if (running && sensor != null) sm.requestTriggerSensor(TRIG, sensor);
    }

    private static void event() {
        if (kind == 4) {                           // 기울기는 내려놓을 때·주머니에서도 오므로 지금 자세를 보고 판단
            final Handler hh = h;
            if (hh != null) hh.post(new Runnable() {
                @Override
                public void run() {
                    if (lifted()) fire(false);
                }
            });
            return;
        }
        fire(false);
    }

    /** 기울기 감지 뒤: 평평하게 놓여 있지 않고(가속도 z < 8) 근접 센서가 가려지지 않았으면 든 것으로 봄 (각각 0.4초까지 기다림) */
    private static boolean lifted() {
        float[] g = once(sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 400);
        if (g == null || g.length < 3 || Math.abs(g[2]) >= 8f) return false;
        Sensor prox = sm.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        if (prox == null) return true;
        float[] p = once(prox, 400);
        return p == null || p.length == 0 || p[0] >= Math.min(prox.getMaximumRange(), 3f);
    }

    /** 센서 값 하나를 받아 옴(최대 waitMs) — 이 스레드가 아닌 곳에서 받도록 따로 스레드를 둠 */
    private static float[] once(Sensor s, long waitMs) {
        if (s == null) return null;
        final float[][] box = new float[1][];
        final Object lock = new Object();
        HandlerThread t = new HandlerThread("pickup-once");
        t.start();
        SensorEventListener l = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent e) {
                synchronized (lock) {
                    if (box[0] == null) box[0] = e.values.clone();
                    lock.notifyAll();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor x, int a) {
            }
        };
        sm.registerListener(l, s, SensorManager.SENSOR_DELAY_GAME, new Handler(t.getLooper()));
        long end = SystemClock.uptimeMillis() + waitMs;
        synchronized (lock) {
            while (box[0] == null) {
                long left = end - SystemClock.uptimeMillis();
                if (left <= 0) break;
                try {
                    lock.wait(left);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        sm.unregisterListener(l);
        t.quitSafely();
        return box[0];
    }

    /** 매크로에 알림. force: 시험용(adb) — 폰을 쓰는 중이어도 보냄 */
    static void fire(boolean force) {
        Context c = app;
        if (c == null) return;
        long now = SystemClock.elapsedRealtime();
        PowerManager pm = c.getSystemService(PowerManager.class);
        KeyguardManager km = c.getSystemService(KeyguardManager.class);
        boolean inUse = pm != null && pm.isInteractive() && (km == null || !km.isKeyguardLocked());
        synchronized (PickupWatch.class) {
            if (!force && (inUse || now - last < 3000)) return;
            last = now;
        }
        if (pm != null) pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "mdhelper:pickup").acquire(3000);   // 매크로가 진동할 때까지 깨어 있게
        c.sendBroadcast(new Intent(ACTION).setPackage(Forward.MD).putExtra("k", String.valueOf(kind)));
        Log.i("MDHelper", "pickup: sent (kind " + kind + (force ? ", test" : "") + ")");
    }

    /** adb 시험: am broadcast --user 0 -a com.mdhelper.PICKUP_TEST -n com.mdhelper.app/.FwdReceiver [--es force true] */
    static void test(Context c, boolean force) {
        if (app == null) app = c.getApplicationContext();
        fire(force);
    }
}
