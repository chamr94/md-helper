package com.mdhelper.app;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener2;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;

/**
 * 기압 상시 기록: 1초마다 기압을 메모리에 기억합니다(최근 30분). 알림 접근 서비스(NotiService)가 켜져 있는 동안 켜 둡니다.
 * 깨우지 않는 센서를 모아 받기(30초)로 받으므로 폰이 잠들어 있으면 센서 칩에 쌓였다가 폰이 깰 때 한꺼번에 들어옵니다(배터리 거의 안 씀).
 * 매크로가 물으면(PressureReceiver) 쌓인 값을 바로 받아(flush) '최근 몇 분 동안 얼마나 내려왔는지'를 계산합니다 —
 * 차에서 내릴 때 도로에서 주차 자리까지 내려온 깊이, 차에 탈 때 집에서 차까지 내려온 깊이.
 */
final class PressureLog {
    static final int SIZE = 1800;              // 1초마다 30분
    private static final long[] T = new long[SIZE];
    private static final float[] P = new float[SIZE];
    private static int n = 0, head = 0;        // head: 다음에 쓸 칸
    private static SensorManager sm;
    private static HandlerThread thread;
    private static volatile boolean flushed;

    private static final SensorEventListener2 L = new SensorEventListener2() {
        @Override
        public void onSensorChanged(SensorEvent e) {
            if (e.values.length > 0) add(wall(e.timestamp), e.values[0]);
        }

        @Override
        public void onAccuracyChanged(Sensor s, int a) {
        }

        @Override
        public void onFlushCompleted(Sensor s) {
            synchronized (PressureLog.class) {
                flushed = true;
                PressureLog.class.notifyAll();
            }
        }
    };

    static synchronized void start(Context c) {
        if (thread != null) return;
        sm = c.getSystemService(SensorManager.class);
        Sensor s = sm == null ? null : sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (s == null) return;
        thread = new HandlerThread("pressure-log");
        thread.start();
        sm.registerListener(L, s, 1_000_000, 30_000_000, new Handler(thread.getLooper()));
    }

    static synchronized void stop() {
        if (thread == null) return;
        sm.unregisterListener(L);
        thread.quitSafely();
        thread = null;
    }

    static synchronized boolean running() {
        return thread != null;
    }

    /** 센서 시각(부팅 뒤 ns) → 벽시계 ms */
    private static long wall(long ts) {
        return System.currentTimeMillis() - (SystemClock.elapsedRealtimeNanos() - ts) / 1_000_000L;
    }

    private static synchronized void add(long t, float p) {
        if (p < 300 || p > 1200) return;
        T[head] = t;
        P[head] = p;
        head = (head + 1) % SIZE;
        if (n < SIZE) n++;
    }

    /** 센서 칩에 쌓인 값을 바로 받습니다(최대 waitMs). */
    static void flush(long waitMs) {
        SensorManager m;
        synchronized (PressureLog.class) {
            if (thread == null) return;
            m = sm;
            flushed = false;
        }
        m.flush(L);
        long end = System.currentTimeMillis() + waitMs;
        synchronized (PressureLog.class) {
            while (!flushed) {
                long left = end - System.currentTimeMillis();
                if (left <= 0) break;
                try {
                    PressureLog.class.wait(left);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    /** from ms 이후의 값들, 시간 순서대로: {시각[], 기압[]} */
    static synchronized Object[] since(long from) {
        int cnt = 0;
        for (int k = 0; k < n; k++) if (T[(head - n + k + SIZE) % SIZE] >= from) cnt++;
        long[] t = new long[cnt];
        float[] p = new float[cnt];
        int j = 0;
        for (int k = 0; k < n; k++) {
            int i = (head - n + k + SIZE) % SIZE;
            if (T[i] >= from) {
                t[j] = T[i];
                p[j] = P[i];
                j++;
            }
        }
        return new Object[]{t, p};
    }

    /** 기억하고 있는 기간(분) — 상태 화면용 */
    static synchronized int minutes() {
        if (n == 0) return 0;
        return (int) ((T[(head - 1 + SIZE) % SIZE] - T[(head - n + SIZE) % SIZE]) / 60000L);
    }
}
