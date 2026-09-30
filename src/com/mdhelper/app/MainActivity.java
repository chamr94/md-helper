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

    TextView status;
    Button notiBtn, btBtn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(56, 120, 56, 56);
        box.setGravity(Gravity.TOP);

        TextView title = new TextView(this);
        title.setText("MD 도우미");
        title.setTextSize(24);
        title.setTextColor(Color.WHITE);
        box.addView(title);

        TextView about = new TextView(this);
        about.setText("MacroDroid 매크로가 부르면 MacroDroid가 못 하는 일을 대신 하는 앱이에요.\n\n"
                + "• 알림 바로 열기 — 알림이 올 때마다 여는 방법을 기억해 두었다가, 메시지 알리미 목록에서 누르면 그 대화방·메일을 바로 열어요.\n"
                + "• 차 알아보기 — 주차 매크로가 물으면 등록된 블루투스 기기 중 차(핸즈프리·카오디오)를 알려 줘요.\n"
                + "• 기압계 — 주차 매크로가 물으면 기압을 재서 몇 층에 주차했는지 짐작하게 도와요.\n\n"
                + "기억은 이 폰 안에서만 쓰고 파일로 저장하거나 밖으로 보내지 않아요.");
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
        notiBtn.setText("알림 접근 켜기");
        notiBtn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        box.addView(notiBtn);

        btBtn = new Button(this);
        btBtn.setText("블루투스 권한 주기 (차 알아보기)");
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
        status.setText((noti ? "✅ 알림 접근: 켜짐" : "⚠️ 알림 접근: 꺼짐 — 아래 [알림 접근 켜기]에서 'MD 도우미'를 켜 주세요")
                + "\n   기억한 알림: " + NotiService.size() + "개"
                + (NotiService.instance == null && noti ? " (알림 서비스 연결 중)" : "")
                + "\n   알림 접근을 켜 두면 도우미가 늘 깨어 있어서 화면이 꺼져 있어도 기압을 잴 수 있어요."
                + "\n\n" + (bt ? "✅ 블루투스 권한: 켜짐" : "⚠️ 블루투스 권한: 꺼짐 — 아래 버튼으로 켜 주세요")
                + "\n   알아본 차: " + CarReceiver.cars(this).replace("CARS=", "").replace("|", ", ")
                + "\n\n" + (baro ? "✅ 기압계: 있음" : "— 기압계: 이 폰에는 없어요 (층 짐작 없이 동작해요)")
                + "\n\n버전 " + ver);
        notiBtn.setText(noti ? "알림 접근 설정 보기" : "알림 접근 켜기");
        btBtn.setEnabled(!bt);
    }
}
