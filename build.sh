#!/bin/bash
# MD 도우미 APK 빌드 — Gradle 없이: javac → d8 → aapt2 → zipalign → apksigner
# usage: ./build.sh [출력 APK 경로]      (기본: build/mdhelper.apk)
# 필요한 것: JDK 11 이상, Android SDK(build-tools 34 이상, platforms/android-34), Python 3.
#   SDK 위치는 ANDROID_SDK_ROOT 또는 ANDROID_HOME (없으면 Windows 기본 위치 %LOCALAPPDATA%\Android\Sdk).
# 서명 키 debug.keystore가 없으면 새로 만듭니다(비밀번호 android). 설치된 앱을 덮어 설치(업데이트)하려면 같은 키로 서명해야 합니다.
set -e
cd "$(dirname "$0")"
SDK="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-${LOCALAPPDATA//\\//}/Android/Sdk}}"
BT=$(ls -d "$SDK"/build-tools/* | sort -V | tail -1)
AJ="$SDK/platforms/android-34/android.jar"
[ -f "$AJ" ] || { echo "android-34 플랫폼이 없어요: $AJ"; exit 1; }
EXE=""; [ -f "$BT/aapt2.exe" ] && EXE=".exe"
BAT=""; [ -f "$BT/d8.bat" ] && BAT=".bat"
OUT="${1:-build/mdhelper.apk}"
VER=$(date +%y%m%d%H)          # 버전 번호: 빌드한 시각 (덮어 설치할 때 낮아지지 않게)

rm -rf build/classes build/gen build/res.zip build/classes.dex build/base.apk build/aligned.apk
mkdir -p build/classes build/gen
# 리소스(글자 영어·한국어, 앱별 언어 목록) → base.apk + R.java
"$BT/aapt2$EXE" compile --dir res -o build/res.zip
"$BT/aapt2$EXE" link -o build/base.apk -I "$AJ" --manifest AndroidManifest.xml --java build/gen build/res.zip \
    --min-sdk-version 29 --target-sdk-version 34 --version-code "$VER" --version-name "1.0.$VER"
javac --release 11 -encoding UTF-8 -Xlint:-options -classpath "$AJ" -d build/classes $(find src build/gen -name '*.java')
"$BT/d8$BAT" --release --min-api 29 --lib "$AJ" --output build $(find build/classes -name '*.class')
python - build/base.apk build/classes.dex <<'PY'
import sys, zipfile
with zipfile.ZipFile(sys.argv[1], 'a', zipfile.ZIP_DEFLATED) as z:
    z.write(sys.argv[2], 'classes.dex')
PY
"$BT/zipalign$EXE" -f 4 build/base.apk build/aligned.apk
[ -f debug.keystore ] || keytool -genkeypair -keystore debug.keystore -storepass android -keypass android -alias md \
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=MD Helper" >/dev/null 2>&1
mkdir -p "$(dirname "$OUT")"
"$BT/apksigner$BAT" sign --ks debug.keystore --ks-pass pass:android --key-pass pass:android --out "$OUT" build/aligned.apk
echo "built: $OUT (version 1.0.$VER)"
