# eyePageFlow

PageByPage 的功能復刻版本：純文字小說的閱讀器兼編輯器，支援分頁、直排／橫排、書頁質感與大檔案快速開啟。

本專案使用自己的品牌與自製素材，不包含原作的程式碼、圖像或說明文件。

## 目前狀態

**M0 技術驗證完成；M1 進行中（檔案層與主視窗殼已完成第一步）。Ubuntu 26.04 原生 Wayland 已驗證；Windows 建置／測試／渲染／benchmark／啟動／單一實例／可攜式打包已驗證（GUI 互動待驗證；Ubuntu 24.04 原生 X11 已延後至 P3）。**

已完成：

- Kotlin/JVM 21 + Swing + FlatLaf 的 Gradle 骨架。
- Big5／GB18030／UTF-8／UTF-16 編碼偵測。
- 字寬表分頁引擎（橫排／直排）。
- Java2D 書頁渲染：桌面、封面、頁面、中縫、陰影、頁碼。
- 顯示設定預覽所需的共用渲染器基礎。
- 10 MiB Big5 分頁 benchmark（< 1 秒）。
- JBR 21 原生 Wayland 啟動驗證。
- `jlink` + `jpackage app-image` 打包驗證。

已完成：開檔／存檔、原編碼／BOM 保留、無法表示字元的 UTF-8／UTF-16LE fallback、命令列開檔、拖放開檔、空白狀態（logo＋雙擊開檔）、顯示設定（表單、字體家族、顏色、內置圖案、配色方案、中縫、即時預覽）、整頁下劃線、翻頁動畫、基本編輯模式（RSyntaxTextArea、dirty、閱讀／編輯切換、尋找／取代、單頁書本外觀、undo/redo 100）、章節列表（可切換規則、目前章節標示）、書籤管理、最近開啟檔案、書庫／檔案列表、老闆鍵（無托盤時最小化）、單一實例（第二次啟動把檔案送到既有視窗）、關窗提示、設定持久化（XDG／AppData）。

尚未完成：檔案關聯註冊、Windows GUI 互動驗證（拖放／IME／縮放）與 Ubuntu 24.04 原生 X11 驗證（已延後至 P3）。

## 平台

| 平台 | 主要後端 | Runtime |
|---|---|---|
| Windows 10 22H2／11 x64 | Windows AWT | Eclipse Temurin 21 x64 |
| Ubuntu 24.04 LTS x86_64 | 原生 X11 `XToolkit` | JetBrains Runtime 21 |
| Ubuntu 26.04 LTS x86_64 | 原生 Wayland `WLToolkit` | JetBrains Runtime 21 |

- 只支援 64-bit。
- Ubuntu 26.04 以原生 Wayland 為主，XWayland 作為 fallback。
- Ubuntu 24.04 以原生 X11 為主；Wayland 也可執行。

## 技術棧

- Kotlin 2.4.10／JVM 21／Gradle 9.5.0
- Swing + FlatLaf 3.7.2
- Java2D 自繪閱讀器
- RSyntaxTextArea 3.6.3（編輯器）
- JUnit 6
- jlink + jpackage；Linux AppImage／tar.gz、Windows 可攜式 zip（不做安裝檔）
- 專案隔離工具鏈：JDK 與 Gradle cache 放在 `.local/`，不修改系統環境

## 快速開始

### Linux

```bash
# 第一次：若 .local/jdk-21 不存在
./scripts/bootstrap-jdk.sh

# Linux 原生 Wayland 需要 JBR 21（JRE + SDK）
./scripts/bootstrap-jbr.sh

# 測試
./scripts/gradlew-local.sh test

# Swing 原型（目前使用 XWayland）
./scripts/gradlew-local.sh run

# 直接開啟檔案
./scripts/gradlew-local.sh run --args="/path/to/novel.txt"

# 直接進編輯模式／顯示設定
./scripts/gradlew-local.sh run --args="--editor"
./scripts/gradlew-local.sh run --args="--settings"

# 產生直排／橫排 PNG
./scripts/gradlew-local.sh run --args="--render build/prototype"

# 10 MiB Big5 分頁 benchmark
./scripts/gradlew-local.sh run --args="--benchmark"
```

### Windows

```powershell
# 第一次：下載 Temurin 21.0.12+8 到 .local\jdk-21（SHA-256 驗證、不動系統 JDK）
.\scripts\bootstrap-jdk.ps1

# 測試
.\scripts\gradlew-local.ps1 test

# Swing 原型
.\scripts\gradlew-local.ps1 run

# 直接開啟檔案／編輯模式／顯示設定
.\scripts\gradlew-local.ps1 run --args="D:\path\to\novel.txt"
.\scripts\gradlew-local.ps1 run --args="--editor"
.\scripts\gradlew-local.ps1 run --args="--settings"

# 產生直排／橫排 PNG
.\scripts\gradlew-local.ps1 run --args="--render build/prototype"

# 10 MiB Big5 分頁 benchmark
.\scripts\gradlew-local.ps1 run --args="--benchmark"
```

- Windows 不需要 `bootstrap-jbr.sh`；JBR 只用於 Linux 的原生 Wayland／X11。
- `gradlew-local.ps1` 會設 `JAVA_HOME=.local\jdk-21`、`GRADLE_USER_HOME=.local\gradle-home`，與 Linux 版同樣隔離於專案內。
- cmd.exe／雙擊情境請改用 `scripts\gradlew-local.cmd`（`.ps1` 在 cmd 會被檔案關聯開成記事本）：
  ```bat
  scripts\gradlew-local.cmd test
  scripts\gradlew-local.cmd run --args="D:\path\to\novel.txt"
  ```

打包（app-image + zip，解開即可執行；不產生安裝檔）：

```powershell
.\scripts\package-windows.ps1
build\package\eyePageFlow\eyePageFlow.exe
```

- 產出 `build\package\eyePageFlow-0.1.0-windows-x64.zip`（約 56 MB），解開後直接執行 `eyePageFlow\eyePageFlow.exe`。
- 腳本會用 `jlink` 產生精簡 runtime，再用 `jpackage --type app-image` 打包，最後用打包進去的 `runtime\bin\java.exe` 跑一次 `--benchmark` 確認 Big5 能解碼。
- 不做 MSI：`jpackage` 的 MSI 產生器需要額外的 WiX Toolset，本專案改以壓縮檔分送。

原生 Wayland：

```bash
./scripts/gradlew-local.sh installDist
JAVA_HOME="$PWD/.local/jbr-21" build/install/eyePageFlow/bin/eyePageFlow
```

toolkit 由程式依 session 自動選擇（Linux + Wayland + JBR 用 `WLToolkit`），
需要強制指定時用 `EYEPAGEFLOW_TOOLKIT=XToolkit`（`-Dawt.toolkit.name` 會被 JBR 的預設值混淆）。

打包（app-image + tar.gz + `.desktop`）：

```bash
./scripts/package-linux.sh
build/package/eyePageFlow/bin/eyePageFlow
```

AppImage（需要 `appimagetool`，放在 `.local/tools/`）：

```bash
./scripts/package-appimage.sh
build/package/eyePageFlow-0.1.0-x86_64.AppImage
```

原型快捷鍵：`V` 直排／橫排、`D` 單頁／雙頁、`T` 主題、`B` 低調模式、方向鍵／PgUp／PgDn 翻頁。

## 驗證數據

Linux（Ubuntu 26.04.1 LTS x86_64、Wayland、JBR 21.0.11）：

- Benchmark（2026-11-03，commit `0fcfbdd`）：10 MiB Big5、5,934,667 字、解碼 130.2 ms
  - 橫排：7,779 頁、300.7 ms
  - 直排：8,323 頁、447.4 ms
- 原生 Wayland：`toolkit=sun.awt.wl.WLToolkit`
- XWayland 路徑：`toolkit=sun.awt.X11.XToolkit`
- 測試：`./scripts/gradlew-local.sh test` 通過（111 tests，0 failures）
- UI 文字：`Noto Sans CJK TC 15pt`（JBR Wayland 原本只算出 13pt）
- 排版：`startup typography: fontSize=26.0 font=Noto Serif CJK TC/26`、20 欄／頁

Windows（Windows 10 x64、Temurin 21.0.12+8、Gradle 9.5.0）：

- Benchmark（2026-11-04）：10 MiB Big5、5,934,667 字、解碼 119.9 ms
  - 橫排：7,724 頁、369.3 ms
  - 直排：8,375 頁、585.2 ms
- 頁數與 Linux 完全一致：頁面字型已內嵌（`page font: Noto Serif CJK TC (embedded=true)`），不再依賴系統字型。
- 啟動：`toolkit=sun.awt.windows.WToolkit`、`ui font: Noto Sans TC 15pt`、`tray: installed`
- 開檔（單一實例轉送）：`encoding=Big5 chars=94 pages=1`
- 測試：`.\scripts\gradlew-local.ps1 test` 通過（115 tests，0 failures）
- 可攜式打包：`build\package\eyePageFlow-0.1.0-windows-x64.zip`（55.7 MB），解開後可執行、Big5 解碼正常。
- 排版參數已對齊原作 PageByPage 的實測值：頁 900×920、內容 812×856、字號 26、
  直排欄距 1.5 em、行距 1.44 em（原作頁 909×919、內容 825×872、字距 26 px、欄距／行距 39 px）。

## 文件

- [功能規格](docs/spec/pagebypage-spec.md)
- [排版規格](docs/spec/typography.md)
- [主題與素材](docs/spec/themes.md)
- [平台與 Runtime](docs/spec/platform-matrix.md)
- [交接紀錄](handoff.md)

## 路線圖

| 階段 | 內容 |
|---|---|
| M0 | 規格、分頁、渲染、JBR Wayland 驗證 |
| M1 | 主視窗殼、開檔／存檔、顯示設定、Java2D 書頁整合 |
| M2 | 文件模型、編碼 round-trip、fallback 對話框 |
| M3 | 字寬表分頁、禁則、offset↔page |
| M4 | 橫排閱讀、顯示設定＋即時預覽 |
| M5 | 直排、RTL 頁序、模式切換位置保存 |
| M6 | 兩套主題、材質、中縫、翻頁動畫 |
| M7 | 編輯模式、undo/redo、搜尋取代 |
| M8 | 書籤、自動恢復、章節列表 |
| M9 | 書庫、老闆鍵、托盤、檔案關聯 |
| M10 | 雙平台驗證、AppImage／可攜式 zip、文件 |

## 授權與致謝

- 功能與操作流程參考馬健的 PageByPage；本專案與原作無隸屬關係。
- 原作程式、圖像與說明文件不包含在本專案中。
- Noto Serif CJK TC 使用 SIL Open Font License。
- JetBrains Runtime 使用 GPLv2+CE；打包時列入第三方授權說明。
- 本專案程式碼授權待定。
