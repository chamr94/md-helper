package com.mdhelper.app;

import android.app.Notification;
import android.app.PendingIntent;
import android.os.Bundle;
import android.os.Parcelable;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * 알림 접근 서비스. 알림이 올 때마다 그 알림을 여는 방법(PendingIntent)을 메모리에 담아 둡니다.
 * 알림이 사라진 뒤에도(다른 기기에서 읽음·밀어서 지움) 며칠 동안 남겨서, 나중에 목록에서 눌러도 그 화면(대화방·메일)이 열리게 합니다.
 * PendingIntent는 파일에 저장할 수 없어서 앱이 다시 시작되면 지금 알림창에 있는 것만 다시 담습니다.
 */
public class NotiService extends NotificationListenerService {

    static final long KEEP_MS = 3L * 24 * 3600 * 1000;   // 사라진 알림도 3일 동안 남김
    static final int MAX = 600;

    static final List<Entry> CACHE = new ArrayList<>();
    static volatile NotiService instance;

    static class Entry {
        String key, pkg, title, text;
        long post;
        PendingIntent intent;
        boolean autoCancel, removed;
    }

    @Override
    public void onListenerConnected() {
        instance = this;
        PressureLog.start(this);        // 알림 접근이 켜져 있는 동안 기압 상시 기록 (주차 층 짐작)
        try {
            StatusBarNotification[] now = getActiveNotifications();
            if (now != null) for (StatusBarNotification s : now) add(s);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onListenerDisconnected() {
        instance = null;
        PressureLog.stop();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification s) {
        add(s);
        Forward.posted(this, s);        // 메시지 알리미: 거르기 정규식에 맞는 알림만 MacroDroid로 넘김 (켜져 있을 때)
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification s) {
        synchronized (CACHE) {
            for (Entry e : CACHE) if (e.key.equals(s.getKey())) e.removed = true;
        }
    }

    static String str(CharSequence c) {
        return c == null ? "" : c.toString();
    }

    static void add(StatusBarNotification s) {
        Notification n = s.getNotification();
        if (n == null || n.contentIntent == null) return;
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;   // 묶음 요약은 대화방이 아님
        Bundle x = n.extras;
        Entry e = new Entry();
        e.key = s.getKey();
        e.pkg = s.getPackageName();
        e.post = s.getPostTime();
        e.intent = n.contentIntent;
        e.autoCancel = (n.flags & Notification.FLAG_AUTO_CANCEL) != 0;
        e.title = x == null ? "" : str(x.getCharSequence(Notification.EXTRA_TITLE));
        StringBuilder t = new StringBuilder();
        if (x != null) {
            t.append(str(x.getCharSequence(Notification.EXTRA_TEXT))).append('\n');
            t.append(str(x.getCharSequence(Notification.EXTRA_BIG_TEXT))).append('\n');
            t.append(str(x.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE))).append('\n');
            CharSequence[] lines = x.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null) for (CharSequence l : lines) t.append(str(l)).append('\n');
            Parcelable[] msgs = x.getParcelableArray(Notification.EXTRA_MESSAGES);
            if (msgs != null) for (Parcelable p : msgs) {
                if (p instanceof Bundle) t.append(str(((Bundle) p).getCharSequence("text"))).append('\n');
            }
        }
        e.text = t.toString();
        long now = System.currentTimeMillis();
        synchronized (CACHE) {
            CACHE.add(e);
            for (int i = CACHE.size() - 1; i >= 0; i--) {
                if (now - CACHE.get(i).post > KEEP_MS) CACHE.remove(i);
            }
            while (CACHE.size() > MAX) CACHE.remove(0);
        }
    }

    static String norm(String s) {
        return s == null ? "" : s.replaceAll("\\s+", " ").trim();
    }

    /**
     * MacroDroid가 넘긴 단서(앱·보낸 사람·내용 앞부분·받은 시각)로 가장 알맞은 알림을 고릅니다.
     * 같은 앱만 보고, 받은 시각이 가까운 것(같은 알림)을 가장 믿고, 보낸 사람·내용이 맞으면 점수를 더합니다.
     * 보낸 사람이나 시각 중 하나는 맞아야 합니다(아무거나 열지 않게).
     */
    static Entry find(String pkg, String title, String text, long t) {
        String nt = norm(title), nx = norm(text);
        Entry best = null;
        int bestScore = 0;
        synchronized (CACHE) {
            for (int i = CACHE.size() - 1; i >= 0; i--) {     // 새것부터 — 점수가 같으면 새것
                Entry e = CACHE.get(i);
                if (!e.pkg.equals(pkg)) continue;
                boolean sameTitle = !nt.isEmpty() && norm(e.title).equals(nt);
                boolean hasText = !nx.isEmpty() && norm(e.text).contains(nx);
                long dt = t > 0 ? Math.abs(e.post - t) : Long.MAX_VALUE;
                boolean nearTime = dt < 15000;
                if (!sameTitle && !nearTime) continue;
                int score = (sameTitle ? 4 : 0) + (hasText ? 3 : 0) + (nearTime ? 6 : dt < 120000 ? 2 : 0) + (e.removed ? 0 : 1);
                if (score > bestScore) {
                    best = e;
                    bestScore = score;
                }
            }
        }
        return best;
    }

    static int size() {
        synchronized (CACHE) {
            return CACHE.size();
        }
    }
}
