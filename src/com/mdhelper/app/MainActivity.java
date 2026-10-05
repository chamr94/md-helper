package com.mdhelper.app;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * 상태 화면: 권한(알림 접근·블루투스)이 켜져 있는지와 기능별 상태. 꺼져 있으면 켜는 버튼.
 * 매크로가 도우미를 설치한 직후 이 화면을 열어 주므로, 무엇을 켜야 하는지 여기서 안내합니다.
 */
public class MainActivity extends Activity {

    TextView status, restricted;
    Button notiBtn, infoBtn, btBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(56, 120, 56, 56);
        box.setGravity(Gravity.TOP);

        TextView title = new TextView(this);
        title.setText(R.string.app_name);
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        box.addView(title);

        TextView about = new TextView(this);
        about.setText(R.string.about);
        about.setTextSize(14);
        about.setTextColor(Color.parseColor("#8B95A1"));
        about.setPadding(0, 24, 0, 24);
        box.addView(about);

        status = new TextView(this);
        status.setTextSize(16);
        status.setTextColor(Color.WHITE);
        status.setPadding(0, 12, 0, 36);
        box.addView(status);

        notiBtn = new Button(this);
        notiBtn.setText(R.string.btn_noti_on);
        notiBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        box.addView(notiBtn);

        // 브라우저로 받아 설치한 앱은 안드로이드 13+에서 알림 접근이 '제한된 설정'으로 막힘 → 앱 정보 ⋮ → '제한된 설정 허용'
        // (2026-10-01 가상 폰에서 재현 — 사용자 제보 "다른 사람 폰에서 설치가 막힘"). 알림 접근이 꺼져 있을 때만 보임
        restricted = new TextView(this);
        restricted.setText(R.string.restricted_hint);
        restricted.setTextSize(14);
        restricted.setTextColor(Color.parseColor("#FFB020"));
        restricted.setPadding(0, 24, 0, 8);
        box.addView(restricted);

        infoBtn = new Button(this);
        infoBtn.setText(R.string.btn_app_info);
        infoBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null))));
        box.addView(infoBtn);

        btBtn = new Button(this);
        btBtn.setText(R.string.btn_bt);
        btBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 31) requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        });
        box.addView(btBtn);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#101826"));
        scroll.setFillViewport(true);
        scroll.addView(box);
        setContentView(scroll);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        onResume();
    }

    @Override
    protected void onResume() {
        super.onResume();
        NotificationManager nm = getSystemService(NotificationManager.class);
        boolean noti = nm != null && nm.isNotificationListenerAccessGranted(new ComponentName(this, NotiService.class));
        boolean bt = Build.VERSION.SDK_INT < 31
                || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        SensorManager sm = getSystemService(SensorManager.class);
        boolean baro = sm != null && sm.getDefaultSensor(Sensor.TYPE_PRESSURE) != null;
        String ver = "";
        try {
            ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        // 글자는 res/values(영어)·values-ko(한국어) — 폰 언어나 앱별 언어 설정(안드로이드 13+)을 따름
        status.setText(getString(noti ? R.string.noti_on : R.string.noti_off)
                + "\n" + getString(R.string.remembered, NotiService.size())
                + (NotiService.instance == null && noti ? getString(R.string.connecting) : "")
                + "\n" + getString(R.string.noti_note)
                + "\n" + getString(Forward.on(this) ? R.string.fwd_on : R.string.fwd_off, Forward.count(this))
                + "\n" + pickupLine()
                + "\n\n" + getString(bt ? R.string.bt_on : R.string.bt_off)
                + "\n" + getString(R.string.cars, CarReceiver.cars(this).replace("CARS=", "").replace("|", ", "))
                + "\n\n" + (!baro ? getString(R.string.baro_none)
                        : PressureLog.running() ? getString(R.string.baro_logging, PressureLog.minutes())
                        : getString(R.string.baro_idle))
                + "\n\n" + getString(R.string.version, ver));
        notiBtn.setText(noti ? R.string.btn_noti_view : R.string.btn_noti_on);
        int hint = !noti && Build.VERSION.SDK_INT >= 33 ? android.view.View.VISIBLE : android.view.View.GONE;
        restricted.setVisibility(hint);
        infoBtn.setVisibility(hint);
        btBtn.setEnabled(!bt);
    }

    /** 폰을 들면 알리기(메시지 알리미): 쓰는 센서 — 없으면 매크로가 화면이 켜질 때로 대신함 */
    private String pickupLine() {
        if (PickupWatch.probe(this) == 0) return getString(R.string.pickup_none);
        return getString(PickupWatch.running() ? R.string.pickup_on : R.string.pickup_off, PickupWatch.name());
    }
}
