# MD 도우미 · MD Helper

**한국어** | [English](#english) · 만든 사람 / Made by **chajunoni**

[MacroDroid](https://www.macrodroid.com/) 매크로가 부르면, MacroDroid만으로는 할 수 없는 일을 대신 하는 작은 안드로이드 앱입니다.
화면 없이 뒤에서 답만 하고, **인터넷 권한이 없어서** 기억한 내용을 밖으로 보내지 않습니다.
접근성 서비스('MD 도우미 보안 잠금 덮개')는 앱 잠금 매크로를 쓸 때만 켜면 되고, **앞에 온 앱 이름만 보고 화면 내용은 읽지 않습니다**.
앱 화면은 폰 언어를 따라 한국어·영어로 나오고, 안드로이드 13 이상에서는 설정 → 앱 → MD 도우미 → 언어에서 따로 고를 수 있습니다.

📖 **사용 설명서(사진 포함)**: [한국어](docs/GUIDE.ko.md) · [English](docs/GUIDE.en.md) — 매크로 3개 설치·사용법, 빠른 설정 타일·위젯,
MD 도우미 설치(Play 프로텍트에 막힐 때 포함).

🔑 **PC 없이 adb 권한 주기**: [MD 도우미 — adb 권한](https://github.com/chamr94/md-helper-adb) — 무선 디버깅으로 MacroDroid에 `WRITE_SECURE_SETTINGS`·`DUMP`·`READ_LOGS` 등을 주고, MacroDroid 공식 헬퍼도 설치해 줍니다(별도 앱, 선택).

## 설치

1. [Releases](../../releases/latest)에서 `mdhelper.apk`를 받아 설치합니다(처음이면 '이 출처 허용'을 한 번 켜야 합니다).
   이 도우미를 쓰는 매크로는 도우미가 없으면 설치 창을 한 번 띄우고, [설치]를 누르면 이 주소를 브라우저로 엽니다.
   **"기기를 보호하기 위해 앱이 차단됨"(Play 프로텍트)이 뜨면**: 브라우저로 받은 앱이 알림 접근을 쓰면 Play 프로텍트가 설치를 막습니다.
   Play 스토어 → 프로필 → Play 프로텍트 → 톱니 → 'Play 프로텍트로 앱 검사' 끄기 → [일시중지] → 다시 설치(다음 날 자동으로 다시 켜짐).
   자세한 순서와 사진은 [설명서 6번](docs/GUIDE.ko.md#6--md-도우미-선택--설치와-play-프로텍트-우회).
2. 'MD 도우미' 앱을 열어 필요한 권한을 켭니다. 알림 접근을 켤 때 **"제한된 설정"**이 뜨면 설정 → 애플리케이션 → MD 도우미 →
   오른쪽 위 ⋮ → '제한된 설정 허용'을 누른 뒤 다시 켭니다.
   - **알림 접근** — 알림 바로 열기에 필요합니다. 켜 두면 도우미가 늘 깨어 있어서 기압을 1초마다 기록합니다(배터리는 거의 쓰지 않음).
   - **블루투스(근처 기기)** — 차 알아보기에 필요합니다.

안드로이드 10 이상(최소 SDK 29)에서 동작합니다.

## 매크로 받기 · 업데이트

[Releases](../../releases/latest)에 이 도우미와 함께 쓰는 MacroDroid 매크로도 있습니다. `.macro` 파일을 폰에서 받아 누르면 MacroDroid가
가져오기 화면을 엽니다(오른쪽 아래 저장 버튼 → 처음이 아니면 [덮어쓰기]). MacroDroid 템플릿(아래 '템플릿' 탭)에서도 받을 수 있습니다 —
업데이트가 매크로를 이름으로 덮어쓰니 **매크로 이름은 바꾸지 마세요**. 화면 글자는 폰 언어(한국어·영어)를 따릅니다.

| 파일 | 매크로 | 언어 |
|---|---|---|
| `msg_helper.macro` | 💬 메시지 알리미 — 키워드 알림·자동응답 | 한국어·English |
| `parkmap.macro` | 🅿️ 주차 위치 지도 — 차에서 내리면 주차 위치·층 기억 | 한국어·English |
| `alarm_auto.macro` | ⏰ 알람 자동 설정 — Turbo Alarm 요일별 아침·점심·일정 알람 | 한국어·English |

- **업데이트 알림**: 매크로가 하루 한 번 이 저장소의 최신 릴리스(`versions.json`)를 확인해서, 새 버전이 나오면 알림을 한 번 띄웁니다.
  알림을 누르면 그 매크로 화면에 [업데이트]가 보이고, 누르면 새 파일을 받아 가져오기 화면을 엽니다(MacroDroid에 '모든 파일 접근'이
  없으면 브라우저로 받습니다). 설정과 기록은 매크로마다 전역 변수 하나에 있어서 업데이트해도 그대로 남습니다.
  설정 화면의 [업데이트 확인]으로 바로 확인할 수도 있습니다.
- **도우미 업데이트**: 그 매크로가 쓰는 기능이 새 도우미에 들어 있을 때만 [MD 도우미 업데이트]가 보입니다(쓰지 않는 기능만 바뀌면 안 띄움).
  누르면 매크로가 새 APK를 받아 도우미에게 넘기고, **도우미가 스스로 업데이트**합니다 — 처음 한 번은 '이 출처 허용'을 켜고 [업데이트]를
  눌러야 하고, 그 뒤로는 안드로이드 12 이상에서 확인 없이 됩니다. Google Play 프로텍트가 처음 보는 앱이라며 검사를 권할 수 있습니다.
  도우미 자체에는 여전히 인터넷 권한이 없습니다(받는 일은 MacroDroid가 함). 설치 권한은 자기 자신(같은 이름·같은 서명)을 업데이트할 때만 씁니다.

## 기능과 부르는 법

| 기능 | 매크로에서 부르는 법 | 답 |
|---|---|---|
| 알림 바로 열기 | '인텐트 보내기'(Activity) 동작 `com.mdhelper.OPEN_NOTIFICATION`, 패키지 `com.mdhelper.app`, 클래스 `com.mdhelper.app.OpenActivity`, 문자열 추가 값 `pkg`(앱 패키지)·`title`·`text`·`t`(받은 시각 ms) | 기억해 둔 알림 중 앱·시각(15초 안)·제목·내용이 맞는 것을 누른 것처럼 엽니다(알림이 이미 사라졌어도 3일 안). 못 찾으면 그 앱을 엽니다. |
| 차 알아보기 | 셸 스크립트 `am broadcast --user 0 -n com.mdhelper.app/.CarReceiver` | 결과 줄의 `data="CARS=이름\|이름"` — 등록된 블루투스 기기 중 종류가 핸즈프리(0x0408)·카오디오(0x0420)인 것. 권한이 없으면 `NOPERM`, 블루투스가 없으면 `NOBT` |
| 기압계 | 셸 스크립트 `am broadcast --user 0 -n com.mdhelper.app/.PressureReceiver [--ei w 초]` | `data="P=961.200;N=5;NET=-7.21;COV=240"` — 지금 기압(hPa)과, 최근 w초(기본 240) 동안 가장 높았던 곳보다 얼마나 내려왔는지(`NET`, m — 올라왔으면 양수). 기록이 60초보다 짧으면 `NET`은 빠집니다. 기압계가 없으면 `NOSENSOR`, 값이 안 오면 `NODATA` |
| 알림 거르기·전달 | '인텐트 보내기'(Broadcast) 동작 `com.mdhelper.SET_FILTER`, 클래스 `com.mdhelper.app.FwdReceiver`, 문자열 추가 값 `re`(정규식)·`on`(`true`/`false`). 상태는 셸 `am broadcast --user 0 -n com.mdhelper.app/.FwdReceiver` | 켜 두면 정규식에 맞는 알림만 방송 `com.mdhelper.NOTI`(MacroDroid로, 추가 값 `raw`=`앱 이름␞패키지␞제목␞부제␞티커␞본문`·`key`·`t`·`w`(받은 시각))로 넘깁니다 — MacroDroid는 '인텐트를 받으면' 트리거로 받고 모든 알림마다 돌지 않아도 됩니다. 대화형 알림(메신저)은 알림 속 메시지 목록에서 그 방에 아직 안 넘긴 메시지를 하나씩 넘깁니다(메신저가 알림 하나로 합친 메시지도). `ack`=`true`면 매크로가 `com.mdhelper.ACK`를 보낼 때까지 다음 것을 기다립니다. `gone`=`true`면 알림이 사라질 때 '읽음'(메신저가 지움 — 그 대화를 앱이나 다른 기기에서 읽음, 또는 알림창에서 눌러 엶)만 방송 `com.mdhelper.NOTI_GONE`(추가 값 `key`·`pkg`·`t`·`why`)으로 같은 줄에 넘깁니다(밀어서 지운 것·자동응답 8초 안은 안 넘김). 상태: `data="FWD=1;L=1;V=5;N=넘긴 수;P=들어 올림 센서;U=보는 중;G=읽음 넘김"` |
| 폰을 들면 알리기 | `SET_FILTER`의 추가 값 `pick`=`true`. 시험: 셸 `am broadcast --user 0 -a com.mdhelper.PICKUP_TEST -n com.mdhelper.app/.FwdReceiver --es force true` | 폰의 들어 올림 센서(들어 올림 동작 → 제조사 센서 → 깨우기·흘끗 보기 → 기울기 감지 순서로 찾음, 센서 칩이 알아보고 그때만 폰을 깨움)로 놓인 폰을 들면 방송 `com.mdhelper.PICKUP`을 MacroDroid로 보냅니다(폰을 쓰는 중이면 안 보냄). 메시지 알리미가 확인 안 한 알림이 새로 왔을 때만 진동합니다. 센서가 없으면 `P=0` — 매크로가 화면이 켜질 때로 대신합니다 |
| 알림에 답장 | '인텐트 보내기'(Broadcast) 동작 `com.mdhelper.REPLY`, 클래스 `com.mdhelper.app.ReplyReceiver`, 문자열 추가 값 `key`(알림 키)·`text` | 알림창의 그 알림에서 답장 칸을 찾아 글자를 보냅니다(메신저의 알림 답장과 같음). 답: `OK` / `GONE` / `NOREPLY` / `ERR` |
| 앱 잠금 덮개 | '인텐트 보내기'(Broadcast) 동작 `com.mdhelper.SET_LOCK`(추가 값 `apps`·`grace`)·`LOCK_ASK`·`LOCK_PASS`·`LOCK_SKIP`, 클래스 `com.mdhelper.app.LockReceiver`. 상태는 셸 `am broadcast --user 0 -n com.mdhelper.app/.LockReceiver` | 접근성 '보안 잠금 덮개'를 켜 두면 잠근 앱이 앞에 오는 순간 불투명한 덮개를 그리고, 매크로의 가림 화면이 뜨면 걷습니다(지문을 묻기 전에 앱 내용이 보이지 않게). 상태: `data="LOCK=1;A=접근성;N=잠근 앱 수;M=놓침"` |
| 스스로 업데이트 | '파일 열기' 동작(모든 파일 접근 경로의 새 `mdhelper.apk`), 앱 `com.mdhelper.app`, 화면 `com.mdhelper.app.UpdateActivity` | 받은 파일이 이 앱(같은 패키지)이고 지금보다 낮지 않은 버전이면 설치 관리자로 자기 자신을 업데이트합니다. '이 출처 허용'이 꺼져 있으면 그 설정을 먼저 엽니다. |

- MacroDroid 셸(루트 없음)에서 `am broadcast`를 쓸 때는 **`--user 0`** 을 붙여야 합니다(없으면 권한 오류).
- 셸 결과는 한 줄로만 받으므로 `grep -o 'data=.CARS=[^"]*'`처럼 필요한 부분만 잘라 쓰면 됩니다.
- 기압(P hPa) → 높이(m): `44330 × (1 − (P / 1013.25) ^ (1 / 5.255))`. 1 hPa는 약 8 m입니다.
  날씨에 따라 기압 자체가 하루에도 몇 hPa 바뀌므로 층 짐작은 늘 **몇 분 안의 차이**(`NET`)로 합니다.

## 이 도우미를 쓰는 매크로

- **메시지 알리미** — 목록·새 알림 창에서 누르면 그 대화방·메일을 바로 엽니다(도우미가 없으면 알림창에 남은 알림만, 없으면 앱만 엶).
  도우미가 알림을 먼저 걸러 넘겨서 MacroDroid가 모든 알림마다 돌지 않고, 자동응답도 도우미가 답장 칸으로 보냅니다
  (도우미가 없거나 응답하지 않으면 MacroDroid의 '알림이 오면' 트리거로 예전처럼 동작).
- **주차 위치 지도** — 차 블루투스를 트리거에서 고르지 않아도 알아보고(도우미가 없으면 MacroDroid에 DUMP 권한을 따로 줘야 함 — PC 또는 [adb 권한 앱](https://github.com/chamr94/md-helper-adb)),
  집 주차장에서 기압으로 몇 층인지 짐작해 먼저 골라 둡니다(도우미가 없으면 짐작 없이 고르기만).

## 빌드

Gradle 없이 SDK 도구로 바로 빌드합니다.

```bash
./build.sh                  # → build/mdhelper.apk
./build.sh ../out/x.apk     # 출력 위치 지정
```

필요한 것: JDK 11 이상, Android SDK(build-tools 34 이상, `platforms/android-34`), Python 3.
SDK 위치는 `ANDROID_SDK_ROOT` 또는 `ANDROID_HOME`로 알려 주세요. 서명 키 `debug.keystore`가 없으면 새로 만듭니다
(이미 설치된 앱을 덮어 설치하려면 같은 키로 서명해야 합니다 — 키는 저장소에 올리지 않습니다).

폰에 바로 설치하고 권한을 켜려면:

```bash
adb install -r build/mdhelper.apk
adb shell cmd notification allow_listener com.mdhelper.app/com.mdhelper.app.NotiService
adb shell pm grant com.mdhelper.app android.permission.BLUETOOTH_CONNECT
```

## 구조

| 파일 | 하는 일 |
|---|---|
| `NotiService.java` | 알림 접근 서비스. 알림이 올 때마다 여는 방법(PendingIntent)·앱·제목·내용·시각을 메모리에만 담아 둠(3일, 최대 600개). 켜져 있는 동안 기압 상시 기록도 켬 |
| `OpenActivity.java` | 투명 화면. 담아 둔 알림을 찾아 엶(보이는 화면에서 보내야 다른 앱 화면을 열 수 있어서 화면으로 받음) |
| `CarReceiver.java` | 차 블루투스 목록 |
| `PressureLog.java` | 기압 상시 기록(1초마다, 최근 30분, 메모리에만) |
| `PressureReceiver.java` | 기압계 — 지금 기압과 최근 몇 분 동안 내려온 높이 |
| `Forward.java`, `FwdReceiver.java` | 알림 거르기·전달(정규식은 앱 저장소에, 넘긴 알림은 MacroDroid로) |
| `ReplyReceiver.java` | 알림 답장 칸으로 글자 보내기 |
| `PickupWatch.java` | 폰을 들면 알리기(들어 올림 센서 — 센서 칩이 알아보는 한 번짜리 센서를 먼저) |
| `LockService.java`, `LockReceiver.java` | 앱 잠금 덮개(접근성 — 창 바뀜만 받고 화면 내용은 안 읽음), 잠근 앱·유예는 매크로가 알려 줌 |
| `UpdateActivity.java` | 스스로 업데이트(받은 APK 확인 → 설치 관리자 세션) |
| `MainActivity.java` | 상태 화면(권한·기능별 상태, 권한 켜기 버튼) |
| `res/values`, `res/values-ko` | 화면 글자(영어·한국어) |

---

<a id="english"></a>

## English

A small Android app that does what [MacroDroid](https://www.macrodroid.com/) macros cannot do on their own, when a macro asks.
It has no UI of its own beyond a status screen, and **it has no internet permission**, so nothing it remembers leaves your phone.
Its accessibility service ("MD Helper app lock cover") is only needed with an app lock macro, and **it only sees which app is in front —
it never reads screen content**.
The app follows your phone language (English or Korean). On Android 13+, you can also pick its language under
Settings → Apps → MD Helper → Language.

📖 **Illustrated guide**: [English](docs/GUIDE.en.md) · [한국어](docs/GUIDE.ko.md) — installing and using the three macros,
Quick Settings tiles and widgets, and installing MD Helper (including when Play Protect blocks it).

🔑 **adb permissions without a PC**: [MD Helper — adb](https://github.com/chamr94/md-helper-adb) — grants MacroDroid `WRITE_SECURE_SETTINGS`, `DUMP`, `READ_LOGS` and more over Wireless debugging, and installs the official MacroDroid Helper (separate app, optional).

### Install

1. Download `mdhelper.apk` from [Releases](../../releases/latest) and install it (the first time, allow installing from that source).
   Macros that use the helper show an install prompt once if it is missing; tapping [Install] opens this download in your browser.
   **If you see "App blocked to protect your device" (Play Protect)**: Play Protect blocks browser-downloaded apps that use
   notification access. Play Store → profile → Play Protect → gear → turn off "Scan apps with Play Protect" → [Pause] → install again
   (scanning turns back on the next day). Steps with pictures: [guide section 6](docs/GUIDE.en.md#6--md-helper-optional--installing-it-past-play-protect).
2. Open the **MD Helper** app and turn on (if you see **"Restricted setting"**, go to Settings → Apps → MD Helper → ⋮ →
   "Allow restricted settings", then try again):
   - **Notification access** — needed to open notifications directly. While it is on, the helper stays awake and logs air pressure
     every second (almost no battery).
   - **Bluetooth (nearby devices)** — needed for car recognition.

Works on Android 10+ (min SDK 29).

### Getting the macros · updates

[Releases](../../releases/latest) also has the MacroDroid macros that use this helper. Download a `.macro` file on your phone and tap it;
MacroDroid opens its import screen (save button at the bottom right → [Overwrite] if you already have it). They are also in
MacroDroid's Template Store — updates replace a macro by its name, so **please don't rename the macros**. The screens follow your
phone language (Korean or English).

| File | Macro | Languages |
|---|---|---|
| `msg_helper.macro` | 💬 Message Alerts — keyword alerts and auto reply | Korean, English |
| `parkmap.macro` | 🅿️ Parking Map — remembers where (and on which floor) you parked | Korean, English |
| `alarm_auto.macro` | ⏰ Alarm Auto — Turbo Alarm weekday, lunch and calendar alarms | Korean, English |

- **Update notifications**: each macro checks the latest release here (`versions.json`) once a day and shows one notification when a new
  version is out. Tap it to see [Update] in that macro's window; it downloads the new file and opens MacroDroid's import screen (without
  MacroDroid's "All files access", your browser downloads it instead). Settings and history live in one global variable per macro,
  so they stay after updating. You can also tap [Check for updates] in the settings.
- **Helper updates**: [Update MD Helper] only appears when the new helper has something the macro actually uses. The macro downloads the
  APK and hands it to the helper, which **updates itself** — the first time you need to allow installing from this source and tap
  [Update]; after that it updates without asking on Android 12+. Google Play Protect may suggest scanning an app it hasn't seen before.
  The helper still has no internet permission (MacroDroid does the download), and it uses the install permission only to update itself
  (same package name and signature).

### Features and how to call them

| Feature | How a macro calls it | Result |
|---|---|---|
| Open a notification directly | "Send Intent" (Activity) action `com.mdhelper.OPEN_NOTIFICATION`, package `com.mdhelper.app`, class `com.mdhelper.app.OpenActivity`, string extras `pkg` (app package), `title`, `text`, `t` (received time in ms) | Opens the remembered notification whose app, time (within 15 s), title and text match, as if you tapped it — even if it is already gone (up to 3 days). If none matches, opens the app. |
| Car recognition | Shell script `am broadcast --user 0 -n com.mdhelper.app/.CarReceiver` | `data="CARS=name\|name"` in the result line — paired Bluetooth devices whose class is hands-free (0x0408) or car audio (0x0420). `NOPERM` without permission, `NOBT` without Bluetooth |
| Barometer | Shell script `am broadcast --user 0 -n com.mdhelper.app/.PressureReceiver [--ei w seconds]` | `data="P=961.200;N=5;NET=-7.21;COV=240"` — current pressure (hPa) and how far you went down (`NET`, meters; positive if you went up) from the highest point in the last `w` seconds (default 240). `NET` is omitted if less than 60 s of history is available. `NOSENSOR` without a barometer, `NODATA` if no value arrives |
| Notification filtering & forwarding | "Send Intent" (Broadcast) action `com.mdhelper.SET_FILTER`, class `com.mdhelper.app.FwdReceiver`, string extras `re` (regex) and `on` (`true`/`false`). Status: shell `am broadcast --user 0 -n com.mdhelper.app/.FwdReceiver` | When on, only notifications matching the regex are forwarded as broadcast `com.mdhelper.NOTI` (to MacroDroid, extras `raw` = `app name␞package␞title␞subtext␞ticker␞text`, `key`, `t`, `w` (received time)) — MacroDroid receives them with an "Intent Received" trigger instead of running for every notification. For conversation notifications (messengers), each message in the notification's message list that hasn't been passed on for that chat yet is forwarded one by one (including messages a messenger merged into one notification). With `ack`=`true` it waits for `com.mdhelper.ACK` from the macro before the next one. With `gone`=`true`, when a notification goes away it passes on only "read" removals (the messenger cleared it — the chat was read in the app or on another device — or it was tapped in the shade) as broadcast `com.mdhelper.NOTI_GONE` (extras `key`, `pkg`, `t`, `why`) in the same queue (not swiped-away ones, not within 8 s of an auto reply). Status: `data="FWD=1;L=1;V=5;N=forwarded count;P=pick-up sensor;U=watching;G=read events"` |
| Buzz on pick-up | `SET_FILTER` extra `pick`=`true`. Test: shell `am broadcast --user 0 -a com.mdhelper.PICKUP_TEST -n com.mdhelper.app/.FwdReceiver --es force true` | Using the phone's pick-up sensor (looked up in order: pick-up gesture → vendor sensor → wake/glance gesture → tilt detector; the sensor chip detects it and wakes the phone only then), picking up a resting phone sends broadcast `com.mdhelper.PICKUP` to MacroDroid (not while you're using the phone). Message Alerts buzzes only when a new unread alert has arrived. Without a sensor `P=0` — the macro uses the screen turning on instead |
| Reply to a notification | "Send Intent" (Broadcast) action `com.mdhelper.REPLY`, class `com.mdhelper.app.ReplyReceiver`, string extras `key` (notification key) and `text` | Finds the reply field of that notification in the shade and sends the text (like a messenger's inline reply). Result: `OK` / `GONE` / `NOREPLY` / `ERR` |
| App lock cover | "Send Intent" (Broadcast) actions `com.mdhelper.SET_LOCK` (extras `apps`, `grace`), `LOCK_ASK`, `LOCK_PASS`, `LOCK_SKIP`, class `com.mdhelper.app.LockReceiver`. Status: shell `am broadcast --user 0 -n com.mdhelper.app/.LockReceiver` | With the "app lock cover" accessibility service on, it draws an opaque cover the moment a locked app comes to the front and removes it when the macro's cover screen appears (so the app isn't visible before the fingerprint prompt). Status: `data="LOCK=1;A=accessibility;N=locked apps;M=misses"` |
| Self-update | "Open File" action (a new `mdhelper.apk` at an all-files-access path), app `com.mdhelper.app`, activity `com.mdhelper.app.UpdateActivity` | If the file is this app (same package) and not older, it updates itself through the package installer. If installing from this source is off, it opens that setting first. |

- From MacroDroid's (non-root) shell, `am broadcast` needs **`--user 0`** (otherwise a permission error).
- Shell results arrive as a single line, so cut out what you need, e.g. `grep -o 'data=.CARS=[^"]*'`.
- Pressure (P hPa) → altitude (m): `44330 × (1 − (P / 1013.25) ^ (1 / 5.255))`; 1 hPa is about 8 m.
  Weather moves the absolute pressure by several hPa a day, so floor guessing always uses **differences within a few minutes** (`NET`).

### Macros that use it

- **Message Alerts** — tapping an alert in the list or new-alert window opens that exact chat or email
  (without the helper, only notifications still in the shade can be opened; otherwise just the app).
  The helper also filters notifications first, so MacroDroid no longer runs for every notification, and sends auto replies through the
  notification's reply field (without the helper, or if it stops answering, the macro falls back to MacroDroid's notification trigger).
- **Parking Map** — recognizes your car without picking it in the trigger (without the helper, MacroDroid needs the DUMP permission
  — from a PC or the [adb app](https://github.com/chamr94/md-helper-adb)), and at your Home garage guesses the floor from air pressure and pre-selects it (without the helper, you pick it yourself).

### Build

Builds directly with the SDK tools, no Gradle.

```bash
./build.sh                  # → build/mdhelper.apk
./build.sh ../out/x.apk     # custom output path
```

Requirements: JDK 11+, Android SDK (build-tools 34+, `platforms/android-34`), Python 3.
Point `ANDROID_SDK_ROOT` or `ANDROID_HOME` to your SDK. If `debug.keystore` is missing it is created
(to update an installed app you must sign with the same key — the key is not in this repository).

To install and grant permissions over adb:

```bash
adb install -r build/mdhelper.apk
adb shell cmd notification allow_listener com.mdhelper.app/com.mdhelper.app.NotiService
adb shell pm grant com.mdhelper.app android.permission.BLUETOOTH_CONNECT
```

### Structure

| File | What it does |
|---|---|
| `NotiService.java` | Notification listener. Keeps each notification's open action (PendingIntent), app, title, text and time in memory only (3 days, up to 600). Also starts continuous pressure logging while enabled |
| `OpenActivity.java` | Transparent activity that finds and opens a remembered notification (it must be a visible activity to open another app's screen) |
| `CarReceiver.java` | Car Bluetooth list |
| `PressureLog.java` | Continuous pressure log (every second, last 30 minutes, memory only) |
| `PressureReceiver.java` | Barometer — current pressure and how far you went down in the last few minutes |
| `Forward.java`, `FwdReceiver.java` | Notification filtering and forwarding (regex kept in app storage, matches sent to MacroDroid) |
| `ReplyReceiver.java` | Sends text through a notification's reply field |
| `PickupWatch.java` | Buzz on pick-up (pick-up sensor — one-shot sensor-hub sensors first) |
| `LockService.java`, `LockReceiver.java` | App lock cover (accessibility — receives window changes only, never screen content); locked apps and grace come from the macro |
| `UpdateActivity.java` | Self-update (checks the downloaded APK → package installer session) |
| `MainActivity.java` | Status screen (permissions, feature status, buttons to grant permissions) |
| `res/values`, `res/values-ko` | UI text (English, Korean) |
