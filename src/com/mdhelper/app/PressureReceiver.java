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
import java.util.Collections;
import java.util.Locale;

/**
 * 기압계: 1.5초 동안 기압(hPa)을 여러 번 읽어 가운데 값을 'P=1008.123;N=24'로 답합니다(주차 층 짐작 — 주차 위치 지도).
 * MacroDroid 셸에서 `am broadcast --user 0 -n com.mdhelper.app/.PressureReceiver`로 부르면 결과 줄의 data="…"로 돌아옵니다.
 * 한 번만 읽으면 흔들리므로 여러 번 읽고 처음 두 값(센서가 막 켜질 때)은 뺍니다. 기압계가 없으면 NOSENSOR, 4초 안에 값이 없으면 NODATA.
 * 높이 차이 계산과 층 배우기는 매크로(parkmap_calc.js)가 합니다 — 이 앱은 값만 읽습니다.
 */
public class PressureReceiver extends BroadcastReceiver {
    static final long SAMPLE_MS = 1500, STEP_MS = 250, TIMEOUT_MS = 4000;

    @Override
    public void onReceive(Context c, Intent i) {
        final SensorManager sm = c.getSystemService(SensorManager.class);
        final Sensor s = sm == null ? null : sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (s == null) {
            setResultCode(Activity.RESULT_OK);
            setResultData("NOSENSOR");
            return;
        }
        final PendingResult pr = goAsync();
        final HandlerThread t = new HandlerThread("pressure");
        t.start();
        final Handler h = new Handler(t.getLooper());
        final ArrayList<Float> v = new ArrayList<>();
        final long start = System.currentTimeMillis();
        final SensorEventListener l = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent e) {
                if (e.values.length > 0 && e.values[0] > 300 && e.values[0] < 1200) v.add(e.values[0]);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };
        sm.registerListener(l, s, 50_000, h);          // 0.05초마다 (센서가 되는 만큼)
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                long spent = System.currentTimeMillis() - start;
                if (v.size() < 5 && spent < TIMEOUT_MS) {   // 아직 값이 적으면 조금 더 (최대 4초)
                    h.postDelayed(this, STEP_MS);
                    return;
                }
                sm.unregisterListener(l);
                pr.setResultCode(Activity.RESULT_OK);
                pr.setResultData(result(v));
                pr.finish();
                t.quitSafely();
            }
        }, SAMPLE_MS);
    }

    static String result(ArrayList<Float> v) {
        if (v.isEmpty()) return "NODATA";
        ArrayList<Float> w = new ArrayList<>(v.size() > 6 ? v.subList(2, v.size()) : v);
        Collections.sort(w);
        int n = w.size();
        double med = n % 2 == 1 ? w.get(n / 2) : (w.get(n / 2 - 1) + w.get(n / 2)) / 2.0;
        return String.format(Locale.US, "P=%.3f;N=%d", med, n);
    }
}
