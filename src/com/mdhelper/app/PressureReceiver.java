package com.mdhelper.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 기압계: 'P=961.200;N=34[;NET=-7.21;COV=240]'으로 답합니다(주차 층 짐작 — 주차 위치 지도).
 * MacroDroid 셸에서 `am broadcast --user 0 -n com.mdhelper.app/.PressureReceiver [--ei w 초]`로 부르면 결과 줄의 data="…"로 돌아옵니다.
 * - P: 지금 기압(hPa). 상시 기록(PressureLog)이 있으면 최근 5초의 가운데 값, 없으면 1.5초 동안 여러 번 읽은 가운데 값.
 * - NET: 최근 w초(기본 240) 동안 지금 높이가 가장 높았던 곳보다 얼마나 낮은지(m, 음수) — 또는 가장 낮았던 곳보다 얼마나 높은지(양수)
 *   중 큰 쪽. 차에서 내릴 때 = 도로에서 주차 자리까지 내려온(올라온) 높이. 10초마다 가운데 값으로 흔들림(문 닫힘 등)을 줄여 계산합니다.
 *   COV: 그 기간 중 기록이 있는 초. 상시 기록이 없거나 60초보다 짧으면 NET은 빠집니다.
 * 기압계가 없으면 NOSENSOR, 값이 안 오면 NODATA. 높이 계산과 층 배우기는 매크로(parkmap_calc.js)가 합니다.
 */
public class PressureReceiver extends BroadcastReceiver {
    static final long ONE_SHOT_MS = 1500, TIMEOUT_MS = 4000;

    @Override
    public void onReceive(Context c, Intent i) {
        final int w = Math.max(60, Math.min(1500, i.getIntExtra("w", 240)));
        final PendingResult pr = goAsync();
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            String r;
            try {
                r = measure(app, w);
            } catch (Exception e) {
                r = "NODATA";
            }
            pr.setResultCode(Activity.RESULT_OK);
            pr.setResultData(r);
            pr.finish();
        }).start();
    }

    static double height(double p) {
        return 44330 * (1 - Math.pow(p / 1013.25, 1 / 5.255));
    }

    static float median(float[] v, int from, int to) {
        float[] s = Arrays.copyOfRange(v, from, to);
        Arrays.sort(s);
        int k = s.length;
        return k % 2 == 1 ? s[k / 2] : (s[k / 2 - 1] + s[k / 2]) / 2f;
    }

    static String measure(Context c, int w) throws InterruptedException {
        SensorManager sm = c.getSystemService(SensorManager.class);
        Sensor s = sm == null ? null : sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (s == null) return "NOSENSOR";
        long now = System.currentTimeMillis();
        float p = 0;
        int count = 0;
        boolean log = PressureLog.running();
        if (log) {
            PressureLog.flush(1500);
            Object[] recent = PressureLog.since(System.currentTimeMillis() - 5000);
            float[] rp = (float[]) recent[1];
            if (rp.length >= 3) {
                p = median(rp, 0, rp.length);
                count = rp.length;
            }
        }
        if (count == 0) {                       // 상시 기록이 없음: 1.5초 동안 읽기
            float[] one = oneShot(sm, s);
            if (one.length == 0) return "NODATA";
            p = median(one, one.length > 6 ? 2 : 0, one.length);
            count = one.length;
        }
        StringBuilder out = new StringBuilder(String.format(Locale.US, "P=%.3f;N=%d", p, count));
        if (log) {
            Object[] hist = PressureLog.since(now - w * 1000L);
            long[] t = (long[]) hist[0];
            float[] v = (float[]) hist[1];
            if (t.length >= 20) {
                long cov = (now - t[0]) / 1000;
                double hNow = height(p), hMax = hNow, hMin = hNow;
                int a = 0;
                while (a < t.length) {                  // 10초씩 묶어 가운데 값
                    int b = a;
                    while (b < t.length && t[b] - t[a] < 10_000) b++;
                    double h = height(median(v, a, b));
                    hMax = Math.max(hMax, h);
                    hMin = Math.min(hMin, h);
                    a = b;
                }
                double drop = hNow - hMax, rise = hNow - hMin;
                double net = Math.abs(drop) >= rise ? drop : rise;
                if (cov >= 60) out.append(String.format(Locale.US, ";NET=%.2f;COV=%d", net, cov));
            }
        }
        return out.toString();
    }

    /** 상시 기록이 없을 때: 1.5초 동안 여러 번 읽음(값이 적으면 4초까지). */
    static float[] oneShot(SensorManager sm, Sensor s) throws InterruptedException {
        final ArrayList<Float> v = new ArrayList<>();
        HandlerThread th = new HandlerThread("pressure-once");
        th.start();
        final CountDownLatch enough = new CountDownLatch(1);
        final long start = System.currentTimeMillis();
        SensorEventListener l = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent e) {
                synchronized (v) {
                    if (e.values.length > 0 && e.values[0] > 300 && e.values[0] < 1200) v.add(e.values[0]);
                    if (System.currentTimeMillis() - start >= ONE_SHOT_MS && v.size() >= 5) enough.countDown();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        sm.registerListener(l, s, 50_000, new Handler(th.getLooper()));
        enough.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
        sm.unregisterListener(l);
        th.quitSafely();
        synchronized (v) {
            float[] out = new float[v.size()];
            for (int k = 0; k < out.length; k++) out[k] = v.get(k);
            return out;
        }
    }
}
