package com.mdhelper.app;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 차 블루투스 목록: 등록된(페어링한) 기기 중 종류가 핸즈프리(0x0408)·카오디오(0x0420)인 것의 이름을 'CARS=이름|이름'으로 답합니다.
 * MacroDroid 셸에서 `am broadcast --user 0 -n com.mdhelper.app/.CarReceiver`로 부르면 결과 줄의 data="…"로 돌아옵니다.
 * 매크로의 DUMP 방식(dumpsys bluetooth_manager)과 같은 기준이라 차 판단 JS(car_calc.js)는 그대로 씁니다.
 * 블루투스 연결 권한이 없으면 NOPERM — 매크로는 그때 DUMP 방식으로 넘어갑니다.
 */
public class CarReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent i) {
        setResultCode(Activity.RESULT_OK);
        setResultData(cars(c));
    }

    static String cars(Context c) {
        if (Build.VERSION.SDK_INT >= 31
                && c.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return "NOPERM";
        }
        BluetoothManager bm = c.getSystemService(BluetoothManager.class);
        BluetoothAdapter a = bm == null ? null : bm.getAdapter();
        if (a == null) return "NOBT";
        Set<String> names = new LinkedHashSet<>();
        try {
            Set<BluetoothDevice> bonded = a.getBondedDevices();
            if (bonded != null) for (BluetoothDevice d : bonded) {
                BluetoothClass bc = d.getBluetoothClass();
                if (bc == null) continue;
                int dc = bc.getDeviceClass();
                if (dc != BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE && dc != BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO) continue;
                add(names, d.getName());
                if (Build.VERSION.SDK_INT >= 30) add(names, d.getAlias());   // 사용자가 바꾼 이름도 (트리거가 어느 쪽을 줄지 몰라서)
            }
        } catch (SecurityException e) {
            return "NOPERM";
        }
        return "CARS=" + String.join("|", names);
    }

    /** DUMP 방식의 tr처럼 백틱·$·역슬래시를 빼고, 결과 전달에 쓰는 큰따옴표(data="…")·구분자(|)·줄바꿈도 뺍니다.
     *  (차 판단 JS는 트리거의 기기 이름과 정확히 같은지 보므로 그 밖의 글자는 그대로 둠) */
    static void add(Set<String> names, String n) {
        if (n == null) return;
        String s = n.replaceAll("[\"`$\\\\|\\r\\n]", "").trim();
        if (!s.isEmpty()) names.add(s);
    }
}
