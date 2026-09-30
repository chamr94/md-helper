package com.mdhelper.app;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * MacroDroid가 부르는 투명 화면. 담아 둔 알림을 찾아 그 알림을 누른 것처럼 열고 바로 닫힙니다.
 * 보이는 화면에서 보내야 다른 앱 화면을 열 수 있어서(뒤에서 화면 열기 제한) 서비스가 아니라 화면으로 받습니다.
 * 넘기는 값(모두 글자): pkg 앱 패키지, title 보낸 사람(알림 제목), text 내용 앞부분, t 받은 시각(ms).
 * 못 찾거나 열지 못하면 그 앱을 엽니다.
 */
public class OpenActivity extends Activity {

    static final String TAG = "MDHelper";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Intent in = getIntent();
        String pkg = extra(in, "pkg"), title = extra(in, "title"), text = extra(in, "text");
        long t = 0;
        try {
            t = Long.parseLong(extra(in, "t").trim());
        } catch (NumberFormatException ignored) {
        }
        boolean opened = false;
        NotiService.Entry e = pkg.isEmpty() ? null : NotiService.find(pkg, title, text, t);
        if (e != null) {
            try {
                ActivityOptions o = ActivityOptions.makeBasic();
                if (Build.VERSION.SDK_INT >= 34) {
                    o.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED);
                }
                e.intent.send(this, 0, null, null, null, null, o.toBundle());
                opened = true;
                // 알림을 직접 누른 것처럼: 누르면 사라지는 알림이면 알림창에서 지움
                NotiService svc = NotiService.instance;
                if (e.autoCancel && !e.removed && svc != null) svc.cancelNotification(e.key);
                Log.i(TAG, "opened " + e.key);
            } catch (Exception ex) {
                Log.w(TAG, "send failed: " + ex);
            }
        } else {
            Log.i(TAG, "not found: " + pkg + " / " + title);
        }
        if (!opened && !pkg.isEmpty()) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try {
                    startActivity(launch);
                } catch (Exception ex) {
                    Log.w(TAG, "launch failed: " + ex);
                }
            }
        }
        finish();
        overridePendingTransition(0, 0);
    }

    static String extra(Intent in, String k) {
        String v = in.getStringExtra(k);
        return v == null ? "" : v;
    }
}
