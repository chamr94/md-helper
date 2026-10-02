package com.mdhelper.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 알림 전달 설정·상태 (메시지 알리미).
 * - 동작 com.mdhelper.SET_FILTER + 글자 추가 값 re(거르기 정규식)·on("true"/"false")·ack("true" — 매크로가 다 처리하면 ACK를 보냄):
 *   저장하고 'OK'로 답함 (MacroDroid '인텐트 보내기' 방송 — 정규식을 셸 명령줄에 넣지 않아 따옴표 걱정이 없음).
 * - 동작 com.mdhelper.ACK: 매크로가 넘긴 알림 하나를 다 처리함 → 줄에 남은 다음 알림을 넘김(Forward.ack).
 * - 그 밖(동작 없음): 상태를 'FWD=1;L=1;V=3;N=전달한 수'로 답함 — MacroDroid 셸이
 *   am broadcast (user 0, 받는 곳 com.mdhelper.app/.FwdReceiver)로 묻고, L=1(알림 접근 연결)이면 자기 '알림이 오면' 트리거를 끔.
 *   V는 이 기능의 판: 2 = 전달·답장, 3 = + ACK로 하나씩 넘기기·when(받은 시각) 넘기기.
 */
public class FwdReceiver extends BroadcastReceiver {
    static final int VERSION = 3;

    @Override
    public void onReceive(Context c, Intent i) {
        if ("com.mdhelper.ACK".equals(i.getAction())) {
            Forward.ack();
            return;
        }
        if ("com.mdhelper.SET_FILTER".equals(i.getAction())) {
            String re = i.getStringExtra("re");
            boolean on = "true".equalsIgnoreCase(i.getStringExtra("on"));
            boolean ack = "true".equalsIgnoreCase(i.getStringExtra("ack"));
            Forward.prefs(c).edit().putString("re", re == null ? "" : re).putBoolean("on", on).putBoolean("ack", ack).apply();
            setResultData("OK");
            return;
        }
        setResultData("FWD=" + (Forward.on(c) ? 1 : 0) + ";L=" + (NotiService.instance != null ? 1 : 0)
                + ";V=" + VERSION + ";N=" + Forward.count(c));
    }
}
