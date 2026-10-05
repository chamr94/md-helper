package com.mdhelper.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * 알림 전달 설정·상태 (메시지 알리미).
 * - 동작 com.mdhelper.SET_FILTER + 글자 추가 값 re(거르기 정규식)·on("true"/"false")·ack("true" — 매크로가 다 처리하면 ACK를 보냄):
 *   저장하고 'OK'로 답함 (MacroDroid '인텐트 보내기' 방송 — 정규식을 셸 명령줄에 넣지 않아 따옴표 걱정이 없음).
 * - 동작 com.mdhelper.ACK: 매크로가 넘긴 알림 하나를 다 처리함 → 줄에 남은 다음 알림을 넘김(Forward.ack). 처음 받으면 하나씩 넘기기를 켬.
 * - 그 밖(동작 없음): 상태를 'FWD=1;L=1;V=3;N=전달한 수'로 답함 — MacroDroid 셸이
 *   am broadcast (user 0, 받는 곳 com.mdhelper.app/.FwdReceiver)로 묻고, L=1(알림 접근 연결)이면 자기 '알림이 오면' 트리거를 끔.
 *   V는 이 기능의 판: 2 = 전달·답장, 3 = + ACK로 하나씩 넘기기·when(받은 시각) 넘기기,
 *   4 = + 대화형 알림의 메시지 목록에서 새 메시지를 하나씩(메신저가 알림 하나로 합친 메시지도 — 2026-10-04),
 *   5 = + 읽은 알림 알리기(SET_FILTER gone=true → com.mdhelper.NOTI_GONE)·폰을 들면 알리기(pick=true → com.mdhelper.PICKUP) — 2026-10-05.
 *   P = 들어 올림 센서(0 없음 1 들어 올림 2 제조사 3 깨우기·흘끗 4 기울기), U = 지금 보는 중, G = 읽음 알리기.
 * - 동작 com.mdhelper.PICKUP_TEST: 폰을 든 것처럼 매크로에 알림(adb 시험 — force=true면 폰을 쓰는 중이어도).
 */
public class FwdReceiver extends BroadcastReceiver {
    static final int VERSION = 5;

    @Override
    public void onReceive(Context c, Intent i) {
        if ("com.mdhelper.ACK".equals(i.getAction())) {
            // ACK를 보내는 매크로면 하나씩 넘기기를 켬 — 매크로를 먼저 업데이트하고 도우미를 나중에 업데이트하면 SET_FILTER의 ack가
            // 예전 도우미에 가서 저장되지 않아, 설정 창을 다시 열 때까지 0.4초 간격으로 넘겼음(2026-10-02)
            SharedPreferences p = Forward.prefs(c);
            if (!p.getBoolean("ack", false)) p.edit().putBoolean("ack", true).apply();
            Forward.ack();
            return;
        }
        if ("com.mdhelper.SET_FILTER".equals(i.getAction())) {
            String re = i.getStringExtra("re");
            boolean on = "true".equalsIgnoreCase(i.getStringExtra("on"));
            boolean ack = "true".equalsIgnoreCase(i.getStringExtra("ack"));
            boolean gone = "true".equalsIgnoreCase(i.getStringExtra("gone"));     // 예전 매크로는 안 보냄 → 꺼짐(읽음을 안 넘김)
            boolean pick = "true".equalsIgnoreCase(i.getStringExtra("pick"));
            Forward.prefs(c).edit().putString("re", re == null ? "" : re).putBoolean("on", on).putBoolean("ack", ack)
                    .putBoolean("gone", gone).putBoolean("pick", pick).commit();
            PickupWatch.sync(c);
            if (isOrderedBroadcast()) setResultData("OK");       // MacroDroid '인텐트 보내기'(일반 방송)면 결과를 안 씀
            return;
        }
        if ("com.mdhelper.PICKUP_TEST".equals(i.getAction())) {
            PickupWatch.test(c, "true".equalsIgnoreCase(i.getStringExtra("force")));
            return;
        }
        setResultData("FWD=" + (Forward.on(c) ? 1 : 0) + ";L=" + (NotiService.instance != null ? 1 : 0)
                + ";V=" + VERSION + ";N=" + Forward.count(c) + ";P=" + PickupWatch.probe(c)
                + ";U=" + (PickupWatch.running() ? 1 : 0) + ";G=" + (Forward.prefs(c).getBoolean("gone", false) ? 1 : 0));
    }
}
