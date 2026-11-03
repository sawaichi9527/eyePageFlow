#!/usr/bin/env bash
# 建立 Linux 版 app-image（jlink runtime + jpackage）與桌面檔。
#
# 用法： ./scripts/package-linux.sh
# 產出： build/package/eyePageFlow/            app-image（可直接執行）
#        build/package/eyePageFlow.desktop    檔案關聯用的桌面檔
#        build/package/eyePageFlow-<ver>-linux-x64.tar.gz   可分送的壓縮檔
#
# 注意：app-image 不在 launcher 寫死 AWT toolkit，改由程式依 session 自動選擇
#       （見 platform/ToolkitSelection）；需要強制指定時用 EYEPAGEFLOW_TOOLKIT。
set -euo pipefail

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly SDK_DIR="${ROOT_DIR}/.local/jbrsdk-21"
readonly OUT_DIR="${ROOT_DIR}/build/package"
readonly RUNTIME_DIR="${OUT_DIR}/runtime"
readonly APP_NAME="eyePageFlow"
readonly APP_VERSION="0.1.0"

if [[ ! -x "${SDK_DIR}/bin/jlink" ]] || [[ ! -x "${SDK_DIR}/bin/jpackage" ]]; then
    printf 'JBR SDK 不存在：%s\n先執行 ./scripts/bootstrap-jbr.sh\n' "${SDK_DIR}" >&2
    exit 1
fi

printf '==> 建置 installDist\n'
"${ROOT_DIR}/scripts/gradlew-local.sh" --quiet installDist

printf '==> 產生 runtime image（jlink）\n'
rm -rf "${RUNTIME_DIR}"
"${SDK_DIR}/bin/jlink" \
    --module-path "${SDK_DIR}/jmods" \
    --add-modules java.base,java.desktop,java.logging,java.prefs \
    --strip-debug --no-man-pages --no-header-files --compress=zip-6 \
    --output "${RUNTIME_DIR}"

printf '==> 打包 app-image（jpackage）\n'
rm -rf "${OUT_DIR:?}/${APP_NAME}"
"${SDK_DIR}/bin/jpackage" \
    --type app-image \
    --name "${APP_NAME}" \
    --app-version "${APP_VERSION}" \
    --input "${ROOT_DIR}/build/install/eyePageFlow/lib" \
    --main-jar "$(cd "${ROOT_DIR}/build/install/eyePageFlow/lib" && ls eyePageFlow-*.jar | head -1)" \
    --main-class io.github.sawaichi9527.eyepageflow.MainKt \
    --runtime-image "${RUNTIME_DIR}" \
    --dest "${OUT_DIR}"

printf '==> 產生桌面檔\n'
cat > "${OUT_DIR}/${APP_NAME}.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=eyePageFlow
Comment=以書頁方式閱讀與編輯純文字檔（Big5／UTF-8）
Exec=${APP_NAME} %f
Icon=utilities-terminal
Terminal=false
Categories=Office;Viewer;
MimeType=text/plain;
StartupNotify=true
DESKTOP

printf '==> 產生壓縮檔\n'
tar -czf "${OUT_DIR}/${APP_NAME}-${APP_VERSION}-linux-x64.tar.gz" \
    -C "${OUT_DIR}" "${APP_NAME}" "${APP_NAME}.desktop"

printf '\n完成：\n'
printf '  app-image : %s\n' "${OUT_DIR}/${APP_NAME}/bin/${APP_NAME}"
printf '  desktop   : %s\n' "${OUT_DIR}/${APP_NAME}.desktop"
printf '  tarball   : %s\n' "${OUT_DIR}/${APP_NAME}-${APP_VERSION}-linux-x64.tar.gz"
printf '\n驗證：\n'
printf '  %s\n' "${OUT_DIR}/${APP_NAME}/bin/${APP_NAME}"
printf '  EYEPAGEFLOW_TOOLKIT=XToolkit %s   # 需要強制 X11 時\n' "${OUT_DIR}/${APP_NAME}/bin/${APP_NAME}"
printf '  desktop-file-validate %s\n' "${OUT_DIR}/${APP_NAME}.desktop"
