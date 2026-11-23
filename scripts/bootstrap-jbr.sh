#!/usr/bin/env bash
set -euo pipefail

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly LOCAL_DIR="${ROOT_DIR}/.local"
readonly DOWNLOAD_DIR="${LOCAL_DIR}/downloads"
readonly TMP_DIR="${LOCAL_DIR}/tmp"

readonly JBR_VERSION="21.0.11+10-1163.125"
readonly JBR_CLASSIFIER="21.0.11-linux-x64-b1163.125"

readonly JRE_ARCHIVE="jbr-${JBR_CLASSIFIER}.tar.gz"
readonly JRE_URL="https://cache-redirector.jetbrains.com/intellij-jbr/${JRE_ARCHIVE}"
# JetBrains publishes SHA-512 only; this SHA-256 was computed after verifying the
# vendor .checksum on 2026-11-23. See docs/security/advisories.md.
readonly JRE_SHA256="5382a252006bb504d0862eafe3ab2c29e885123b559e98d8f1acfa92b632ff32"
readonly JRE_DIR="${LOCAL_DIR}/jbr-21"

readonly SDK_ARCHIVE="jbrsdk-${JBR_CLASSIFIER}.tar.gz"
readonly SDK_URL="https://cache-redirector.jetbrains.com/intellij-jbr/${SDK_ARCHIVE}"
readonly SDK_SHA256="255e47175863ed7be108da3eae3133e8f1a78eff47f938d447acec11a5556b14"
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

matches_pinned_release() {
    local dir="$1"
    [[ -f "${dir}/release" ]] || return 1
    grep -Fxq 'IMPLEMENTOR="JetBrains s.r.o."' "${dir}/release" || return 1
    grep -Fq "JBR-${JBR_VERSION}" "${dir}/release" || return 1
}

jre_ok=false
if [[ -x "${JRE_DIR}/bin/java" ]] && matches_pinned_release "${JRE_DIR}"; then
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
    [[ -d "${SDK_DIR}/jmods" ]] && matches_pinned_release "${SDK_DIR}"; then
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
