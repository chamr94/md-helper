package com.mdhelper.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 알림 전달 설정·상태 (메시지 알리미).
 * - 동작 com.mdhelper.SET_FILTER + 글자 추가 값 re(거르기 정규식)·on("true"/"false"): 저장하고 'OK'로 답함
 *   (MacroDroid '인텐트 보내기' 방송 — 정규식을 셸 명령줄에 넣지 않아 따옴표 걱정이 없음).
 * - 그 밖(동작 없음): 상태를 'FWD=1;L=1;V=2;N=전달한 수'로 답함 — MacroDroid 셸이
 *   am broadcast (user 0, 받는 곳 com.mdhelper.app/.FwdReceiver)로 묻고, L=1(알림 접근 연결)이면 자기 '알림이 오면' 트리거를 끔.
 *   V는 이 기능의 판: 2 = 전달·답장 지원.
 */
public class FwdReceiver extends BroadcastReceiver {
    static final int VERSION = 2;

    @Override
    public void onReceive(Context c, Intent i) {
        if ("com.mdhelper.SET_FILTER".equals(i.getAction())) {
            String re = i.getStringExtra("re");
            boolean on = "true".equalsIgnoreCase(i.getStringExtra("on"));
            Forward.prefs(c).edit().putString("re", re == null ? "" : re).putBoolean("on", on).apply();
            setResultData("OK");
            return;
        }
        setResultData("FWD=" + (Forward.on(c) ? 1 : 0) + ";L=" + (NotiService.instance != null ? 1 : 0)
                + ";V=" + VERSION + ";N=" + Forward.count(c));
    }
}
