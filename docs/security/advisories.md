# Platform Advisories

本檔記錄 eyePageFlow 對平台／工具鏈漏洞的判定與證據。政策見根目錄 `SECURITY.md`。

- 最後更新：2026-11-23
- 目前發行版：1.0.0
- 釘住的 runtime：Temurin `21.0.13.1+2`（Windows）、JBR `21.0.11+10-1163.125`（Linux）
- 相依：Kotlin stdlib 2.4.10、FlatLaf 3.7.2、RSyntaxTextArea 3.6.3（JUnit 6.1.2 僅測試）

## 判定總表

| # | 元件 | 公告 | 嚴重度 | 影響本專案？ | 處理 |
|---|---|---|---|---|---|
| 1 | Apache Log4j2 | CVE-2021-44228（Log4Shell）等 | 10.0 Critical | **否** | 不適用；不加相依；未來若引入須 ≥ 2.17.1 |
| 2 | OpenSSH sshd | CVE-2026-123158（"SSHole"） | 8.1／9.8 | **否** | 不適用（musl + 無 PAM 的 sshd）；App 不提供 SSH |
| 3 | OpenSSH（Ubuntu） | USN-8928-1（CVE-2026-106552…106587） | High | 僅開發 VM | 建議 VM 執行 apt upgrade；不影響發行物 |
| 4 | OpenSSH client（Windows） | CVE-2025-26465、CVE-2025-26466 | 6.8／7.5 | **否**（僅開發機） | `VerifyHostKeyDNS` 為 `false`；不升級也可 |
| 5 | OpenJDK 21 | 2026-10 CPU（21.0.13）、eMudhra CA（CVE-2026-114820） | 最高 7.0 | **是**（內附 runtime） | 已升到 Temurin 21.0.13.1+2 |
| 6 | JetBrains Runtime 21 | JDK 21 線停在 21.0.11（無 2026-10 CPU） | 同上，未修 | **是**（Linux runtime） | 升到 21.0.11b1163.125（含憑證修正）；CPU 缺口列為已知風險 |

## 1. Log4Shell（CVE-2021-44228 及後續）

**判定：不適用。**

- Log4Shell 是 Apache Log4j2 `log4j-core` 的 JNDI lookup RCE，**不是 JDK 的漏洞**；更新 JDK 不會修它，反之本專案也沒用到它。
- 證據（2026-11-23，`D:\workspace\eyePageFlow`）：

  ```text
  $ ./scripts/gradlew-local.ps1 -q dependencies --configuration runtimeClasspath
  +--- org.jetbrains.kotlin:kotlin-stdlib:2.4.10
  |    \--- org.jetbrains:annotations:13.0
  +--- com.formdev:flatlaf:3.7.2
  \--- com.fifesoft:rsyntaxtextarea:3.6.3
  ```

  repo 內 `grep -i "log4j|slf4j|logback|commons-logging"` 無結果；Gradle、Temurin、JBR 皆不內含 `log4j-core`。
- 規則：若未來引入 logging，`log4j-core` 必須 ≥ 2.17.1，且 `check` 前要重跑上面的相依盤點。

## 2. OpenSSH CVE-2026-123158（"SSHole"，2026-11-05）

**判定：不適用。**

- 條件（全部成立才會中）：OpenSSH < 10.6p2 ＋ Linux musl libc ＋ 沒有 PAM（或 `UsePAM no`）＋ `PasswordAuthentication yes` ＋ 機器上同時存在使用 `*` 密碼欄的帳號與 `$5$`／`$6$` 雜湊的帳號。
- 本專案發行物不含 SSH client／server；Windows 開發機沒有安裝 sshd（無 `%ProgramData%\ssh\sshd_config`、無 `sshd` 服務），Windows 也不使用 musl。
- Ubuntu 24.04／26.04 使用 glibc 與 PAM，不符合條件。
- 已修版本：上游 10.6p2；Alpine 各分支 backport（3.24 `10.3_p1-r2`、3.23 `10.2_p1-r1`、3.22 `10.0_p1-r11`…）。
- 若未來在 Alpine／OpenWrt 容器裡跑 sshd：`PasswordAuthentication no`，並檢查 `Accepted password for` 異常紀錄。

## 3. Ubuntu OpenSSH 公告 USN-8928-1（2026-10-14）

**判定：僅開發／測試 VM 受影響，與發行物無關。**

- 修復版本：24.04 `1:9.6p1-3ubuntu13.20`、26.04 `1:10.2p1-2ubuntu3.8`、22.04 `1:8.9p1-3ubuntu0.18`。
- 處理：**2026-11-23 已檢查測試 VM（Ubuntu 24.04.5）**，`openssh-client`／`openssh-server` 都是
  `1:9.6p1-3ubuntu13.20`，`apt list --upgradable` 沒有 OpenSSH 待更新項目，**無需動作**。
  ```text
  ii  openssh-client 1:9.6p1-3ubuntu13.20 amd64
  ii  openssh-server 1:9.6p1-3ubuntu13.20 amd64
  openssh: no upgrades pending
  ```
- Ubuntu 26.04 開發機待使用者下次回歸時用同一條指令確認。
- 不影響 eyePageFlow 的功能與打包結果。

## 4. Windows OpenSSH client 9.5p2

**判定：不影響本專案。**

- 開發機為 `OpenSSH_for_Windows_9.5p2, LibreSSL 3.8.2`（Windows 內建選用元件，只有 client）。
- CVE-2025-26465（`VerifyHostKeyDNS yes` 時的 MITM）需要非預設設定；實測 `ssh -G github.com` 為 `verifyhostkeydns false`，不可利用。
- CVE-2025-26466（9.5p1／9.5p2 的 pre-auth DoS）在 client 端影響有限；repo 的 remote 是 HTTPS，未使用 SSH。
- 若日後要用 SSH 連內部主機：改裝 Win32-OpenSSH 最新預覽版（2026-10-20 `10.4.0.0p1-Preview`）或等 Windows Update 更新，並維持 `VerifyHostKeyDNS no`。

## 5. OpenJDK 21 的 2026-10 CPU 與 eMudhra CA 事件

**判定：需要更新，已更新。**

- 2026-10-20 的 CPU 讓 21.0.12 以前含已修補弱點（Java 21 server-side 最高 CVSS 7.0）。
- 2026-10-21／10-24 的 `CVE-2026-91403`／`CVE-2026-114820`（JDK-8395690）移除遭外洩的 eMudhra 中繼 CA；未更新的 runtime 仍會信任偽造憑證。
- 更新：Temurin `21.0.12+8` → **`21.0.13.1+2`**（2026-11-20 發佈，含 11-17 CSPU）。
  - Windows zip SHA-256：`bbebea1469996b276683c9299c227b294f85c847039c82633218186a4ad4fbf2`
  - Linux tar.gz SHA-256：`bb8a93f9ae6e7a693b2b721e85dbeff26ae1a77447985a8d7afcdd9c7cf6222e`
- 驗證：Windows 131 tests 全綠、render 11／12 頁、benchmark 418.8／560.7 ms、頁數 7,724／8,375。Linux 待本輪 VM 回歸。

## 6. JetBrains Runtime 21 的 CPU 缺口

**判定：可接受但要記錄。**

- JBR 21 線最新為 `21.0.11b1163.125`（2026-10-27），JDK 基底仍是 **21.0.11**，沒有 2026-10 的 CPU 修正；`1163.125` 只含憑證／建置更新。
- 因此 Linux 原生 Wayland／X11 的發行物仍帶有 21.0.11 的已知弱點（同 #5 的清單，最高 CVSS 7.0，非 Critical）。
- 因應：專案維持 JDK 21（使用者決定）；每次 CPU 後重新檢查 JBR 是否跟進，跟進即升級並重跑回歸。若長期沒有 21.0.13 的 JBR，再評估移到 JBR 25。
- JBR 只發佈 SHA-512；本專案先驗供應商 `.checksum`，再以本地算出的 SHA-256 釘住：

  | 檔案 | SHA-256（釘住） | 供應商 SHA-512 前 16 碼 |
  |---|---|---|
  | `jbr-21.0.11-linux-x64-b1163.125.tar.gz` | `5382a252…b632ff32` | `9080e035…a1ae` |
  | `jbrsdk-21.0.11-linux-x64-b1163.125.tar.gz` | `255e4717…5556b14` | `690fbea6…846a` |

## 情資來源

- NVD／CVE.org（CVSS 與受影響版本）
- OSV.dev、GitHub Advisory Database（相依）
- CISA KEV（已遭利用者優先）
- 供應商公告：Adoptium、JetBrains、OpenSSH release notes、Ubuntu USN、Alpine SecDB

## 更新程序

1. 依 CPU 節奏（1／4／7／10 月）或重大公告檢查上表。
2. 更新 `scripts/bootstrap-*` 的版本與雜湊，先驗雜湊再解壓。
3. 重跑：`test --rerun-tasks`、`check`、`--render`（頁數不得變）、`--benchmark`、`package-*`
   （打包後用內附 runtime 跑 `--benchmark` 驗 Big5）。
4. 把指令、exit code、通過數、log 路徑寫進 `handoff.md`，並更新本檔日期與判定。
