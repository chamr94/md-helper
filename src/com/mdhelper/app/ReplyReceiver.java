package com.mdhelper.app;

import android.app.Notification;
import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * 자동응답 보내기 (메시지 알리미 — 도우미가 넘긴 알림에 답장).
 * MacroDroid의 '알림에 답장'은 그 실행을 부른 알림 트리거가 있어야 해서, 도우미가 넘긴 알림(인텐트 받기 트리거)에는 쓸 수 없습니다.
 * 그래서 동작 com.mdhelper.REPLY + 글자 추가 값 key(알림 키)·text(보낼 말)를 받으면, 알림창의 그 알림에서 답장 칸(RemoteInput)이 있는
 * 버튼을 찾아 글자를 넣어 보냅니다(메신저 앱의 알림 답장과 같음). 답: OK / GONE(알림이 없음) / NOREPLY(답장 칸 없음) / ERR.
 */
public class ReplyReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        String key = i.getStringExtra("key");
        String text = i.getStringExtra("text");
        NotiService svc = NotiService.instance;
        if (svc == null || key == null || text == null || text.isEmpty()) {
            result("ERR");
            return;
        }
        try {
            StatusBarNotification[] found = svc.getActiveNotifications(new String[]{key});
            if (found == null || found.length == 0 || found[0] == null) {
                result("GONE");
                return;
            }
            Notification n = found[0].getNotification();
            Notification.Action a = replyAction(n.actions);
            if (a == null) a = replyAction(new Notification.WearableExtender(n).getActions().toArray(new Notification.Action[0]));
            if (a == null) {
                result("NOREPLY");
                return;
            }
            RemoteInput[] ins = a.getRemoteInputs();
            Bundle values = new Bundle();
            for (RemoteInput r : ins) values.putCharSequence(r.getResultKey(), text);
            Intent fill = new Intent();
            RemoteInput.addResultsToIntent(ins, fill, values);
            Forward.replied(key);           // 답장 뒤 메신저가 알림을 지워도 '읽음'으로 넘기지 않게
            a.actionIntent.send(c, 0, fill);
            result("OK");
        } catch (Exception e) {
            Log.w("MDHelper", "reply failed", e);
            result("ERR");
        }
    }

    /** 답장 칸이 있는 버튼 — 답장 뜻(SEMANTIC_ACTION_REPLY)이 붙은 것을 먼저 */
    static Notification.Action replyAction(Notification.Action[] actions) {
        if (actions == null) return null;
        Notification.Action any = null;
        for (Notification.Action a : actions) {
            if (a == null || a.actionIntent == null) continue;
            RemoteInput[] ins = a.getRemoteInputs();
            if (ins == null || ins.length == 0) continue;
            boolean free = false;
            for (RemoteInput r : ins) if (r.getAllowFreeFormInput()) free = true;
            if (!free) continue;
            if (a.getSemanticAction() == Notification.Action.SEMANTIC_ACTION_REPLY) return a;
            if (any == null) any = a;
        }
        return any;
    }

    /** 결과는 결과를 기다리는 방송(am broadcast)일 때만 — MacroDroid '인텐트 보내기'는 일반 방송이라 결과를 쓰면 오류 줄만 남음(2026-10-04) */
    private void result(String r) {
        if (isOrderedBroadcast()) setResultData(r);
    }
}
