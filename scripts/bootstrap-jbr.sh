#!/usr/bin/env bash
set -euo pipefail

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly LOCAL_DIR="${ROOT_DIR}/.local"
readonly DOWNLOAD_DIR="${LOCAL_DIR}/downloads"
readonly TMP_DIR="${LOCAL_DIR}/tmp"

readonly JBR_VERSION="21.0.11+10-1163.116"
readonly JBR_CLASSIFIER="21.0.11-linux-x64-b1163.116"

readonly JRE_ARCHIVE="jbr-${JBR_CLASSIFIER}.tar.gz"
readonly JRE_URL="https://cache-redirector.jetbrains.com/intellij-jbr/${JRE_ARCHIVE}"
readonly JRE_SHA256="72bf2d1830587a3cd31b718d4af9fc7a88fc6c3740642d842b3b378d4acbe476"
readonly JRE_DIR="${LOCAL_DIR}/jbr-21"

readonly SDK_ARCHIVE="jbrsdk-${JBR_CLASSIFIER}.tar.gz"
readonly SDK_URL="https://cache-redirector.jetbrains.com/intellij-jbr/${SDK_ARCHIVE}"
readonly SDK_SHA256="097bf328b5bf5a236c095b8c8e5204c0eda91e5baa06b37383a7754b10671d8b"
readonly SDK_DIR="${LOCAL_DIR}/jbrsdk-21"

if [[ "$(uname -s)" != "Linux" || "$(uname -m)" != "x86_64" ]]; then
    printf 'Unsupported platform. This bootstrap supports Linux x86_64 only.\n' >&2
    exit 1
fi

for command in curl sha256sum tar; do
    command -v "${command}" >/dev/null || {
        printf '%s is required but was not found.\n' "${command}" >&2
        exit 1
    }
done

mkdir -p "${DOWNLOAD_DIR}" "${TMP_DIR}"

verify_or_download() {
    local archive="$1"
    local url="$2"
    local expected_sha="$3"

    if [[ ! -f "${archive}" ]]; then
        printf 'Downloading %s\n' "$(basename "${archive}")"
        curl --fail --location --retry 3 --continue-at - \
            --output "${archive}.part" "${url}"
        mv "${archive}.part" "${archive}"
    fi

    local actual_sha
    actual_sha="$(sha256sum "${archive}" | cut -d ' ' -f 1)"
    if [[ "${actual_sha}" != "${expected_sha}" ]]; then
        printf 'Checksum mismatch for %s\nExpected: %s\nActual:   %s\n' \
            "${archive}" "${expected_sha}" "${actual_sha}" >&2
        exit 1
    fi
}

extract_fresh() {
    local archive="$1"
    local target="$2"
    local staging
    staging="$(mktemp -d "${TMP_DIR}/jbr.XXXXXX")"

    tar -xzf "${archive}" -C "${staging}"
    local extracted
    extracted="$(find "${staging}" -mindepth 1 -maxdepth 1 -type d -print -quit)"
    if [[ -z "${extracted}" || ! -x "${extracted}/bin/java" ]]; then
        printf 'The archive did not contain a usable runtime: %s\n' "${archive}" >&2
        exit 1
    fi
    rm -rf "${target}"
    mv "${extracted}" "${target}"
    rm -rf "${staging}"
}

jre_ok=false
if [[ -x "${JRE_DIR}/bin/java" ]] && [[ -f "${JRE_DIR}/release" ]] &&
    grep -Fxq 'IMPLEMENTOR="JetBrains s.r.o."' "${JRE_DIR}/release"; then
    jre_ok=true
fi

if [[ "${jre_ok}" == true ]]; then
    printf 'JBR %s already installed at %s\n' "${JBR_VERSION}" "${JRE_DIR}"
else
    verify_or_download "${DOWNLOAD_DIR}/${JRE_ARCHIVE}" "${JRE_URL}" "${JRE_SHA256}"
    extract_fresh "${DOWNLOAD_DIR}/${JRE_ARCHIVE}" "${JRE_DIR}"
    printf 'Installed JBR %s at %s\n' "${JBR_VERSION}" "${JRE_DIR}"
fi

sdk_ok=false
if [[ -x "${SDK_DIR}/bin/jlink" ]] && [[ -x "${SDK_DIR}/bin/jpackage" ]] &&
    [[ -d "${SDK_DIR}/jmods" ]]; then
    sdk_ok=true
fi

if [[ "${sdk_ok}" == true ]]; then
    printf 'JBR SDK %s already installed at %s\n' "${JBR_VERSION}" "${SDK_DIR}"
else
    verify_or_download "${DOWNLOAD_DIR}/${SDK_ARCHIVE}" "${SDK_URL}" "${SDK_SHA256}"
    extract_fresh "${DOWNLOAD_DIR}/${SDK_ARCHIVE}" "${SDK_DIR}"
    printf 'Installed JBR SDK %s at %s\n' "${JBR_VERSION}" "${SDK_DIR}"
fi

"${JRE_DIR}/bin/java" -version
"${SDK_DIR}/bin/jlink" --version
