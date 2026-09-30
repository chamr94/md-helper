# MD 도우미 (MacroDroid Helper)

[MacroDroid](https://www.macrodroid.com/) 매크로가 부르면, MacroDroid만으로는 할 수 없는 일을 대신 하는 작은 안드로이드 앱입니다.
화면 없이 뒤에서 답만 하고, **인터넷 권한이 없어서** 기억한 내용을 밖으로 보내지 않습니다.

## 설치

1. [Releases](../../releases/latest)에서 `mdhelper.apk`를 받아 설치합니다(처음이면 '이 출처 허용'을 한 번 켜야 합니다).
   이 도우미를 쓰는 매크로는 도우미가 없으면 설치 창을 한 번 띄우고, [설치]를 누르면 이 주소를 브라우저로 엽니다.
2. 'MD 도우미' 앱을 열어 필요한 권한을 켭니다.
   - **알림 접근** — 알림 바로 열기에 필요합니다. 켜 두면 도우미가 늘 깨어 있어서 화면이 꺼져 있어도 기압을 잴 수 있습니다.
   - **블루투스(근처 기기)** — 차 알아보기에 필요합니다.

안드로이드 10 이상(최소 SDK 29)에서 동작합니다.

## 기능과 부르는 법

| 기능 | 매크로에서 부르는 법 | 답 |
|---|---|---|
| 알림 바로 열기 | '인텐트 보내기'(Activity) 동작 `com.mdhelper.OPEN_NOTIFICATION`, 패키지 `com.mdhelper.app`, 클래스 `com.mdhelper.app.OpenActivity`, 문자열 추가 값 `pkg`(앱 패키지)·`title`·`text`·`t`(받은 시각 ms) | 기억해 둔 알림 중 앱·시각(15초 안)·제목·내용이 맞는 것을 누른 것처럼 엽니다(알림이 이미 사라졌어도 3일 안). 못 찾으면 그 앱을 엽니다. |
| 차 알아보기 | 셸 스크립트 `am broadcast --user 0 -n com.mdhelper.app/.CarReceiver` | 결과 줄의 `data="CARS=이름\|이름"` — 등록된 블루투스 기기 중 종류가 핸즈프리(0x0408)·카오디오(0x0420)인 것. 권한이 없으면 `NOPERM`, 블루투스가 없으면 `NOBT` |
| 기압계 | 셸 스크립트 `am broadcast --user 0 -n com.mdhelper.app/.PressureReceiver` | `data="P=961.200;N=34"` — 1.5초 동안 여러 번 읽은 기압(hPa)의 가운데 값과 개수. 기압계가 없으면 `NOSENSOR`, 값이 안 오면 `NODATA` |

- MacroDroid 셸(루트 없음)에서 `am broadcast`를 쓸 때는 **`--user 0`** 을 붙여야 합니다(없으면 권한 오류).
- 셸 결과는 한 줄로만 받으므로 `grep -o 'data=.CARS=[^"]*'`처럼 필요한 부분만 잘라 쓰면 됩니다.
- 기압(P hPa) → 높이(m): `44330 × (1 − (P / 1013.25) ^ (1 / 5.255))`. 1 hPa는 약 8 m입니다.
  층 짐작은 같은 곳에서 잰 두 값의 **차이**로 합니다(날씨에 따라 기압 자체는 크게 바뀜).

## 이 도우미를 쓰는 매크로

- **메시지 알리미** — 목록·새 알림 창에서 누르면 그 대화방·메일을 바로 엽니다(도우미가 없으면 알림창에 남은 알림만, 없으면 앱만 엶).
- **주차 위치 지도** — 차 블루투스를 트리거에서 고르지 않아도 알아보고(도우미가 없으면 MacroDroid에 DUMP 권한을 PC에서 따로 줘야 함),
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
| `NotiService.java` | 알림 접근 서비스. 알림이 올 때마다 여는 방법(PendingIntent)·앱·제목·내용·시각을 메모리에만 담아 둠(3일, 최대 600개) |
| `OpenActivity.java` | 투명 화면. 담아 둔 알림을 찾아 엶(보이는 화면에서 보내야 다른 앱 화면을 열 수 있어서 화면으로 받음) |
| `CarReceiver.java` | 차 블루투스 목록 |
| `PressureReceiver.java` | 기압계 |
| `MainActivity.java` | 상태 화면(권한·기능별 상태, 권한 켜기 버튼) |
