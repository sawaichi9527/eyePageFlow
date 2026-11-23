# Security Policy

eyePageFlow 是離線桌面純文字閱讀／編輯器，但發行物**內附 Java runtime**，因此
runtime 的補丁等級與工具鏈的供應鏈完整性都視為專案安全範圍。

## 支援版本

| 版本 | 平台 | Runtime | 狀態 |
|---|---|---|---|
| 0.1.x | Windows 10/11 x64 | Eclipse Temurin 21 x64 | 支援 |
| 0.1.x | Ubuntu 24.04 LTS x64（原生 X11） | JetBrains Runtime 21 x64 | 支援 |
| 0.1.x | Ubuntu 26.04 LTS x64（原生 Wayland） | JetBrains Runtime 21 x64 | 支援 |

只支援 64-bit。舊版 0.1.x 不提供回溯修補；安全性修正以最新 0.1.x 重新打包提供。

## 通報方式

- 優先使用 GitHub 的 **private vulnerability reporting**（Repository → Security →
  Advisories → Report a vulnerability）。
- 不要開公開 issue，也不要在 issue／PR 貼出可直接利用的細節。
- 請提供：受影響版本（`--version` 或發行檔名）、平台、重現步驟、影響評估。
- 回應時限：5 個工作天內確認收到，10 個工作天內完成初步分級。
- 本專案不會要求你提供密碼、金鑰或任何憑證。

## 範圍

**在範圍內**

- `src/` 的程式碼（檔案解析、編碼處理、分頁、渲染、設定儲存）。
- 打包腳本（`scripts/bootstrap-*`、`scripts/package-*`）與其釘住的版本／雜湊。
- 發行物內附的 runtime（Temurin／JBR）與 `jlink` 模組清單。

**不在範圍內**

- 第三方程式庫本身的漏洞（FlatLaf、RSyntaxTextArea、Kotlin stdlib、JUnit）→ 請通報上游；
  本專案以升級版本因應。
- 使用者自行改裝的 runtime、自行安裝的系統字型。
- 社交工程、實體存取、已遭入侵的開發機。

## 修補政策

| 嚴重度 | 目標 |
|---|---|
| 已遭實際利用（CISA KEV）或 Critical | 7 天內評估並更新 runtime／相依 |
| High | 30 天內 |
| Medium／Low | 併入下一次季度更新（OpenJDK CPU：1／4／7／10 月） |

- Runtime 以 OpenJDK CPU／CSPU 節奏更新；每次更新都重跑完整回歸（測試、渲染頁數、
  benchmark、打包後 smoke test），並把證據寫進 `handoff.md`。
- 判定結果（含「不適用」的理由與證據）記錄在 `docs/security/advisories.md`。
- 每次更新都會在 release note 加一個 Security 段落，說明升了什麼、為什麼、驗了什麼。

## 供應鏈

- 所有工具鏈版本與 SHA-256／SHA-512 釘在 `scripts/bootstrap-*.{sh,ps1}`；下載後先驗雜湊再解壓。
- JBR 只發佈 SHA-512，腳本以供應商的 `.checksum` 驗 SHA-512，再以本地計算的 SHA-256 釘住；
  來源與日期記在 `docs/security/advisories.md`。
- 專案不含密碼、token、私鑰或個人憑證；設定檔只存路徑與顯示偏好。

## 揭露

修正發佈後，於 release note 與 `docs/security/advisories.md` 公開 CVE 編號、影響範圍與
修正版本；未修正前不公開可操作細節。
