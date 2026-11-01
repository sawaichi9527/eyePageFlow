# 平台與 Runtime 矩陣

## 1. 平台基準

| 平台 | 架構 | 主要顯示後端 | 次要／fallback | Runtime |
|---|---|---|---|---|
| Windows 10 22H2／11 | x64 | Windows AWT | — | Eclipse Temurin 21 x64 |
| Ubuntu 24.04 LTS | x86_64 | X11 `XToolkit` | Wayland `WLToolkit`／XWayland | JetBrains Runtime 21 x64 |
| Ubuntu 26.04 LTS | x86_64 | Wayland `WLToolkit` | XWayland | JetBrains Runtime 21 x64 |

- 只支援 64-bit；不提供 x86／32-bit JVM 或 native library。
- Ubuntu 26.04 的 X11 session 不是預設，需自行安裝 Xorg；列為選配驗證。
- 其他 interim 版本（25.10 等）不列入正式支援，best-effort。

## 2. Runtime

### Linux：JBR 21

- 版本：`JBR-21.0.11+10-1163.116`
- Release tag：`jbr-release-21.0.11b1163.116`
- JRE 下載：
  `https://cache-redirector.jetbrains.com/intellij-jbr/jbr-21.0.11-linux-x64-b1163.116.tar.gz`
  SHA-256：`72bf2d1830587a3cd31b718d4af9fc7a88fc6c3740642d842b3b378d4acbe476`
- SDK 下載（jlink／jpackage 用）：
  `https://cache-redirector.jetbrains.com/intellij-jbr/jbrsdk-21.0.11-linux-x64-b1163.116.tar.gz`
  SHA-256：`097bf328b5bf5a236c095b8c8e5204c0eda91e5baa06b37383a7754b10671d8b`
- 授權：GPLv2+CE；打包時列入 `THIRD_PARTY_NOTICES.md` 並提供來源連結。

### Windows：Temurin 21

- 版本：`21.0.12+8`（與 eyeShell 相同）
- 由 Adoptium 提供；Windows 不需要 JBR 的 Wayland 補丁。

## 3. 啟動策略

1. 讀取 `XDG_SESSION_TYPE`、`WAYLAND_DISPLAY`、`DISPLAY`。
2. `XDG_SESSION_TYPE=wayland` → `-Dawt.toolkit.name=WLToolkit`。
3. `XDG_SESSION_TYPE=x11` → 使用預設 `XToolkit`。
4. 可用 `EYEPAGEFLOW_TOOLKIT=wayland|x11` 強制指定。
5. WLToolkit 初始化失敗 → 記錄原因，退回 XWayland，log 與狀態列提示「相容模式」。
6. 啟動 log 記錄 OS、session、toolkit class、縮放比例、JBR 版本。

## 4. 測試矩陣與目前證據

| # | 環境 | 狀態 | 證據 |
|---|---|---|---|
| 1 | Ubuntu 26.04 + Wayland | 通過（M0） | `toolkit=sun.awt.wl.WLToolkit` |
| 2 | Ubuntu 26.04 + Wayland 強制 X11 | 通過（M0） | `toolkit=sun.awt.X11.XToolkit` |
| 3 | Ubuntu 24.04 + X11 | 待驗證 | 需要 24.04 Xorg 環境 |
| 4 | Ubuntu 24.04 + Wayland | 待驗證 | — |
| 5 | Windows 10 x64 | 待驗證 | — |
| 6 | Windows 11 x64 | 待驗證 | — |

M0 執行環境：

- Ubuntu 26.04.1 LTS
- `XDG_SESSION_TYPE=wayland`
- JBR `21.0.11`（JetBrains s.r.o.）
- 原生 Wayland：`sun.awt.wl.WLToolkit`
- XWayland 路徑：`sun.awt.X11.XToolkit`

## 5. 打包

- Linux：JBR SDK → `jlink` runtime image → `jpackage --type app-image` → AppImage（M1）。
- Windows：Temurin 21 → `jpackage --type msi`（M1）。
- M0 已驗證：
  - `jlink` runtime image 約 97 MB。
  - `jpackage --type app-image` 可啟動。
  - app-image 內建 `-Dawt.toolkit.name=WLToolkit`，在 26.04 原生 Wayland 啟動成功。

## 6. 待驗證的 Wayland 行為

- 新注音／拼音 IME 候選字位置。
- 125%／150% 分數縮放與清晰度。
- 系統匣（StatusNotifier／AppIndicator）。
- 老闆鍵縮到托盤後如何還原。
- 檔案拖放（Nautilus → 視窗）。
- 檔案對話框（優先使用 Swing `JFileChooser`）。
- 剪貼簿。
- Client-side decorations 與 FlatLaf 的搭配。

## 7. 已知限制

- Wayland 不允許任意全域鍵盤攔截；老闆鍵以「視窗有焦點時 Esc」實作。
- XWayland 在分數縮放下的清晰度可能不如原生 WLToolkit。
- Ubuntu 24.04 的預設 session 是 Wayland，但本專案要求同時驗證原生 X11；測試環境需明確選擇 Xorg session。
