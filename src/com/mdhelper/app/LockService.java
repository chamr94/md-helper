package com.mdhelper.app;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;

import java.util.HashSet;
import java.util.Set;

/**
 * 보안 잠금 매크로의 빠른 덮개 (2026-10-04 사용자 "로그캣 방식 말고 앱을 키자마자 화면 바로 가릴 수 있게").
 * MacroDroid의 '앱 실행' 트리거는 MacroDroid 접근성 설정(notificationTimeout 200ms)과 처리 때문에 앱이 그려지고 0.25초쯤 뒤에 와서,
 * 그동안 잠근 앱(갤러리 사진 등)이 보였음. 이 접근성 서비스는 기다리지 않고(notificationTimeout 0) 창이 바뀌는 순간을 받아,
 * 잠근 앱이 앞에 오면 그 위에 불투명한 검은 덮개(접근성 덮개 창 — 다른 앱 위에 그리기 권한이 필요 없음)를 바로 그림.
 * 지문을 묻고 기록하는 일은 매크로가 그대로 하고, 덮개는 매크로의 가림 화면(SceneDisplayActivity)이 뜨면 걷음.
 * - 잠글 앱·유예(매크로와 같은 규칙: 앱마다 마지막 통과 + 유예 시간, 화면을 끄면 모두 다시 물음)는 매크로가 LockReceiver로 알려 줌.
 * - 매크로가 '이번엔 안 물음'이라고 하거나(LOCK_SKIP) 다른 앱·홈으로 가면 바로 걷음. 매크로가 2.5초 안에 묻는지 알려 주지 않으면(묻는다고 했으면
 *   6초 안에 가림 씬이 안 뜨면) 걷고(매크로가 꺼졌을 때 막히지 않게), 그런 일이 두 번 이어지면 매크로가 다시 알려 줄 때까지 덮지 않음.
 * - 화면 내용은 읽지 않음(canRetrieveWindowContent=false) — 앞에 온 앱의 패키지 이름만 봄.
 */
public class LockService extends AccessibilityService {

    static final String TAG = "MDLock";
    static final String MD = "com.arlosoft.macrodroid";
    static final long COVER_MAX_MS = 2500;      // 매크로가 묻는지·안 묻는지 알려 줄 때까지
    static final long ASKED_MAX_MS = 6000;      // 묻는다고 한 뒤 가림 씬이 뜰 때까지
    static volatile LockService instance;

    private final Handler h = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private View cover;
    private String coverPkg = "";
    private long coverAt;
    private String lastPkg = "";

    private final Runnable timeout = new Runnable() {
        @Override
        public void run() {
            if (cover == null) return;
            Log.w(TAG, "timeout: macro did not cover " + coverPkg);
            SharedPreferences p = prefs(LockService.this);
            int miss = p.getInt("miss", 0) + 1;
            p.edit().putInt("miss", miss).apply();
            remove("timeout");
        }
    };

    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            prefs(c).edit().putLong("off_at", System.currentTimeMillis()).apply();   // 매크로처럼: 화면을 끄면 잠근 앱 모두 각자 다시 물음
            // 앞 앱(lastPkg)은 그대로: 잠근 앱을 연 채 끈 경우는 매크로가 '화면 끔' 가림 씬으로 덮고 잠금을 풀면 지문을 물음. MacroDroid 씬은
            // 불투명하게 그려도 창은 투명 창이라, 잠금을 풀 때 아래 앱도 창 바뀜을 보내서 여기서 다시 덮으면 매크로 씬과 겹쳐 시간 초과가 남
            remove("screen off");       // 덮개는 잠금 화면보다 위라, 남겨 두면 켰을 때 잠금을 못 풂(앱은 매크로가 '화면 끔' 가림 씬으로 덮음)
        }
    };

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("lock", Context.MODE_PRIVATE);
    }

    @Override
    protected void onServiceConnected() {
        instance = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(screenOff, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenOff, f);
        Log.i(TAG, "connected");
    }

    @Override
    public void onDestroy() {
        instance = null;
        try {
            unregisterReceiver(screenOff);
        } catch (Exception ignored) {
        }
        remove("destroy");
        super.onDestroy();
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || e.getPackageName() == null) return;
        String pkg = e.getPackageName().toString();
        String cls = e.getClassName() == null ? "" : e.getClassName().toString();
        long now = SystemClock.uptimeMillis();
        Log.i(TAG, "ev " + pkg + "/" + cls + " up=" + now);
        if (MD.equals(pkg)) {
            // 매크로의 가림 화면이 떴으면 덮개를 걷음(지문 창이 덮개 아래에 가리지 않게). MacroDroid 화면은 앞 앱 바뀜으로 치지 않음
            if (cover != null && cls.contains("SceneDisplayActivity")) remove("scene");
            return;
        }
        if (pkg.equals(getPackageName()) || isSystemUi(pkg)) return;   // 알림창·지문 창·자판 등은 앞 앱 바뀜이 아님
        if (cover != null && !pkg.equals(coverPkg)) remove("left");      // 홈·최근 앱·다른 앱으로 감
        if (pkg.equals(lastPkg)) return;                                 // 같은 앱 안의 화면 바뀜
        lastPkg = pkg;
        if (needCover(pkg)) show(pkg, now);
    }

    static boolean isSystemUi(String pkg) {
        // 삼성 지문 창은 com.samsung.android.biometrics.app.setting이 띄움(2026-10-04 폰 — 다른 앱으로 보고 덮개를 0.05초 일찍 걷었음)
        return "com.android.systemui".equals(pkg) || pkg.contains("inputmethod") || pkg.contains("honeyboard")
                || pkg.startsWith("com.samsung.android.biometrics") || "android".equals(pkg);
    }

    /** 매크로와 같은 판단: 잠근 앱이고, (통과한 적 없음 || 유예가 지남 || 마지막 통과가 마지막 화면 끔보다 앞)이면 덮음 */
    boolean needCover(String pkg) {
        SharedPreferences p = prefs(this);
        if (p.getInt("miss", 0) >= 2) return false;                      // 매크로가 연달아 안 가림 → 꺼졌다고 봄
        Set<String> apps = p.getStringSet("apps", new HashSet<String>());
        if (!apps.contains(pkg)) return false;
        long pass = p.getLong("pass_" + pkg, 0);
        long grace = p.getLong("grace_ms", 20 * 60000L);
        long offAt = p.getLong("off_at", 0);
        return pass <= 0 || System.currentTimeMillis() - pass > grace || pass < offAt;
    }

    void show(String pkg, long evAt) {
        if (cover != null) remove("replace");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(Color.BLACK);                            // 검은 배경만(매크로 가림 씬과 같게 — 2026-10-04 사용자 요청)
        box.setClickable(true);                                        // 덮개 아래 앱을 누르지 못하게
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.OPAQUE);
        // 아래 탐색 막대(뒤로·홈·최근 앱)는 덮지 않음 — 덮개가 떠 있어도 나갈 수 있게
        int navBottom = 0, height = WindowManager.LayoutParams.MATCH_PARENT;
        if (Build.VERSION.SDK_INT >= 30) {
            WindowMetrics m = wm.getCurrentWindowMetrics();
            Rect b = m.getBounds();
            navBottom = m.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars()).bottom;
            height = b.height() - navBottom;
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            lp.setFitInsetsTypes(0);
        }
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = height;
        lp.gravity = Gravity.TOP;
        final long t0 = evAt;
        box.getViewTreeObserver().addOnDrawListener(new ViewTreeObserver.OnDrawListener() {
            boolean once;

            @Override
            public void onDraw() {
                if (once) return;
                once = true;
                Log.i(TAG, "drawn +" + (SystemClock.uptimeMillis() - t0) + "ms");
            }
        });
        try {
            wm.addView(box, lp);
        } catch (Exception ex) {
            Log.w(TAG, "cover failed: " + ex);
            return;
        }
        cover = box;
        coverPkg = pkg;
        coverAt = SystemClock.uptimeMillis();
        h.removeCallbacks(timeout);
        h.postDelayed(timeout, COVER_MAX_MS);
        Log.i(TAG, "cover " + pkg + " +" + (coverAt - evAt) + "ms nav=" + navBottom);
    }

    void remove(String why) {
        h.removeCallbacks(timeout);
        if (cover == null) return;
        try {
            wm.removeView(cover);
        } catch (Exception ignored) {
        }
        Log.i(TAG, "uncover " + why + " after " + (SystemClock.uptimeMillis() - coverAt) + "ms");
        if ("scene".equals(why) || "skip".equals(why)) prefs(this).edit().putInt("miss", 0).apply();
        cover = null;
        coverPkg = "";
    }

    /** 매크로가 '물음'(가림 씬을 띄우는 중) — 느린 폰에서 씬이 늦게 떠도 덮개를 먼저 걷지 않게 기다림을 늘림 */
    static void asked(final String pkg) {
        final LockService s = instance;
        if (s == null) return;
        s.h.post(new Runnable() {
            @Override
            public void run() {
                if (s.cover != null && (pkg == null || pkg.isEmpty() || pkg.equals(s.coverPkg))) {
                    s.h.removeCallbacks(s.timeout);
                    s.h.postDelayed(s.timeout, ASKED_MAX_MS);
                }
            }
        });
    }

    /** 매크로가 '이 앱은 이번에 안 물음'(유예 안) — 덮개를 바로 걷음 */
    static void skip(final String pkg) {
        final LockService s = instance;
        if (s == null) return;
        s.h.post(new Runnable() {
            @Override
            public void run() {
                if (s.cover != null && (pkg == null || pkg.isEmpty() || pkg.equals(s.coverPkg))) s.remove("skip");
            }
        });
    }
}
