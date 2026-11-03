# Handoff

## Current status

**M0 技術驗證完成（Go）；M1 進行中，已完成檔案層與主視窗殼的第一步。**

已完成規格文件、Gradle 骨架、編碼偵測、字寬表分頁、Java2D 書頁渲染、Swing 原型、10 MiB benchmark、JBR 21 原生 Wayland／XToolkit 啟動，以及文件讀寫與基本開檔／存檔。

顯示設定、空白狀態、雙擊開檔、翻頁動畫與整頁下劃線已完成；編輯模式已接上 RSyntaxTextArea，可切換閱讀／編輯、偵測 dirty、存檔同步、尋找／取代、單頁書本外觀與 undo/redo 100 上限，並有關窗未儲存提示。章節列表支援可切換規則與目前章節標示；顯示設定、章節規則、書籤、最近開啟檔案與書庫會存到 XDG／AppData。檔案列表、書庫管理、老闆鍵（最小化）與單一實例已完成。尚未完成檔案關聯註冊、Windows GUI 互動驗證與 Ubuntu 24.04 X11 驗證。

Windows 開發環境已建立：`scripts/bootstrap-jdk.ps1`（Temurin 21.0.12+8 到 `.local/jdk-21`）與 `scripts/gradlew-local.ps1`（`JAVA_HOME`／`GRADLE_USER_HOME` 隔離於 `.local/`），建置、45 個測試、渲染、benchmark、GUI 啟動、托盤與單一實例轉送皆已通過。

## Completed

- `docs/spec/pagebypage-spec.md`：功能矩陣、畫面、快捷鍵、編碼、資料存放、與原作差異。
- `docs/spec/typography.md`：頁面幾何、頁序、頁碼、狀態列、直排／橫排規則。
- `docs/spec/themes.md`：仿真紙書／現代簡約、程序化材質、設定優先順序。
- `docs/spec/platform-matrix.md`：Windows／Ubuntu 24.04／26.04、JBR／Temurin、Wayland／X11。
- Gradle 骨架：Kotlin 2.4.10、JVM 21、FlatLaf 3.7.2、JUnit 6。
- 專案隔離工具鏈：`.local/jdk-21`（Temurin 21.0.12+8）、`.local/jbr-21`、`.local/jbrsdk-21`。
- `scripts/bootstrap-jbr.sh`：鎖定 JBR 21.0.11 JRE／SDK、SHA-256 驗證、可重複執行。
- `scripts/bootstrap-jdk.ps1`：Windows 版 Temurin 21.0.12+8 bootstrap（SHA-256 驗證、解壓到 `.local/jdk-21`、版本不符即中止、不動系統 JDK）。
- `scripts/gradlew-local.ps1`：Windows 版 Gradle Wrapper（`JAVA_HOME=.local\jdk-21`、`GRADLE_USER_HOME=.local\gradle-home`）。
- `scripts/gradlew-local.cmd`、`scripts/bootstrap-jdk.cmd`：給 cmd.exe／雙擊情境的包裝；`.ps1` 在 cmd 會被檔案關聯開成記事本。
- `.gitattributes`：文字檔 LF、`*.bat`／`*.cmd` CRLF、二進位標記，避免 Windows 的 `core.autocrlf=true` 讓整批檔案顯示為已修改。
- 排版對齊原作 PageByPage：頁 900×920、內容區 812×856、字號 26、行距 1.44、直排欄距 1.5；依原作截圖實測（頁 909×919、內容 825×872、字距 26 px、欄距／行距 39 px）。
- 直排半角字（拉丁字母、數字）改為依字寬前進，旋倒字串不再被撐成 1em／字。
- `Typeface` 增加 CJK 優先的 fallback 鏈（Noto Serif CJK TC → Noto Serif TC → 新細明體 → …），避免落到拉丁字型。
- `SettingsStore` 增加 `display.typographyVersion`，舊設定檔的字號／行距自動升級到新基準，其餘設定保留。
- 修正主畫面字級不生效：`BookCanvas` 內的 `font` 被解析成 `JPanel.font`（元件預設字型，約 9px），
  導致書頁用預設字型繪製、卻按設定字級排版（分頁正確、看起來卻「字很小、間距很大」）。
  外層欄位改名 `pageFont`，並在啟動時記錄 `startup typography` 供診斷。
- 修正直排標點壓到欄線：字型沒有直排形式時（新細明體缺 U+FE10–FE19），
  原本用「+0.58 em 右移、−0.62 em 上移」的近似法，會把 `。、，` 推出字格並壓到下劃線。
  改為直接沿用原字形（墨水本來就在字格內置中），與原版一致。
- 顯示設定新增「直排標點」（置中／右上）：右上模式用字形視覺邊界換算位移，把標點墨跡放到字格右上角。
- 顯示設定新增「下劃線粗細」（淡 0.5／普通 1.0／粗黑 1.5），搭配既有的「下劃線顏色」。
- 修正下劃線顏色污染文字：`drawPageUnderlines` 改掉畫筆顏色後沒有還原，字就沿用下劃線色。
- 修正直排 `UTF-8` 的連字號壓在 `8` 上：半角標點改為與拉丁字串一起旋倒，並把窄字在字格內垂直置中。
- `RenderRegressionTest`：4 個書頁渲染回歸測試（字色不被下劃線污染、字墨符合字級、窄字不溢出、標點右上位置），刻意用像素性質而非跨平台 golden image。
- 禁則：`NO_LINE_START`／`NO_LINE_END` 查表，橫排與直排都在換行前用前瞻避開孤字；連續關閉標點整段帶到下一個行／欄。`PaginationTest` 驗證每一行／欄的首尾字元。
- 左右對齊（橫排 justify）：因寬度換行的行把剩餘寬度平均分配到字距，段末行與剩餘 > 0.5em 的行不拉齊；`display.justify` 可關閉，顯示設定有「左右對齊（橫排）」核取方塊。
- `core/TextEncoding.kt`：BOM、UTF-8、UTF-16 LE/BE、Big5、GB18030 偵測與換行偵測。
- `core/TextDocument.kt`、`core/DocumentIO.kt`：文件模型、開檔、存檔、原編碼／BOM 保留、無法表示字元的 fallback 結果。
- `core/Pagination.kt`：字寬表分頁、橫排／直排、段落縮排、直排形式標點、拉丁字母旋轉。
- `render/BookRenderer.kt`：桌面、封面、頁面、中縫、陰影、頁碼、低調模式。
- `render/Theme.kt`、`render/TexturePainter.kt`：兩套主題與程序化材質。
- `ui/ReaderWindow`（`ReaderWindow.kt`）：Swing 主視窗、選單、工具列、翻頁、直排／橫排、主題、低調模式、狀態列、開檔／存檔、拖放、命令列開檔、編碼 fallback 對話框。
- `core/DisplaySettings.kt`、`ui/DisplaySettingsDialog.kt`：顯示設定模型、表單、即時預覽、確定／取消、字體家族、五組顏色。
- `render/EmptyStateRenderer.kt`：空白狀態 logo、tagline、雙擊開檔提示。
- `render/PatternCatalog.kt`：桌面／封面／頁面內置圖案與材質對應。
- `core/ColorSchemeCatalog.kt`：預設／護眼／夜間／羊皮紙／高對比配色。
- `core/GutterColorCatalog.kt`：深棕／淺灰／米色／抹茶／深黑中縫預設色。
- 翻頁動畫：淡入、平移、捲頁近似；`--demo` 可自動翻頁驗證。
- 下劃線頁面線：`PageUnderlines` 計算整頁行槽／欄槽，`BookRenderer` 不再逐字畫底線。
- `ui/EditorPane.kt`：RSyntaxTextArea 3.6.3、純文字模式、dirty callback、主題套用、Ctrl+E 切換。
- 閱讀／編輯共用 `TextDocument`；離開編輯器以 caret offset 回到對應頁。
- 閱讀模式單鍵快捷鍵改綁在畫布，避免攔截編輯器打字。
- `ui/FindReplaceDialog.kt`：尋找／取代／全部取代、大小寫／全字／正規表達式／向上。
- `core/ChapterParser.kt`：預設章節標題規則、offset／行號、4096 上限。
- `ui/ChapterListDialog.kt`：章節列表、目前章節標示、雙擊／跳至、閱讀模式跳到對應頁、編輯模式移到 offset。
- `core/ChapterRule.kt`、`ui/ChapterRuleDialog.kt`：預設／簡中／純數字／自訂正規表達式、即時預覽章節數。
- `core/AppPaths.kt`、`core/SettingsStore.kt`：XDG／AppData 設定路徑、顯示設定與章節規則的 properties 持久化。
- `core/BookmarkStore.kt`、`ui/BookmarkListDialog.kt`：書籤新增／跳至／刪除與持久化。
- `core/RecentFilesStore.kt`：最近開啟檔案（最多 10 筆）與「最近開啟」選單。
- `core/LibraryStore.kt`、`ui/LibraryDialog.kt`、`ui/FileListDialog.kt`：書庫、預設書架、檔案列表、設定持久化。
- `platform/TrayService.kt`：系統匣顯示／結束；`Esc` 老闆鍵最小化。
- 編輯模式：單頁桌面／封面／頁面外觀、undo/redo 上限 100（透過 RSyntaxTextArea 的 UndoManager）。
- `platform/SingleInstance.kt`：檔案鎖＋loopback socket；第二次啟動把檔案送到既有視窗。
- 關窗未儲存提示。
- `EditorPaneTest`：程式設定文字不觸發 dirty；插入文字會觸發 dirty；find／replace／replaceAll 正常。
- 測試：57 個測試，0 失敗（Windows；含 4 個書頁渲染回歸測試）。
- Benchmark：10 MiB Big5，橫排 191.4 ms、直排 342.9 ms。
- JBR 21 原生 Wayland：`sun.awt.wl.WLToolkit`。
- JBR 21 XToolkit：`sun.awt.X11.XToolkit`。
- `jlink` runtime image（97 MB）與 `jpackage --type app-image` 驗證。

## In progress

- Ubuntu 24.04 原生 X11 驗證尚未執行。
- Windows GUI 互動驗證（拖放、IME、125%／150% 縮放、檔案對話框、編輯存檔）尚未執行；建置、測試、渲染、benchmark、啟動、托盤與單一實例已完成。
- 直排標點與左右對齊仍是原型等級（禁則已完成）。
- 尚未做 golden image 基準與字型子集。
- 目前環境的 GNOME／Wayland 不支援 `SystemTray`，老闆鍵會退回最小化；托盤在有支援的桌面才會安裝。
- `AUTO`／`EQUAL_HEIGHT` 目前都當成雙頁；尚未依視窗比例或固定頁寬做真正區分。
- 翻頁動畫 CURL 仍是「平移＋陰影」近似。

## Next actions

### P0 收尾（約半天）

1. Windows GUI 互動驗證：拖放、IME、125%／150% 縮放、檔案對話框、剪貼簿、編輯存檔、關窗提示。
2. 文件同步：把舊數據（45 tests、191 ms）更新為現況，README 驗證數據比照。
3. 定案顯示預設值（字級 26、直排標點、下劃線粗細與顏色）。

### P1 排版正確性（M3，約 1–2 天）

4. ~~禁則~~（已完成：關閉標點不落行首／欄首、開括號不落行尾／欄尾，連續標點整段帶走）。
5. ~~左右對齊~~（已完成：橫排 justify，段末行與過鬆的行不拉齊，可在顯示設定關閉）。
6. offset↔page 對應測試（編輯 caret ↔ 閱讀頁碼）。
7. Surrogate pair（emoji／罕用字）不被拆成兩半。
8. 書頁渲染回歸測試（已完成：`RenderRegressionTest`，4 個像素性質測試）。

### P2 打包與平台（M10，約 1–2 天）

9. Windows MSI（jpackage）＋檔案關聯註冊。
10. Linux AppImage；修掉 app-image 寫死 `-Dawt.toolkit.name=WLToolkit`。
11. Ubuntu 24.04 原生 X11 回歸。

### P3 體驗

12. `AUTO`／`EQUAL_HEIGHT` 依視窗比例或固定頁寬真正區分。
13. CURL 翻頁動畫改為實際捲頁。
14. 內嵌 Noto Serif CJK TC（OFL），讓 Windows／Linux 字型一致。
15. GNOME／Wayland 的托盤替代方案。

## Validation evidence

執行環境：

Linux（M0 基準）：

- OS：Ubuntu 26.04.1 LTS x86_64
- Session：Wayland（`XDG_SESSION_TYPE=wayland`）
- 原生 Wayland runtime：JBR `21.0.11`（JetBrains s.r.o.）
- XWayland runtime：JBR `21.0.11` / `sun.awt.X11.XToolkit`
- Build runtime：Temurin `21.0.12+8`
- DUT／firmware：N/A（桌面應用程式）
- UART／Console Log：N/A
- PCAP／packet evidence：N/A

Windows（2026-11-02）：

- OS：Microsoft Windows 10 IoT 企業版 LTSC 21H2 x64（`10.0.19044`）
- Runtime：Temurin `21.0.12+8`（Eclipse Adoptium）
- Build runtime：Gradle `9.5.0`、Kotlin `2.4.10`、`JAVA_HOME=.local\jdk-21`、`GRADLE_USER_HOME=.local\gradle-home`
- Toolkit：`sun.awt.windows.WToolkit`
- 專案路徑：`D:\workspace\eyePageFlow`（`core.autocrlf=false`，repo 無 `.gitattributes`）

### Linux session close（2026-11-01 21:34 CST）

| 指令 | Exit | Duration | 結果 | 證據 |
|---|---|---|---|---|
| `./scripts/gradlew-local.sh test --rerun-tasks` | 0 | 45s | 45 tests, 0 failures, 0 skipped | `/tmp/opencode/session-close-test-rerun.log`、`build/test-results/test/` |
| `./scripts/gradlew-local.sh check` | 0 | 5s | BUILD SUCCESSFUL | `/tmp/opencode/session-close-check.log` |

### Windows session（2026-11-02）

| 指令 | Exit | Duration | 結果 | 證據 |
|---|---|---|---|---|
| `.\scripts\bootstrap-jdk.ps1` | 0 | 7m44s | Temurin `21.0.12+8` 安裝到 `.local\jdk-21`，SHA-256 `9ba963ee…21d0` 相符 | `.local\downloads\OpenJDK21U-jdk_x64_windows_hotspot_21.0.12_8.zip` |
| `.\scripts\gradlew-local.ps1 --version` | 0 | 下載 150 MB | Gradle `9.5.0`、Launcher JVM `21.0.12 (Eclipse Adoptium)`、Daemon JVM `.local\jdk-21` | 終端輸出 |
| `.\scripts\gradlew-local.ps1 test` | 0 | 2m22s | 45 tests, 0 failures, 0 errors, 0 skipped（19 suites；首次含相依下載） | `build\test-results\test\` |
| `.\scripts\gradlew-local.ps1 test --rerun-tasks` | 0 | 33s | 49 tests, 0 failures, 0 errors, 0 skipped | `build\test-results\test\` |
| `.\scripts\gradlew-local.ps1 check` | 0 | 1s | BUILD SUCCESSFUL | 終端輸出 |
| `.\scripts\gradlew-local.ps1 run --args="--render build/prototype"` | 0 | 4s | 橫排 10 頁、直排 11 頁、3,761 字；另產出 underline 版本 | `build\prototype\*.png` |
| `.\scripts\gradlew-local.ps1 run --args="--benchmark"` | 0 | 10s | 10 MiB、5,934,667 字、解碼 125.5 ms、橫排 228.3 ms／7,531 頁、直排 270.6 ms／8,091 頁 | 終端輸出 |
| `.\scripts\gradlew-local.ps1 run`（GUI） | 執行中 | — | `toolkit=sun.awt.windows.WToolkit`、`tray: installed` | `%TEMP%\opencode\gui-run.log` |
| 第二個實例開 Big5 檔 | 0 | 3s | secondary 立即結束；primary log 出現 `opened ... encoding=Big5 chars=94 pages=1` | `%TEMP%\opencode\gui-run.log` |

Big5 測試檔：`%TEMP%\opencode\eyePageFlow-big5-win.txt`（自製文字，146 bytes，Big5／CP950、CRLF）。

### Linux 先前累積驗證

| 指令 | Exit | 結果 | 證據 |
|---|---|---|---|
| `./scripts/gradlew-local.sh test` | 0 | 45 tests, 0 failures, 0 skipped | `build/test-results/test/` |
| `./scripts/gradlew-local.sh check` | 0 | BUILD SUCCESSFUL | 終端輸出 |
| `./scripts/gradlew-local.sh run --args="--render build/prototype"` | 0 | 橫排 11 頁、直排 8 頁、3,761 字；另產出 underline 版本 | `build/prototype/*.png` |
| `./scripts/gradlew-local.sh run --args="--benchmark"` | 0 | 10 MiB、5,934,667 字、解碼 177.2 ms、橫排 191.4 ms／7,889 頁、直排 342.9 ms／6,838 頁 | 終端輸出 |
| `JAVA_HOME=.local/jbr-21 JAVA_OPTS=-Dawt.toolkit.name=WLToolkit build/install/eyePageFlow/bin/eyePageFlow` | 0 | `toolkit=sun.awt.wl.WLToolkit`、`session=wayland` | 終端輸出 |
| `JAVA_HOME=.local/jbr-21 JAVA_OPTS=-Dawt.toolkit.name=XToolkit ...` | 0 | `toolkit=sun.awt.X11.XToolkit`、`session=wayland` | 終端輸出（XWayland，非原生 X11） |
| `... eyePageFlow /tmp/opencode/eyePageFlow-big5.txt`（WLToolkit） | 0 | `encoding=Big5 chars=3320 pages=9` | 終端輸出 |
| `... eyePageFlow /tmp/opencode/eyePageFlow-big5.txt`（XToolkit） | 0 | `encoding=Big5 chars=3320 pages=9` | 終端輸出 |
| `... eyePageFlow --settings`（WLToolkit） | 0 | `display settings: theme=realistic layout=AUTO pageWidth=900 fontSize=24.0` | 終端輸出 |
| `... eyePageFlow --editor /tmp/opencode/eyePageFlow-big5.txt` | 0 | `editor mode: chars=3320`，RSyntaxTextArea 正常建立 | 終端輸出 |
| `... eyePageFlow --find /tmp/opencode/eyePageFlow-big5.txt` | 0 | `editor mode` + `find dialog`，無 focus exception | 終端輸出 |
| `... eyePageFlow --chapters /tmp/opencode/eyePageFlow-big5.txt` | 0 | `chapter list: 40 chapters, rule=default` | 終端輸出 |
| `... eyePageFlow --chapter-rule /tmp/opencode/eyePageFlow-big5.txt` | 0 | `chapter rule dialog: default` | 終端輸出 |
| `XDG_CONFIG_HOME=/tmp/opencode/eyePageFlow-config ... --bookmarks` | 0 | `bookmark manager: 1 bookmarks` | 終端輸出 |
| `XDG_CONFIG_HOME=/tmp/opencode/eyePageFlow-config ... --library` | 0 | `library dialog: 1 shelves` | 終端輸出 |
| `XDG_CONFIG_HOME=/tmp/opencode/eyePageFlow-config ... --files` | 0 | `file list: /tmp/opencode` | 終端輸出 |
| 一般啟動 | 0 | `tray: not supported, boss key will minimize` | 終端輸出 |
| 第二個實例開同檔 | 0 | secondary 立即結束；primary log 出現第二次 `opened ...` | `/tmp/opencode/primary.log` |
| `... eyePageFlow --demo /tmp/opencode/eyePageFlow-big5.txt` | 0 | 自動翻頁 `3/9`、`5/9`、`7/9`、`9/9`，SLIDE 動畫無例外 | 終端輸出 |
| `./scripts/bootstrap-jbr.sh` | 0 | JRE／SDK 已存在，checksum 通過 | 終端輸出 |
| `jlink --add-modules java.base,java.desktop,java.logging,java.prefs` | 0 | runtime image 97 MB | `.local/jbrsdk-21-runtime/` |
| `jpackage --type app-image` | 0 | app-image 可啟動 | `build/package/eyePageFlow/bin/eyePageFlow` |
| `build/package/eyePageFlow/bin/eyePageFlow --render build/prototype-jbr` | 0 | 直排／橫排 PNG 產生 | `build/prototype-jbr/` |

JBR 下載與 checksum：

- JRE：`jbr-21.0.11-linux-x64-b1163.116.tar.gz`
  SHA-256：`72bf2d1830587a3cd31b718d4af9fc7a88fc6c3740642d842b3b378d4acbe476`
- SDK：`jbrsdk-21.0.11-linux-x64-b1163.116.tar.gz`
  SHA-256：`097bf328b5bf5a236c095b8c8e5204c0eda91e5baa06b37383a7754b10671d8b`

尚未驗證：

- Ubuntu 24.04 原生 X11（需 Xorg session）。
- Ubuntu 24.04 Wayland。
- Windows 10／11 其他版本（本次為 Windows 10 IoT 企業版 LTSC 21H2／19044）。
- Windows GUI 互動：拖放、IME、125%／150% 縮放、檔案對話框、剪貼簿、編輯存檔。
- 直排標點與字型 fallback 的 golden image。
- 10 MiB 以上的檔案與 20 MiB 上限。
- 編輯器 10 MiB 的輸入延遲（M7）。

## Known issues

- Windows 未安裝 Noto Serif CJK TC，`Typeface` 會依序退回 Noto Serif TC／新細明體／Noto Sans TC；Windows 與 Linux 的字型不同，頁數也會不同。
- 直排半角字目前用字寬前進，未做整體字串的字距微調；旋倒字串在窄欄位仍可能與相鄰欄輕微重疊。
- `pwsh -File <script> --args="C:\path"` 會把磁碟機代號的 `:` 當成參數分隔而拆成兩個引數；請直接用 `.\scripts\gradlew-local.ps1 run --args="C:\path"`，或改用 `pwsh -Command "& '.\scripts\gradlew-local.ps1' run '--args=C:\path'"`。
- 行尾由 `.gitattributes` 統一（文字檔 LF、`*.bat`／`*.cmd` CRLF）。既有 clone 若仍顯示整批 modified，執行 `git add --renormalize .` 或重新 clone。
- 直排標點目前使用 Unicode 直排形式；部分字型不支援時會 fallback，尚未量測。
- 禁則（行首／行尾標點）與左右對齊已完成。
- 直排縮排以「欄頂跳 2 格」近似；遇到新頁或空白行仍需更多測試。
- 尚未處理 surrogate pair；emoji／罕用字可能被拆開。
- 每次重繪都重新產生程序化材質；動畫前需要快取。
- 顯示設定已露出主題、版面、頁寬、字體家族、字級、行高、粗體、底線、五組顏色、三組內置圖案、配色方案、中縫具名預設色、動畫；CURL 目前是平移＋陰影的近似。
- 章節規則、書籤、書庫與老闆鍵已可用；托盤在目前 GNOME／Wayland 不可用，老闆鍵以最小化替代。
- 編輯模式空白鍵仍是插入空白，尚未照原作改成「下一頁」。
- 檔案關聯註冊尚未實作；目前依靠命令列參數與單一實例轉送。
- `jpackage` app-image 目前把 `-Dawt.toolkit.name=WLToolkit` 寫死；X11 版需要 session-aware launcher，留在 M10 修正。
- `jpackage` app-image 尚未包成 AppImage／MSI；正式打包在 M10。
- Ubuntu 26.04 的 X11 session 不是預設，尚未測試。
- 原作素材與說明文件有著作權，未包含在專案中。

## M0 判定

**Go。** 分頁速度、直排／橫排渲染、JBR 原生 Wayland、app-image 啟動都已在目前環境驗證。Windows 端已補上建置、測試、渲染、benchmark、GUI 啟動與單一實例；最大風險仍是直排標點與禁則的精確度，以及尚未驗證的 Ubuntu 24.04 X11 與 Windows GUI 互動（拖放／IME／縮放／編輯存檔），列入 M1／M3 的必檢項目。
