package com.mdhelper.app;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.service.notification.StatusBarNotification;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 알림 거르기·전달 — 메시지 알리미를 가볍게 (2026-10-01).
 * MacroDroid가 넘겨 준 거르기 정규식(메시지 알리미 설정이 만듦: 대상 앱이거나 항상 알릴 이름·문구가 든 알림)에 맞는 알림만
 * MacroDroid로 넘깁니다. 그동안 MacroDroid는 모든 알림마다 돌던 '알림이 오면' 트리거를 끄고 이 전달(인텐트 받기 트리거)만 받습니다.
 * 넘기는 글자는 MacroDroid 알림 트리거의 값과 같게: 앱 이름␞패키지␞제목(android.title)␞부제(android.subText)␞티커␞본문(android.text)
 * (MacroDroid 5.67.8 NotificationService·MagicTextReplacer 코드로 확인 — {notification}은 android.text).
 * 대화형 알림(메신저 — 알림 속 메시지 목록 android.messages)은 목록에서 이번에 새로 온 메시지를 하나씩 넘김(2026-10-04, 전달 판 4):
 * 메신저는 거의 동시에 온 메시지를 알림 하나로 합쳐 내용(android.text)엔 마지막 것만 보여서 앞 메시지를 놓쳤음(가상 폰 — 구글 메시지).
 * 알림이 사라지면(전달 판 5, 2026-10-05) 메신저가 지웠거나(그 대화를 앱에서 읽음·다른 기기에서 읽음) 알림창에서 눌러 연 것만 알려 줌
 * (com.mdhelper.NOTI_GONE — 메시지 알리미 '앱에서 직접 읽은 알림도 지우기'). 밀어서 지운 것·다른 앱(도우미·MacroDroid)이 지운 것은 안 알림.
 */
final class Forward {
    static final String ACTION = "com.mdhelper.NOTI";
    static final String GONE = "com.mdhelper.NOTI_GONE";
    /** 알림이 사라진 이유 중 '읽음'으로 보는 것: 알림창에서 눌러 엶(1), 앱이 지움(8 — 그 대화를 읽음), 앱이 모두 지움(9) */
    static final int REASON_CLICK = 1, REASON_APP_CANCEL = 8, REASON_APP_CANCEL_ALL = 9;
    /** 자동응답을 보낸 뒤 이 시간 안에 앱이 지운 알림은 '읽음'이 아님 — 메신저는 답장하면 그 대화를 읽음으로 보고 알림을 지우기도 함(보통 1~3초 안) */
    static final long REPLY_GRACE_MS = 8000;
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
    /** 자동응답을 보낸 알림 키 → 보낸 시각 (ReplyReceiver) */
    private static final Map<String, Long> REPLIED = new LinkedHashMap<String, Long>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Long> e) {
            return size() > 100;
        }
    };

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("forward", Context.MODE_PRIVATE);
    }

    /**
     * 대화방(알림 키)마다 '처음 본 때|넘긴 메시지 시각들' — 다시 시작해도 남게 파일에(3일 지난 방은 지움). 시각 하나(마지막으로 넘긴 것)로만 거르면
     * 안 됨: 메신저가 합친 알림에 마지막 메시지만 넣었다가 다음 알림에 앞 메시지를 넣기도 함(2026-10-04 가상 폰 — 구글 메시지: 두 번째 문자가 먼저,
     * 첫 문자는 0.4초 뒤 알림에 — 시각이 더 이르다고 건너뛰어 놓쳤음)
     */
    static SharedPreferences msgPrefs(Context c) {
        return c.getSharedPreferences("msgseen", Context.MODE_PRIVATE);
    }

    static final long FIRST_MS = 60000;                 // 처음 보는 대화방은 알림 1분 안에 온 메시지만(예전 메시지를 쏟아내지 않게)
    static final long MSG_KEEP_MS = 3L * 24 * 3600 * 1000;
    static final int SEEN_MAX = 40;                     // 방마다 기억하는 넘긴 메시지 수(넘치면 오래된 것부터 빼고 '처음 본 때'를 그만큼 올림)

    /** 알림 속 메시지 목록의 한 줄: 보낸 사람(빈 글자 = 이 폰 사용자가 보낸 것)·글·시각 */
    static final class Msg {
        String sender = "", text = "";
        long time;
    }

    static List<Msg> messages(Bundle x) {
        List<Msg> out = new ArrayList<Msg>();
        if (x == null) return out;
        Parcelable[] arr;
        try {
            arr = x.getParcelableArray(Notification.EXTRA_MESSAGES);
        } catch (Exception e) {
            return out;
        }
        if (arr == null) return out;
        for (Parcelable p : arr) {
            if (!(p instanceof Bundle)) continue;
            Bundle b = (Bundle) p;
            Msg m = new Msg();
            CharSequence t = b.getCharSequence("text");
            m.text = t == null ? "" : t.toString();
            m.time = b.getLong("time", 0);
            CharSequence sd = b.getCharSequence("sender");
            String sender = sd == null ? "" : sd.toString();
            if (sender.isEmpty()) {
                try {
                    Parcelable pp = b.getParcelable("sender_person");
                    if (pp instanceof android.app.Person) {
                        CharSequence nm = ((android.app.Person) pp).getName();
                        sender = nm == null ? "" : nm.toString();
                    }
                } catch (Exception ignored) {
                }
            }
            m.sender = sender;
            out.add(m);
        }
        return out;
    }

    static Intent intent(String raw, StatusBarNotification s, long when) {
        return new Intent(ACTION).setPackage(MD)
                .putExtra("raw", raw).putExtra("key", s.getKey()).putExtra("t", String.valueOf(s.getPostTime()))
                .putExtra("w", String.valueOf(when));
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

    static String raw(Context c, StatusBarNotification s) {
        Notification n = s.getNotification();
        Bundle x = n.extras;
        String pkg = s.getPackageName();
        return appName(c, pkg) + SEP + pkg + SEP + str(x, Notification.EXTRA_TITLE) + SEP + str(x, Notification.EXTRA_SUB_TEXT) + SEP
                + (n.tickerText == null ? "" : n.tickerText.toString()) + SEP + str(x, Notification.EXTRA_TEXT);
    }

    static void replied(String key) {
        if (key == null) return;
        synchronized (REPLIED) {
            REPLIED.put(key, System.currentTimeMillis());
        }
    }

    /**
     * 알림이 사라짐(NotiService.onNotificationRemoved — 이유 코드가 있음). 매크로가 바라면(SET_FILTER gone=true) 거르기 정규식에 맞는 알림 중
     * '읽음'(앱이 지움·알림창에서 눌러 엶)만 넘김 — 알림이 올 때와 같은 줄에 넣어 순서를 지킴(그 방의 앞 메시지를 다 넘긴 뒤 '읽음').
     * 자동응답을 막 보낸 알림은 빼고(답장하면 메신저가 읽음으로 지우기도 함), 묶음 요약·계속 표시되는 알림도 뺌.
     */
    static void removed(Context c, StatusBarNotification s, int reason) {
        if (!on(c) || !prefs(c).getBoolean("gone", false)) return;
        if (reason != REASON_CLICK && reason != REASON_APP_CANCEL && reason != REASON_APP_CANCEL_ALL) return;
        Notification n = s.getNotification();
        if (n == null) return;
        String pkg = s.getPackageName();
        if (MD.equals(pkg) || c.getPackageName().equals(pkg) || "android".equals(pkg)) return;
        if (s.isOngoing() || (n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        long now = System.currentTimeMillis();
        synchronized (REPLIED) {
            Long t = REPLIED.get(s.getKey());
            if (t != null && now - t < REPLY_GRACE_MS) return;
        }
        Pattern p = pattern(c);
        if (p == null || !p.matcher(raw(c, s)).find()) return;
        List<Intent> out = new ArrayList<Intent>();
        out.add(new Intent(GONE).setPackage(MD).putExtra("key", s.getKey()).putExtra("pkg", pkg)
                .putExtra("t", String.valueOf(now)).putExtra("why", String.valueOf(reason)));
        enqueue(c, out);
        android.util.Log.i("MDHelper", "gone: " + pkg + " reason " + reason);
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
        String app = appName(c, pkg), title = str(x, Notification.EXTRA_TITLE), sub = str(x, Notification.EXTRA_SUB_TEXT);
        String text = str(x, Notification.EXTRA_TEXT);
        String raw = raw(c, s);
        Pattern p = pattern(c);
        if (p == null) return;
        List<Intent> out = new ArrayList<Intent>();
        List<Msg> ms = messages(x);
        boolean timed = !ms.isEmpty();
        for (Msg m : ms) if (m.time <= 0) timed = false;
        if (timed) {
            // 대화형 알림: 목록에서 이 대화방에 마지막으로 넘긴 뒤 새로 온 메시지(내가 보낸 것 빼고)를 오래된 것부터 하나씩.
            // 다시 올림·내 답장·읽음 처리로 같은 알림이 고쳐 올라와도 새 메시지가 없으면 안 넘김
            String key = s.getKey();
            SharedPreferences mp = msgPrefs(c);
            String rec = "";
            try {
                rec = mp.getString(key, "");
            } catch (Exception ignored) {
            }
            long floor = s.getPostTime() - FIRST_MS;
            java.util.TreeSet<Long> done = new java.util.TreeSet<Long>();
            int bar = rec.indexOf('|');
            if (bar > 0) {
                try {
                    floor = Long.parseLong(rec.substring(0, bar));
                } catch (Exception ignored) {
                }
                for (String t : rec.substring(bar + 1).split(",")) {
                    try {
                        if (!t.isEmpty()) done.add(Long.parseLong(t));
                    } catch (Exception ignored) {
                    }
                }
            }
            int doneBefore = done.size();
            long floorBefore = floor;
            Msg latest = null;
            for (Msg m : ms) if (!m.sender.isEmpty()) latest = m;
            // 합쳐진 앞 메시지는 그 앱 알림 모양대로: 제목이 보낸 사람이면(카톡·문자 1:1) 그 사람을, 내용 앞에 '보낸 사람: '이 붙는 앱(단톡방)은 그렇게
            boolean titleIsSender = latest != null && title.trim().equals(latest.sender.trim());
            boolean prefixed = latest != null && text.startsWith(latest.sender + ": ");
            for (Msg m : ms) {
                if (m.sender.isEmpty() || m.time <= floor || done.contains(m.time)) continue;   // 내가 보낸 것·처음 보기 전·이미 넘긴 것
                done.add(m.time);
                String r;
                if (m == latest && (text.equals(m.text) || text.equals(m.sender + ": " + m.text))) {
                    r = raw;                               // 마지막 메시지는 알림 그대로(예전과 같은 글자)
                } else {
                    r = app + SEP + pkg + SEP + (titleIsSender ? m.sender : title) + SEP + sub + SEP + SEP
                            + (prefixed ? m.sender + ": " + m.text : m.text);
                }
                // 받은 시각은 메시지마다(알림의 when은 합친 알림이면 첫 메시지 시각이라 다른 메시지와 섞임)
                if (p.matcher(r).find()) out.add(intent(r, s, m.time));
            }
            while (done.size() > SEEN_MAX) floor = Math.max(floor, done.pollFirst());
            if (done.size() != doneBefore || floor != floorBefore || bar <= 0) {
                StringBuilder sb = new StringBuilder().append(floor).append('|');
                for (Long t : done) sb.append(t).append(',');
                SharedPreferences.Editor ed = mp.edit().putString(key, sb.toString());
                if (mp.getAll().size() > 400) {                // 3일 넘게 새 메시지가 없는 방은 지움
                    long old = System.currentTimeMillis() - MSG_KEEP_MS;
                    for (Map.Entry<String, ?> e : mp.getAll().entrySet()) {
                        String v = String.valueOf(e.getValue());
                        long newest = 0;
                        for (String t : v.substring(v.indexOf('|') + 1).split(",")) {
                            try {
                                if (!t.isEmpty()) newest = Math.max(newest, Long.parseLong(t));
                            } catch (Exception ignored) {
                            }
                        }
                        if (newest < old) ed.remove(e.getKey());
                    }
                }
                ed.apply();
            }
        } else {
            if (!p.matcher(raw).find()) return;
            // 내용이 같은 갱신은 다시 넘기지 않음 — 메신저는 답장을 보내거나 읽음 처리할 때 같은 알림을 고쳐 여러 번 다시 올림
            // (2026-10-01 가상 폰: 문자 하나에 8번 넘어가 매크로가 동시에 처리하며 답장을 두 번 보냈음).
            // when(메시지를 받은 시각)까지 같아야 같은 것 — 다시 올려도 when은 그대로라 다시 안 넘기고, 같은 사람이 같은 말을 또 보내면(when이 다름)
            // 넘김(2026-10-02 가상 폰: 구글 메시지가 다시 올린 대화 알림 12개 모두 when이 처음 받은 시각 그대로)
            String sig = raw + SEP + n.when;
            synchronized (LAST) {
                if (sig.equals(LAST.get(s.getKey()))) return;
                LAST.put(s.getKey(), sig);
            }
            out.add(intent(raw, s, n.when));
        }
        if (out.isEmpty()) return;
        enqueue(c, out);
        SharedPreferences pr = prefs(c);
        pr.edit().putInt("n", pr.getInt("n", 0) + out.size()).apply();
        if (out.size() > 1) android.util.Log.i("MDHelper", "forward: " + out.size() + " messages from one notification");
    }

    /** 넘길 줄에 넣음 — 하나씩(ACK·간격) 보냄 */
    private static void enqueue(Context c, List<Intent> out) {
        synchronized (H) {
            appCtx = c.getApplicationContext();
            QUEUE.addAll(out);
            if (!busy) {
                busy = true;
                H.post(NEXT);
            }
        }
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
