package com.mdhelper.app;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 스스로 업데이트. 매크로가 깃허브 최신 릴리스에서 받은 새 APK를 MacroDroid '파일 열기' 동작으로 넘기면(content:// + 읽기 허락)
 * 안드로이드 설치 관리자(PackageInstaller)로 자기 자신을 업데이트합니다. 도우미에는 인터넷 권한이 없어서 받는 일은 MacroDroid가 합니다.
 * - 이 앱(같은 패키지 이름)이고 지금보다 낮지 않은 버전만 설치합니다. 서명이 다르면 안드로이드가 설치를 거절합니다.
 * - 안드로이드 12+는 '사용자 확인 없이'를 청합니다(앱이 자기 자신을 업데이트할 때 허용됨). 확인이 필요하면 설치 확인 창을 띄웁니다 —
 *   처음에는 '이 출처 허용'을 한 번 켜야 할 수 있습니다.
 * 확인 창은 보이는 화면에서만 띄울 수 있어서(뒤에서 화면 열기 제한), 결과가 올 때까지 이 투명 화면이 떠 있습니다.
 * '이 출처 허용'이 꺼져 있으면 먼저 그 설정 화면을 열고, 켜고 돌아오면 이어서 업데이트합니다(2026-10-01 가상 폰: 설치 관리자 쪽에서
 * 허용하고 돌아오면 설치가 'User rejected permissions'로 취소됐음).
 */
public class UpdateActivity extends Activity {

    static final String TAG = "MDHelper";
    static final String STATUS = "com.mdhelper.app.INSTALL_STATUS";

    private Uri pending;           // '이 출처 허용'을 켜러 간 동안 기다리는 파일

    private final BroadcastReceiver result = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            int status = i.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE);
            if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                Intent confirm = i.getParcelableExtra(Intent.EXTRA_INTENT);
                if (confirm != null) {
                    try {
                        startActivity(confirm);
                        return;           // 확인 창의 결과가 다시 이리로 옴
                    } catch (Exception e) {
                        Log.w(TAG, "confirm failed: " + e);
                    }
                }
            }
            if (status != PackageInstaller.STATUS_SUCCESS) {
                String msg = i.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                Log.w(TAG, "update failed: " + status + " " + msg);
                if (status != PackageInstaller.STATUS_FAILURE_ABORTED) {
                    toast(getString(R.string.update_failed, msg == null ? String.valueOf(status) : msg));
                }
            }
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        final Uri uri = getIntent().getData();
        if (uri == null) {
            finish();
            return;
        }
        IntentFilter f = new IntentFilter(STATUS);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(result, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(result, f);
        }
        if (!getPackageManager().canRequestPackageInstalls()) {
            pending = uri;
            toast(getString(R.string.update_allow));
            try {
                startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + getPackageName())));
                return;
            } catch (Exception e) {
                Log.w(TAG, "unknown sources settings: " + e);
            }
        }
        start(uri);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (pending == null) return;
        Uri uri = pending;
        if (getPackageManager().canRequestPackageInstalls()) {
            pending = null;
            start(uri);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        // 설정에서 허용하지 않고 돌아옴 → 그만둠
        if (pending != null && !getPackageManager().canRequestPackageInstalls()) {
            pending = null;
            toast(getString(R.string.update_not_allowed));
            finish();
        }
    }

    private void start(final Uri uri) {
        toast(getString(R.string.update_started));
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String err = install(uri);
                if (err != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            toast(err);
                            finish();
                        }
                    });
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        try {
            unregisterReceiver(result);
        } catch (Exception ignored) {
        }
        super.onDestroy();
    }

    /** 받은 파일을 확인하고 설치를 맡깁니다. 문제가 있으면 사용자에게 보일 글자, 없으면 null. */
    private String install(Uri uri) {
        File apk = new File(getCacheDir(), "update.apk");
        try {
            try (InputStream in = getContentResolver().openInputStream(uri); OutputStream out = new FileOutputStream(apk)) {
                copy(in, out);
            }
            PackageInfo got = getPackageManager().getPackageArchiveInfo(apk.getPath(), 0);
            if (got == null || !getPackageName().equals(got.packageName)) {
                return getString(R.string.update_wrong);
            }
            long cur = getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode();
            if (got.getLongVersionCode() < cur) {
                return getString(R.string.update_older);
            }
            PackageInstaller pi = getPackageManager().getPackageInstaller();
            PackageInstaller.SessionParams p = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            p.setAppPackageName(getPackageName());
            if (Build.VERSION.SDK_INT >= 31) {
                p.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
            }
            int id = pi.createSession(p);
            try (PackageInstaller.Session s = pi.openSession(id)) {
                try (InputStream in = new FileInputStream(apk); OutputStream out = s.openWrite("base.apk", 0, apk.length())) {
                    copy(in, out);
                    s.fsync(out);
                }
                Intent cb = new Intent(STATUS).setPackage(getPackageName());
                int flags = PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
                s.commit(PendingIntent.getBroadcast(this, id, cb, flags).getIntentSender());
            }
            Log.i(TAG, "update committed: " + got.getLongVersionCode());
            return null;
        } catch (Exception e) {
            Log.w(TAG, "update error: " + e);
            return getString(R.string.update_failed, String.valueOf(e.getMessage()));
        }
    }

    private static void copy(InputStream in, OutputStream out) throws java.io.IOException {
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
    }

    private void toast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}
