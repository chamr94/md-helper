package com.mdhelper.app;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 알림 거르기·전달 — 메시지 알리미를 가볍게 (2026-10-01).
 * MacroDroid가 넘겨 준 거르기 정규식(메시지 알리미 설정이 만듦: 대상 앱이거나 항상 알릴 이름·문구가 든 알림)에 맞는 알림만
 * MacroDroid로 넘깁니다. 그동안 MacroDroid는 모든 알림마다 돌던 '알림이 오면' 트리거를 끄고 이 전달(인텐트 받기 트리거)만 받습니다.
 * 넘기는 글자는 MacroDroid 알림 트리거의 값과 같게: 앱 이름␞패키지␞제목(android.title)␞부제(android.subText)␞티커␞본문(android.text)
 * (MacroDroid 5.67.8 NotificationService·MagicTextReplacer 코드로 확인 — {notification}은 android.text).
 */
final class Forward {
    static final String ACTION = "com.mdhelper.NOTI";
    static final String MD = "com.arlosoft.macrodroid";
    static final String SEP = "␞";
    /** 거의 동시에 온 알림은 0.4초 간격으로 넘김 — MacroDroid가 받은 값을 한 사전에 저장하므로 다음 것이 덮어쓰기 전에 매크로가 옮겨 가게 */
    static final long GAP_MS = 400;
    /**
     * 매크로가 '다 됨'(ACK)을 알려 주는 판이면(SET_FILTER의 ack=true) 한 알림을 다 처리할 때까지 다음 것을 넘기지 않음 — 0.4초 간격으로는
     * 느린 폰에서 앞 알림을 처리하는 동안 다음 알림이 그 사전을 덮어써 앞 알림을 놓쳤음(2026-10-02 흉내). 3초 안에 답이 없으면 그냥 다음 것.
     */
    static final long ACK_WAIT_MS = 3000;

    private static final Handler H = new Handler(Looper.getMainLooper());
    private static final ArrayDeque<Intent> QUEUE = new ArrayDeque<Intent>();
    private static boolean busy;              // 하나를 넘기고 다음 것을 넘길 때(간격·ACK)를 기다리는 중
    private static Context appCtx;
    private static final Runnable NEXT = new Runnable() {
        @Override
        public void run() {
            sendNext();
        }
    };
    private static Pattern pattern;
    private static String patternSrc = "";
    /** 같은 알림(키·올린 시각)은 한 번만 — MacroDroid도 이렇게 거름 */
    private static final Map<String, String> LAST = new LinkedHashMap<String, String>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, String> e) {
            return size() > 200;
        }
    };
    private static final Map<String, Long> SEEN = new LinkedHashMap<String, Long>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> e) {
            return size() > 300;
        }
    };

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("forward", Context.MODE_PRIVATE);
    }

    static boolean on(Context c) {
        SharedPreferences p = prefs(c);
        return p.getBoolean("on", false) && !p.getString("re", "").isEmpty();
    }

    static int count(Context c) {
        return prefs(c).getInt("n", 0);
    }

    static synchronized Pattern pattern(Context c) {
        String re = prefs(c).getString("re", "");
        if (!re.equals(patternSrc)) {
            patternSrc = re;
            try {
                pattern = re.isEmpty() ? null : Pattern.compile(re);
            } catch (Exception e) {
                pattern = null;
            }
        }
        return pattern;
    }

    static String str(Bundle x, String key) {
        if (x == null) return "";
        CharSequence c = x.getCharSequence(key);
        return c == null ? "" : c.toString();
    }

    static String appName(Context c, String pkg) {
        try {
            PackageManager pm = c.getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    static void posted(Context c, StatusBarNotification s) {
        if (!on(c)) return;
        Notification n = s.getNotification();
        if (n == null) return;
        String pkg = s.getPackageName();
        if (MD.equals(pkg) || c.getPackageName().equals(pkg) || "android".equals(pkg)) return;
        if (s.isOngoing()) return;                                     // MacroDroid 트리거의 '계속 표시되는 알림 무시'와 같게
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        synchronized (SEEN) {
            Long t = SEEN.get(s.getKey());
            if (t != null && t == s.getPostTime()) return;
            SEEN.put(s.getKey(), s.getPostTime());
        }
        Bundle x = n.extras;
        String raw = appName(c, pkg) + SEP + pkg + SEP + str(x, Notification.EXTRA_TITLE) + SEP + str(x, Notification.EXTRA_SUB_TEXT)
                + SEP + (n.tickerText == null ? "" : n.tickerText.toString()) + SEP + str(x, Notification.EXTRA_TEXT);
        Pattern p = pattern(c);
        if (p == null || !p.matcher(raw).find()) return;
        // 내용이 같은 갱신은 다시 넘기지 않음 — 메신저는 답장을 보내거나 읽음 처리할 때 같은 알림을 고쳐 여러 번 다시 올림
        // (2026-10-01 가상 폰: 문자 하나에 8번 넘어가 매크로가 동시에 처리하며 답장을 두 번 보냈음).
        // when(메시지를 받은 시각)까지 같아야 같은 것 — 다시 올려도 when은 그대로라 다시 안 넘기고, 같은 사람이 같은 말을 또 보내면(when이 다름)
        // 넘김(2026-10-02 가상 폰: 구글 메시지가 다시 올린 대화 알림 12개 모두 when이 처음 받은 시각 그대로)
        String sig = raw + SEP + n.when;
        synchronized (LAST) {
            if (sig.equals(LAST.get(s.getKey()))) return;
            LAST.put(s.getKey(), sig);
        }
        final Intent i = new Intent(ACTION).setPackage(MD)
                .putExtra("raw", raw).putExtra("key", s.getKey()).putExtra("t", String.valueOf(s.getPostTime()))
                .putExtra("w", String.valueOf(n.when));
        synchronized (H) {
            appCtx = c.getApplicationContext();
            QUEUE.add(i);
            if (!busy) {
                busy = true;
                H.post(NEXT);
            }
        }
        SharedPreferences pr = prefs(c);
        pr.edit().putInt("n", pr.getInt("n", 0) + 1).apply();
    }

    /** 줄의 맨 앞 알림을 넘기고, 다음 것은 ACK(매크로가 그 알림을 다 처리함)나 시간(ACK 판이 아니면 0.4초, 맞으면 3초) 뒤에 */
    private static void sendNext() {
        Intent i;
        Context app;
        synchronized (H) {
            H.removeCallbacks(NEXT);
            i = QUEUE.poll();
            app = appCtx;
            if (i == null || app == null) {
                busy = false;
                return;
            }
        }
        app.sendBroadcast(i);
        H.postDelayed(NEXT, prefs(app).getBoolean("ack", false) ? ACK_WAIT_MS : GAP_MS);
    }

    /** 매크로가 넘긴 알림을 다 처리했다고 알려 줌(com.mdhelper.ACK) → 줄에 남은 것이 있으면 곧바로 다음 것 */
    static void ack() {
        synchronized (H) {
            if (!busy) return;
            H.removeCallbacks(NEXT);
            H.postDelayed(NEXT, 30);
        }
    }
}
