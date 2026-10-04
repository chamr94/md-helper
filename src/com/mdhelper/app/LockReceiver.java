package com.mdhelper.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;

/**
 * 보안 잠금 매크로 ↔ 빠른 덮개(LockService).
 * - com.mdhelper.SET_LOCK + apps("패키지,패키지")·grace(초): 잠글 앱과 유예 시간(매크로의 '앱 실행' 트리거·타이머 설정 — 화면을 끌 때마다 보냄)
 * - com.mdhelper.LOCK_PASS + pkg: 그 앱 지문 통과(그때부터 유예 — 화면을 끄면 앱마다 다시, 매크로와 같은 규칙)
 * - com.mdhelper.LOCK_ASK + pkg: 매크로가 지문을 물음 → 가림 씬이 뜰 때까지 덮개를 더 기다림(느린 폰)
 * - com.mdhelper.LOCK_SKIP + pkg: 매크로가 이번엔 안 물음(유예 안) → 덮개를 바로 걷음
 * - 그 밖(동작 없음): 'LOCK=1;A=접근성 켜짐(1/0);N=잠근 앱 수'로 답함
 * 다른 앱이 보내도 덮개만 바뀔 뿐 지문은 매크로가 따로 물으므로 잠금이 풀리지는 않음.
 */
public class LockReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        SharedPreferences p = LockService.prefs(c);
        String a = i.getAction() == null ? "" : i.getAction();
        String pkg = i.getStringExtra("pkg");
        Log.i(LockService.TAG, "macro " + a + " " + (pkg == null ? "" : pkg) + (i.hasExtra("apps") ? " apps=" + i.getStringExtra("apps") + " grace=" + i.getStringExtra("grace") : ""));
        if (a.equals("com.mdhelper.SET_LOCK")) {
            String apps = i.getStringExtra("apps");
            long grace = 1200;
            try {
                grace = Long.parseLong(String.valueOf(i.getStringExtra("grace")).trim());
            } catch (Exception ignored) {
            }
            HashSet<String> set = new HashSet<String>();
            if (apps != null) for (String x : Arrays.asList(apps.split("[,|\\s]+"))) if (!x.isEmpty()) set.add(x);
            p.edit().putStringSet("apps", set).putLong("grace_ms", grace * 1000L).putInt("miss", 0).apply();
            if (isOrderedBroadcast()) setResultData("OK");
            return;
        }
        if (a.equals("com.mdhelper.LOCK_PASS") && pkg != null) {
            p.edit().putLong("pass_" + pkg, System.currentTimeMillis()).apply();
            LockService.skip(pkg);
            return;
        }
        if (a.equals("com.mdhelper.LOCK_ASK")) {
            LockService.asked(pkg);
            return;
        }
        if (a.equals("com.mdhelper.LOCK_SKIP")) {
            LockService.skip(pkg);
            return;
        }
        setResultData("LOCK=1;A=" + (LockService.instance != null ? 1 : 0) + ";N="
                + p.getStringSet("apps", new HashSet<String>()).size() + ";M=" + p.getInt("miss", 0));
    }
}
