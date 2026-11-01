# eyePageFlow

PageByPage 的功能復刻版本：純文字小說的閱讀器兼編輯器，支援分頁、直排／橫排、書頁質感與大檔案快速開啟。

本專案使用自己的品牌與自製素材，不包含原作的程式碼、圖像或說明文件。

## 目前狀態

**M0 技術驗證完成；M1 進行中（檔案層與主視窗殼已完成第一步）。Ubuntu 26.04 原生 Wayland 已驗證；24.04 X11 與 Windows 待驗證。**

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

尚未完成：檔案關聯註冊、Windows 與 Ubuntu 24.04 X11 驗證。

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
- jlink + jpackage；Linux AppImage、Windows MSI
- 專案隔離工具鏈：JDK 與 Gradle cache 放在 `.local/`，不修改系統環境

## 快速開始

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

原生 Wayland：

```bash
./scripts/gradlew-local.sh installDist
JAVA_HOME="$PWD/.local/jbr-21" \
  JAVA_OPTS="-Dawt.toolkit.name=WLToolkit" \
  build/install/eyePageFlow/bin/eyePageFlow
```

原型快捷鍵：`V` 直排／橫排、`D` 單頁／雙頁、`T` 主題、`B` 低調模式、方向鍵／PgUp／PgDn 翻頁。

## M0 驗證數據

- 環境：Ubuntu 26.04.1 LTS x86_64、Wayland、JBR 21.0.11
- Benchmark：10 MiB Big5、5,934,667 字、解碼 177.2 ms
  - 橫排：7,889 頁、191.4 ms
  - 直排：6,838 頁、342.9 ms
- 原生 Wayland：`toolkit=sun.awt.wl.WLToolkit`
- XWayland 路徑：`toolkit=sun.awt.X11.XToolkit`
- 測試：`./scripts/gradlew-local.sh test` 通過（45 tests，0 failures）

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
| M10 | 雙平台驗證、AppImage／MSI、文件 |

## 授權與致謝

- 功能與操作流程參考馬健的 PageByPage；本專案與原作無隸屬關係。
- 原作程式、圖像與說明文件不包含在本專案中。
- Noto Serif CJK TC 使用 SIL Open Font License。
- JetBrains Runtime 使用 GPLv2+CE；打包時列入第三方授權說明。
- 本專案程式碼授權待定。
