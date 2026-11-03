#!/usr/bin/env bash
# 把 app-image 包成 AppImage（需要 .local/tools/appimagetool-x86_64.AppImage）。
#
# 用法： ./scripts/package-appimage.sh
# 前置： 先執行 ./scripts/package-linux.sh，或本腳本會自動跑一次。
# 產出： build/package/eyePageFlow-<ver>-x86_64.AppImage
set -euo pipefail

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly OUT_DIR="${ROOT_DIR}/build/package"
readonly APP_NAME="eyePageFlow"
readonly APP_VERSION="0.1.0"
readonly APP_IMAGE="${OUT_DIR}/${APP_NAME}/bin/${APP_NAME}"
readonly APPDIR="${OUT_DIR}/${APP_NAME}.AppDir"
readonly TOOL="${ROOT_DIR}/.local/tools/appimagetool-x86_64.AppImage"

if [[ ! -x "${APP_IMAGE}" ]]; then
    printf '==> app-image 不存在，先執行 package-linux.sh\n'
    "${ROOT_DIR}/scripts/package-linux.sh"
fi

if [[ ! -x "${TOOL}" ]]; then
    printf '找不到 appimagetool：%s\n' "${TOOL}" >&2
    printf '下載：\n  mkdir -p .local/tools && curl -fL -o .local/tools/appimagetool-x86_64.AppImage \\\n' >&2
    printf '    https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-x86_64.AppImage\n' >&2
    exit 1
fi

printf '==> 組裝 AppDir\n'
rm -rf "${APPDIR}"
mkdir -p "${APPDIR}/usr"
cp -a "${OUT_DIR}/${APP_NAME}" "${APPDIR}/usr/${APP_NAME}"
cp "${OUT_DIR}/${APP_NAME}.png" "${APPDIR}/${APP_NAME}.png"

cat > "${APPDIR}/${APP_NAME}.desktop" <<DESKTOP
[Desktop Entry]
Type=Application
Name=${APP_NAME}
Comment=以書頁方式閱讀與編輯純文字檔（Big5／UTF-8）
Exec=${APP_NAME} %f
Icon=${APP_NAME}
Terminal=false
Categories=Office;Viewer;
MimeType=text/plain;
StartupNotify=true
DESKTOP

cat > "${APPDIR}/AppRun" <<'APPRUN'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
exec "${HERE}/usr/eyePageFlow/bin/eyePageFlow" "$@"
APPRUN
chmod +x "${APPDIR}/AppRun"

printf '==> 產生 AppImage\n'
ARCH=x86_64 "${TOOL}" --no-appstream "${APPDIR}" \
    "${OUT_DIR}/${APP_NAME}-${APP_VERSION}-x86_64.AppImage"

printf '\n完成： %s\n' "${OUT_DIR}/${APP_NAME}-${APP_VERSION}-x86_64.AppImage"
printf '驗證： %s --help\n' "${OUT_DIR}/${APP_NAME}-${APP_VERSION}-x86_64.AppImage"
