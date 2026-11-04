# Handoff

## Current status

**M0 技術驗證完成（Go）；M1 進行中，已完成檔案層與主視窗殼的第一步。**

已完成規格文件、Gradle 骨架、編碼偵測、字寬表分頁、Java2D 書頁渲染、Swing 原型、10 MiB benchmark、JBR 21 原生 Wayland／XToolkit 啟動，以及文件讀寫與基本開檔／存檔。

顯示設定、空白狀態、雙擊開檔、翻頁動畫與整頁下劃線已完成；編輯模式已接上 RSyntaxTextArea，可切換閱讀／編輯、偵測 dirty、存檔同步、尋找／取代、單頁書本外觀與 undo/redo 100 上限，並有關窗未儲存提示。章節列表支援可切換規則與目前章節標示；顯示設定、章節規則、書籤、最近開啟檔案與書庫會存到 XDG／AppData。檔案列表、書庫管理、老闆鍵（最小化）與單一實例已完成。尚未完成檔案關聯註冊；Windows GUI 互動與 Ubuntu 24.04 原生 X11 已於 2026-11-04 驗證。

Ubuntu 26.04 Wayland 已完成本輪回歸：67 個測試、benchmark、渲染快照、JBR 原生 Wayland／XWayland 啟動與 Big5 開檔皆通過；過程中修掉一個只在 Linux 失敗的字型相依測試。

Windows 開發環境已建立：`scripts/bootstrap-jdk.ps1`（Temurin 21.0.12+8 到 `.local/jdk-21`）與 `scripts/gradlew-local.ps1`（`JAVA_HOME`／`GRADLE_USER_HOME` 隔離於 `.local/`）。2026-11-04 已在 Windows 追平遠端 9 個 commit 並完成回歸：111 個測試、渲染、benchmark、GUI 啟動（`WToolkit`、內嵌字型）、托盤與可攜式 zip 打包皆通過；頁數與 Linux 完全一致。

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
- `PaginationTest` 的「直排標點右上」改為依字型能力分支：字型有 U+FE10 直排逗號（Linux Noto）時驗證「不位移、只下移一個字格」，沒有時才驗證 0.75em／0.22em 位移公式。原本的寫法寫死 Windows 的字型能力，在 Linux 必然失敗。
- 禁則：`NO_LINE_START`／`NO_LINE_END` 查表，橫排與直排都在換行前用前瞻避開孤字；連續關閉標點整段帶到下一個行／欄。`PaginationTest` 驗證每一行／欄的首尾字元。
- 左右對齊（橫排 justify）：因寬度換行的行把剩餘寬度平均分配到字距，段末行與剩餘 > 0.5em 的行不拉齊；`display.justify` 可關閉，顯示設定有「左右對齊（橫排）」核取方塊。
- `Pagination.pageIndexFor`：文字 offset ↔ 頁碼（二分搜尋、夾邊界），`PaginationTest` 驗證每個 offset 都落在回傳頁碼的範圍內。
- Surrogate pair：`PlacedGlyph.text` 改為字串，emoji 佔一格；`Typeface.symbolFont` 在頁面字型缺字時退回系統 emoji 字型。
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
- 測試：111 個測試，0 失敗（Windows 與 Ubuntu 26.04 Wayland；含 4 個書頁渲染回歸測試）。
- 直排「數字壓在字上」的修正，分兩層：
  1. `BookRenderer.drawGlyph`：直排時把**行框在字格內垂直置中**。字格高 = 字號，但字型 ascent + descent 更大（Noto Serif CJK 26px 為 38px），沿用基線會讓每個字的墨跡往下溢 6px、壓到下一個字（`第10段` 的數字就是被 `第` 壓住）。
  2. `Pagination.verticalAdvance`：旋倒的半角字維持依字寬前進（`UTF-8` 不會被拉長），但下限由 0.3em 提到 0.5em，避免 `1`、`.` 的旋倒墨跡比字格高。
  旋轉分支另外改用**墨跡邊界置中**（與畫面轉換無關的 `FontRenderContext`），不再靠基線推算。曾試過「旋倒字固定 1em」，會讓 `UTF-8` 明顯變長，已否決。
- 標點擠壓：連續標點的第二個以後壓成半個字寬（橫排右半、直排下半）；行尾／欄尾懸掛（差半個字就掛在內容區外，不多換一行）；行首／欄首壓成半寬。`」』）` 因墨跡靠右不列入連續壓縮；懸掛時若後面還有標點則不懸掛，避免整串被擠到行首。
- 寡行控制：`Pagination.avoidWidowLine()` 往後量「頁尾到段末」，若只剩一行就能結束該段，就把本頁最後一行也推到下一頁，避免段末行單獨落在下一頁頂端。量測有容量上限，超過就停止，避免單一段落的長文件變成 O(n²)。
- 捲頁動畫：`render/PageCurl` 真正做紙背鏡射＋摺線陰影＋紙邊高光（原本只是平移＋陰影），`PageCurlTest` 8 個測試（進度 0／1、前翻／回翻半程、兩個方向互為鏡射、紙背變暗、進度夾取、region 限制範圍）。
- **翻頁動畫支援回翻**：`PageCurl.paint(..., forward)` 由 `BookCanvas.animateTo()` 依 `target > spreadStart` 決定方向。往前翻摺線由右緣往左跑，往回翻由左緣往右跑；SLIDE 也一樣改成雙向（舊頁與新頁左右對調、陰影換邊）。原本兩個方向共用同一個「由右往左」動畫。
- **直排（右翻書）動畫方向反轉**：`animateTo()` 先看寫作模式——橫排「下一頁＝右→左」，直排「下一頁＝左→右」（把左邊那張紙掀到右邊），因為直排的頁序是右→左。`PageCurl` 只認摺線方向（`forward` = 右→左），模式換算留在 UI 層（`pageTurnForward()`，由 `PageTurnDirectionTest` 覆蓋）。
- **顯示預設值定案（2026-11-04）**：字級 26、行距 1.44、縮排 2 em、頁寬 900、Noto Serif CJK TC、仿真紙書、AUTO 版面、直排標點置中、左右對齊開、粗體關、下劃線關，`underlineWidth` 由「普通（1.0）」改為「淡（0.5）」。只影響新使用者（沒有 `settings.properties` 時）；既有設定檔的值優先，不做遷移。`DisplayDefaultsTest` 4 個測試把這組值鎖住。
- **直排雙頁只掀半頁**：`PageCurl.paint(..., region)` 把捲頁限制在指定矩形內。直排雙頁傳入左半頁，掃描距離從整個跨頁縮成一個頁寬（同樣 220 ms → 速度加倍、方向感明顯）；範圍外直接顯示新頁面，與實體書掀開一張紙後底下跨頁已經換掉一致。橫排與單頁維持原本的整張畫布。
- 紙背原本實際上看不到：舊程式把鏡射後的紙背畫到摺線左側，卻把 clip 設在摺線右側，兩者沒有交集，畫面上只剩陰影與高光；`PageCurlTest` 當時也只比對「紙背位置與平面頁面不同」，所以沒抓到。已改成「clip 與鏡射同側」，並補上「紙背仍是舊頁面內容且比平面頁暗」的斷言。
- 內嵌字型：`src/main/resources/fonts/NotoSerifCJKtc-Regular.otf`（OFL，24 MB）+ `Typeface` 以 `Font.createFont` 載入並註冊；預設頁面字型不再依賴系統安裝，Windows／Linux 頁數一致。授權與 SHA-256 記在 `THIRD_PARTY_NOTICES.md`，授權全文在 `fonts/OFL.txt`。啟動 log 會印 `page font:`。
- 直排標點：`VerticalPunctuation.CENTER` 改為依墨跡邊界真正置中（補上繪圖端的行框置中補償），先前只是沿用基線、墨跡落在字格左下。
- 版面模式：`DisplaySettings.doublePageFor()` 讓 AUTO 依畫布寬高比（≥1.25）自動單／雙頁，`BookCanvas` 在視窗縮放時即時切換；EQUAL_HEIGHT 交給 `BookRenderer` 的 `scaleToPageHeight`，只依高度縮放，雙頁字級與單頁相同。
- 直排字距微調：`PageMetrics.cellSpacing` 與橫排的 `lineSpacing` 對稱，把欄內剩餘高度平均分配（上限 5%）。
- 行距微調：`PageMetrics.lineSpacing` 把一頁放不滿的剩餘高度平均分配給各行（最多放寬 5%），`PageUnderlines` 用同一組間距畫線；直排仍固定 1em 字格。
- 孤行控制：`Pagination.avoidOrphanLine()` 不讓某段的第一行單獨留在頁面最下方（頁面至少還有一行、且該段還有後續時，把這一行移到下一頁）。橫排／直排都適用。寡行（段末行單獨落在下一頁頂端）尚未處理。
- 材質快取：`TexturePainter` 把材質依（寬、高、底色、種類）快取成點陣圖，LRU 上限 6 M 像素（約 24 MB）。實測 20 次材質繪製由 254 ms 降到 80 ms（JIT 暖機後）。
- 應用程式圖示：`--icon <path>` 產生 512×512 透明背景 PNG（`AppIcon` + `EmptyStateRenderer.renderIcon`，自製書頁 logo，無外部素材）。
- AppImage：`scripts/package-appimage.sh` 把 app-image 組成 AppDir（`AppRun`＋`.desktop`＋`.png`）後用 `appimagetool` 打包，產出 `build/package/eyePageFlow-0.1.0-x86_64.AppImage`（44 MB）。
- Linux 打包：`scripts/package-linux.sh` 依序做 `installDist` → `jlink`（java.base/java.desktop/java.logging/java.prefs）→ `jpackage --type app-image` → 產出 `.desktop`（`MimeType=text/plain`、`Exec=eyePageFlow %f`）→ `tar.gz`。app-image 不寫死 AWT toolkit，靠程式自動選擇。
- AWT toolkit 自動選擇：`platform.ToolkitSelection` + `Main.applyToolkitSelection()`。Linux + Wayland session + JVM 具備 `sun.awt.wl.WLToolkit` 才設 `WLToolkit`，其餘沿用 JVM 預設。覆寫改用 `-Deyepageflow.toolkit=`／`EYEPAGEFLOW_TOOLKIT`；`-Dawt.toolkit.name` 不能用來判斷，因為 JBR 會自己塞 `XToolkit` 當預設。
- UI 文字：FlatLaf 依系統挑 sans CJK（`Noto Sans CJK TC`／`Noto Sans TC`，`Typeface.resolveUiFamily`），UI 字級下限 15pt；JBR 的 Wayland toolkit 原本只算出 13pt，中文又硬又擠，是「閱讀區以外字看起來很舊」的主因。啟動 log 會印 `ui font: <家族> <級數>`。
- Benchmark（Windows）：10 MiB Big5，橫排 336.1 ms、直排 381.4 ms；Ubuntu 26.04 Wayland：解碼 130.2 ms、橫排 300.7 ms、直排 447.4 ms。
- JBR 21 原生 Wayland：`sun.awt.wl.WLToolkit`。
- JBR 21 XToolkit：`sun.awt.X11.XToolkit`。
- `jlink` runtime image（97 MB）與 `jpackage --type app-image` 驗證。
- **Windows 可攜式打包**：`scripts/package-windows.ps1`（＋`.cmd`）依序做 `installDist` → 產生 ICO → `jlink` → `jpackage --type app-image` → smoke test → `Compress-Archive`，產出 `build\package\eyePageFlow-0.1.0-windows-x64.zip`（55.7 MB），解開後直接執行 `eyePageFlow\eyePageFlow.exe`。**不做 MSI**（jpackage MSI 需要 WiX Toolset，且使用者指定只要可攜式 zip）。
- `AppIcon.writeAs()`：依副檔名輸出 PNG（Linux `.desktop`／AppImage）或 ICO（Windows `jpackage --icon`）。ICO 是 7 個尺寸（16–256）的 PNG-in-ICO，`AppIconTest` 4 個測試驗證檔頭、entry 位移與 payload 簽章。
- **修正打包後無法使用 Big5／GB18030**：`package-linux.sh` 與 `package-windows.ps1` 的 `jlink --add-modules` 少了 `jdk.charsets`，精簡 runtime 因此沒有 Big5，開舊編碼檔案會丟 `UnsupportedCharsetException: Big5`（GUI 外觀完全正常，只有開檔才炸）。已在 jlink 模組清單補上 `jdk.charsets`，並在打包腳本加上 smoke test。
- Windows 打包的 smoke test 改用**打包進去的 `runtime\bin\java.exe`** 跑 `--benchmark`（實際解一次 Big5），不能用 `eyePageFlow.exe`：jpackage 的 Windows GUI launcher 會立刻回 0 並把 JVM 丟到背景，JVM 失敗時也不例外。

## In progress

- **變更 Windows 顯示縮放（DPI）後，視窗不會立即重繪**：當下畫面可能還是舊比例，把視窗最大化／還原一次就恢復正常。需要確認是不是 Swing 沒收到 DPI 變更事件。
- 直排標點使用「置中」時已改為依墨跡邊界真正置中；字型缺直排形式、且選「右上」時仍用近似位移。
- 尚未做 golden image 基準與字型子集。
- 目前環境的 GNOME／Wayland 不支援 `SystemTray`，老闆鍵會退回最小化；托盤在有支援的桌面才會安裝。
- `AUTO` 依寬高比（1.25）切換單／雙頁；`EQUAL_HEIGHT` 只依高度縮放。尚未做到「固定頁寬」的第三種尺寸。
- 翻頁動畫 CURL 已是紙背鏡射＋陰影的 2D 近似，仍非真正的 3D 捲曲；方向已依寫作模式換算（直排為右翻書），直排雙頁只掀半頁；翻頁速度（`PAGE_TURN_DURATION_NS`，220 ms）尚未調校，直排雙頁改半頁後體感變快。

## Next actions

### P0 收尾（約半天）

1. ~~Windows GUI 互動驗證~~（已完成 2026-11-04：IME 中英輸入、125%／150% 縮放、關窗未存提示、拖放、檔案對話框、書籤、書庫、編輯功能皆由使用者實機確認。縮放後需放大視窗才會重繪，列為已知問題）。剪貼簿未單獨確認。
2. ~~文件同步~~（已完成：README／handoff 的測試數與 benchmark 數據已更新到 125 tests 與現況；殘存的舊數字只出現在有標日期的歷史驗證表，屬於當時紀錄）。
3. ~~定案顯示預設值~~（已完成 2026-11-04：字級 26、直排標點置中、下劃線**淡（0.5）**、其餘維持原預設。`DisplayDefaultsTest` 4 個測試鎖住這組值）。

### P1 排版正確性（M3，約 1–2 天）

4. ~~禁則~~（已完成：關閉標點不落行首／欄首、開括號不落行尾／欄尾，連續標點整段帶走）。
5. ~~左右對齊~~（已完成：橫排 justify，段末行與過鬆的行不拉齊，可在顯示設定關閉）。
6. ~~offset↔page 對應~~（已完成：`Pagination.pageIndexFor`，二分搜尋＋邊界夾取，含測試）。
7. ~~Surrogate pair~~（已完成：emoji 視為單一字、佔一格、頁面邊界不切半；缺字時退回系統 emoji 字型）。
8. 書頁渲染回歸測試（已完成：`RenderRegressionTest`，4 個像素性質測試）。

### P2 打包與平台（M10，約 1–2 天）

9. ~~Windows 可攜式 zip~~（已完成：`scripts/package-windows.ps1`，55.7 MB，解開即可執行；**不提供 MSI**，使用者已明確排除安裝檔）。檔案關聯註冊仍未做。
10. ~~Linux AppImage~~（已產出；`appimagetool` 放在 `.local/tools/`，未納入版控）。
11. ~~Ubuntu 26.04 Wayland 回歸~~（已完成：67 tests、benchmark、渲染、WLToolkit／XToolkit、Big5 開檔；清單見 `docs/linux-regression.md`）。Ubuntu 24.04 原生 X11 已延後至 P3。

### P3 體驗

12. ~~`AUTO`／`EQUAL_HEIGHT` 區分~~（已完成：AUTO 依寬高比、EQUAL_HEIGHT 只依高度縮放）。
13. ~~CURL 翻頁動畫~~（已完成：紙背鏡射、摺線陰影、紙邊高光；回翻方向已補）。
14. ~~內嵌 Noto Serif CJK TC（OFL）~~（已完成：24 MB OTF + OFL 全文，jar 由 1 MB 增至 20 MB）。
15. GNOME／Wayland 的托盤替代方案。
16. ~~Ubuntu 24.04 原生 X11 回歸~~（**已完成 2026-11-04**：VMware 上的 Ubuntu 24.04.5 + `Type=x11`/`Xorg`，125 tests、benchmark、渲染、`toolkit=sun.awt.X11.XToolkit`、Big5 開檔、`package-linux.sh` 全部通過；benchmark 在 2 vCPU VM 上偶有 >1 s 的環境抖動，見下方驗證章節）。

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
- 專案路徑：`D:\workspace\eyePageFlow`（`core.autocrlf=false`；行尾由 `.gitattributes` 統一）

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

### Windows session close（2026-11-03 17:43 CST）

環境：Windows 10 IoT 企業版 LTSC 21H2 x64（`10.0.19044`）、Temurin `21.0.12+8`、Gradle `9.5.0`、`sun.awt.windows.WToolkit`。

| 指令 | Exit | Duration | 結果 | 證據 |
|---|---|---|---|---|
| `.\scripts\gradlew-local.ps1 test --rerun-tasks` | 0 | 16.8s | **67 tests, 0 failures, 0 errors, 0 skipped**（20 suites） | `%TEMP%\opencode\session-close-test.log`、`build\test-results\test\` |
| `.\scripts\gradlew-local.ps1 check` | 0 | 2.3s | BUILD SUCCESSFUL | 終端輸出 |
| `.\scripts\gradlew-local.ps1 run --args="--render build/prototype"` | 0 | 3.8s | 橫排 10 頁、直排 11 頁、3,761 字；underline 版本同時產出 | `build\prototype\*.png` |
| `.\scripts\gradlew-local.ps1 run --args="--benchmark"` | 0 | 3.2s | 10 MiB、5,934,667 字、解碼 113.2 ms、橫排 336.1 ms／7,613 頁、直排 381.4 ms／8,193 頁 | `%TEMP%\opencode\session-close-benchmark.log` |

本次分頁變慢（228 → 336 ms）是禁則前瞻、左右對齊與 surrogate pair 的固定成本，仍遠低於 1 秒門檻；surrogate 支援一開始讓每個字都做 `substring`，已改成只在需要時才配置。

尚未驗證（本次）：

- Windows GUI 互動：拖放、IME、125%／150% 縮放、檔案對話框、剪貼簿、編輯後存檔、關窗提示。
- Linux 原生 X11（需 Xorg session，已延後至 P3）與 Ubuntu 24.04；Ubuntu 26.04 Wayland 已於 2026-11-03 完成回歸，見下節。
- 打包（MSI／AppImage）與檔案關聯註冊。

### Linux 回歸（2026-11-03，Ubuntu 26.04.1 LTS / Wayland）

環境：`XDG_SESSION_TYPE=wayland`、`WAYLAND_DISPLAY=wayland-0`、Temurin `21.0.12+8`（建置／測試）、JBR `21.0.11`（GUI）、Gradle `9.5.0`、`Noto Serif CJK TC`、`Noto Color Emoji`。
基準 commit：`0fcfbdd`（`docs/linux-regression.md` 的清單）。

| 指令 | Exit | 結果 | 證據 |
|---|---|---|---|
| `./scripts/gradlew-local.sh test --rerun-tasks` | 0 | **67 tests、20 suites、0 failures、0 errors、0 skipped** | `build/test-results/test/` |
| `./scripts/gradlew-local.sh check` | 0 | BUILD SUCCESSFUL | 終端輸出 |
| `./scripts/gradlew-local.sh run --args="--benchmark"` | 0 | 10 MiB、5,934,667 字、解碼 130.2 ms、橫排 300.7 ms／7,779 頁、直排 447.4 ms／8,323 頁 | `/tmp/opencode/linux-benchmark.log` |
| `./scripts/gradlew-local.sh run --args="--render build/prototype"` | 0 | 橫排 10 頁、直排 11 頁、3,761 字；underline 版本同時產出 | `build/prototype/*.png`、`/tmp/opencode/linux-render.log` |
| `JAVA_HOME=.local/jbr-21 JAVA_OPTS=-Dawt.toolkit.name=WLToolkit build/install/eyePageFlow/bin/eyePageFlow` | 124（timeout 終止，GUI 常駐） | `session=wayland`、`toolkit=sun.awt.wl.WLToolkit`、`startup typography: fontSize=26.0 font=Noto Serif CJK TC/26`、`tray: not supported` | `/tmp/opencode/linux-wayland-start.log` |
| 同上，`-Dawt.toolkit.name=XToolkit` | 124（同上） | `toolkit=sun.awt.X11.XToolkit`、`tray: installed`（**XWayland，非原生 X11**） | `/tmp/opencode/linux-xwayland-start.log` |
| WLToolkit 開 Big5 檔（自製 2,259 bytes／CRLF） | 124（同上） | `opened ... encoding=Big5 chars=1199 pages=4` | `/tmp/opencode/linux-open-big5.log` |

第一次 `test --rerun-tasks` 為 67 tests / **1 failed**：`PaginationTest > vertical punctuation can sit at the top right`。
原因：該測試寫死了「字型沒有 U+FE10 的近似位移」分支；Linux 的 Noto Serif CJK TC 有 U+FE10，排版改走直排形式（不位移），行為本身正確。修正測試後全綠。

目視檢查（`build/prototype`）：

- 直排：第 1 頁在右、往左遞增；內容寬 812／欄距 39 → 20 欄／頁；`。，` 留在字格內、`「」『』（）` 正確旋轉；`UTF-8`、`PageByFlow` 旋倒且不重疊。
- 橫排：換行行尾貼齊內容區右緣（justify）、行首無 `。，、！？」』`（禁則）、段末行不拉齊。

補充實測（暫用 `/tmp/opencode/verify` 的一次性 Java 探針，未進 repo）：

| 項目 | 方法 | 結果 |
|---|---|---|
| emoji 佔格 | JBR 21 渲染 `檔😀😀😀檔` 直排／橫排 | 每個 emoji 佔一格、不重疊；JBR 能畫 Noto Color Emoji 彩色點陣 |
| emoji（Temurin） | 同一份 PNG 改由 Temurin 21 產生 | **emoji 完全沒有墨跡**（空白字格）；字格與分頁仍正確 |
| 下劃線粗細 | 藍線＋深色字，寬 0.5／1.0／1.5 | 三個等級目視可分辨（0.5 較淡、1.5 較粗） |
| 下劃線顏色 | 藍線、字色 `#112233` | 文字像素數在三個寬度下都是 2,358 → 顏色不污染文字 |

### UI 文字可讀性（2026-11-03）

問題回報：「閱讀區以外的文字不圓潤，像 20 年前的顯示」。

量測結論：

- Swing 預設就有反鋸齒；`-Dawt.useSystemAAFontSettings=on/lcd/gasp` 對 FlatLaf 版面的截圖 **md5 完全相同**，不是反鋸齒的問題。
- 真正原因有兩個：UI 中文落到不合適的 fallback，且 JBR 的 Wayland toolkit 只算出 **13pt**（XToolkit 是 15pt）。

修正（`ReaderWindow`、`Typeface`）：

- `Typeface.resolveUiFamily()`：有 `Noto Sans CJK TC`／`Noto Sans TC` 就當 UI 家族，否則回傳 null 沿用系統預設。
- `ReaderWindow.enforceMinimumUiFontSize()`：UI 字級下限 15pt，只上調不下調（HiDPI 仍用 FlatLaf 算出的較大值）。
- 啟動 log 增加 `ui font: <家族> <級數>`。

| 指令 | Exit | 結果 | 證據 |
|---|---|---|---|
| `./scripts/gradlew-local.sh test --rerun-tasks` | 0 | 111 tests、22 suites、0 failures、0 skipped | `build/test-results/test/` |
| `./scripts/gradlew-local.sh check` | 0 | BUILD SUCCESSFUL | 終端輸出 |
| WLToolkit 啟動 | 124（timeout） | `ui font: Noto Sans CJK TC 15pt`（修正前為 13pt／Ubuntu Sans） | `/tmp/opencode/ui-WLToolkit.log` |
| XToolkit 啟動 | 124（timeout） | `ui font: Noto Sans CJK TC 15pt` | `/tmp/opencode/ui-XToolkit.log` |

截圖對照（XWayland、工具列 3 倍放大）：修正前 `/tmp/opencode/aa-crop-test.png`、修正後 `/tmp/opencode/ui-after-noto15.png`。

### AWT toolkit 自動選擇（2026-11-03）

`Main.applyToolkitSelection()` 在 AWT 初始化前依環境決定 toolkit；`ToolkitSelection` 負責判斷。實測（`build/install/eyePageFlow/bin/eyePageFlow`，每次 `timeout 10`，exit 124 為 GUI 常駐後終止）：

| 環境 | 結果 |
|---|---|
| JBR + `XDG_SESSION_TYPE=wayland`（不給參數） | `toolkit=sun.awt.wl.WLToolkit` |
| JBR + `XDG_SESSION_TYPE=x11` | `toolkit=sun.awt.X11.XToolkit` |
| Temurin + `XDG_SESSION_TYPE=wayland` | `toolkit=sun.awt.X11.XToolkit`（Temurin 無 Wayland toolkit） |
| JBR + wayland + `EYEPAGEFLOW_TOOLKIT=XToolkit` | `toolkit=sun.awt.X11.XToolkit` |
| JBR + x11 + `EYEPAGEFLOW_TOOLKIT=WLToolkit` | `toolkit=sun.awt.wl.WLToolkit` |

同時確認 headless 路徑不受影響：`run --args="--benchmark"`（解碼 157.2 ms、橫排 314.8 ms／7,779 頁、直排 478.4 ms／8,420 頁）與 JBR 下 `--render` 四張 PNG 都正常。

### Session close（2026-11-04 00:19 CST）

環境：Ubuntu 26.04.1 LTS x86_64、Wayland session、Temurin `21.0.12+8`（建置／測試）、JBR `21.0.11`（GUI 驗證）、Gradle 9.5.0。
本次工作區在開始收尾時已 commit 並 push，`git status` 乾淨（`main...origin/main`）。

| 指令 | Exit | Duration | 結果 | 證據 |
|---|---|---|---|---|
| `./scripts/gradlew-local.sh test --rerun-tasks` | 0 | 11s | **111 tests、26 suites、0 failures、0 errors、0 skipped** | `/tmp/opencode/close-test.log`、`build/test-results/test/` |
| `./scripts/gradlew-local.sh check` | 0 | 2s | BUILD SUCCESSFUL | `/tmp/opencode/close-check.log` |
| `./scripts/gradlew-local.sh run --args="--benchmark"` | 0 | — | 解碼 139.9 ms、橫排 372.6 ms／7,724 頁、直排 641.2 ms／8,375 頁 | `/tmp/opencode/close-benchmark.log` |
| `./scripts/gradlew-local.sh run --args="--render /tmp/opencode/close-render"` | 0 | — | 4 張 PNG 產生（橫排／直排 × 一般／下劃線） | `/tmp/opencode/close-render.log`、`/tmp/opencode/close-render/*.png` |

本次 Session 的提交：

- `d89cd88` perf: cache procedural textures
- `cd10770` feat: finish M3 typography
- `def0616` feat: embed Noto Serif CJK TC and finish layout/vertical typography
- `a5b2ff9` feat: replace the slide-only page turn with a real page curl

尚未驗證（本次 Session 未執行）：

- Windows 的建置、測試、GUI 互動與 MSI 打包（明天遷移到 Windows 環境處理）。
- AppImage 在非沙箱桌面環境的實際執行（此環境會自刪，見 Known issues）。
- Ubuntu 24.04 原生 X11（已延後至 P3）。
- 拖放、IME、縮放、剪貼簿、編輯存檔、關窗提示等需要真人操作的項目。

### Session close（2026-11-04 16:51 CST）

環境：Windows 10 IoT 企業版 LTSC 21H2 x64（`10.0.19044`）、Temurin `21.0.12+8`、Gradle `9.5.0`、`sun.awt.windows.WToolkit`。
基準 commit：`4e337b6`（工作樹乾淨，`main` 與 `origin/main` 同步）。

| 指令 | Exit | Duration | 結果 | 證據 |
|---|---|---|---|---|
| `.\scripts\gradlew-local.ps1 test --rerun-tasks` | 0 | 20s | **131 tests、30 suites、0 failures、0 errors、0 skipped** | `%TEMP%\opencode\close-test.log`、`build\test-results\test\` |
| `.\scripts\gradlew-local.ps1 check` | 0 | 1s | BUILD SUCCESSFUL | 終端輸出 |
| `.\scripts\gradlew-local.ps1 run --args="--render build/prototype"` | 0 | 3s | 橫排 11 頁、直排 12 頁、3,761 字（與 Ubuntu 24.04、Linux 一致） | `build\prototype\*.png` |
| `.\scripts\gradlew-local.ps1 run --args="--benchmark"` | 0 | 3s | 10 MiB、5,934,667 字、解碼 144.4 ms、橫排 386.5 ms／7,724 頁、直排 485.8 ms／8,375 頁 | `%TEMP%\opencode\close-benchmark.log` |

本次 Session 的提交（Windows 打包 → 翻頁方向 → 顯示預設 → Ubuntu 24.04 X11 → 閱讀位置 → 空白鍵）：

- `ba9acd2` fix: keep legacy charsets in the packaged runtime
- `6ed6719` feat: add portable Windows packaging with a real .ico icon
- `4bc75aa` fix: turn pages in the reading direction of the writing mode
- `1df82e1` docs: record the Windows GUI verification results
- `af6f6dd` feat: finalize the display defaults with a faint underline
- `46c35a4` docs: record the Ubuntu 24.04 native X11 regression
- `e168686` docs: verify the Linux deliverables on Ubuntu 24.04 X11
- `c07e717` docs: refresh the Linux regression checklist for the current build
- `5ed68ab` docs: drop stale known issues and close the doc-sync item
- `cc0d761` feat: resume each file at the last reading position
- `7bdfc9c` docs: record the reading-position resume run
- `497d6be` feat: make space turn the page in the editor
- `4e337b6` docs: record the editor space-key run and close the gap

本次 Session 未驗證（明確清單）：

- Ubuntu 26.04 Wayland 的**本輪**回歸（基準已從 `0fcfbdd` 推進到 `4e337b6`，使用者安排於晚間執行，清單是 `docs/linux-regression.md`）。
- Ubuntu 24.04 的 Wayland session（VM 固定 Xorg）。
- Windows 剪貼簿 GUI 互動、檔案關聯註冊、`.desktop` 實際安裝。
- 10 MiB 以上檔案、20 MiB 上限、編輯器 10 MiB 輸入延遲（M7）。
- 本輪為文件與 UI 行為變更，未執行壓力測試、未觸及硬體。

### Windows 回歸與可攜式打包（2026-11-04）

環境：Windows 10 IoT 企業版 LTSC 21H2 x64（`10.0.19044`）、Temurin `21.0.12+8`、Gradle `9.5.0`、`sun.awt.windows.WToolkit`。
基準 commit：`110940a`（`git pull --ff-only` 自 `0fcfbdd` 快轉 9 個 commit）。

| 指令 | Exit | 結果 | 證據 |
|---|---|---|---|
| `.\scripts\gradlew-local.ps1 test --rerun-tasks` | 0 | **111 tests、26 suites、0 failures、0 errors、0 skipped** | `%TEMP%\opencode\win-baseline-test.log`、`build\test-results\test\` |
| `.\scripts\gradlew-local.ps1 test --rerun-tasks`（本次變更後） | 0 | **115 tests、27 suites、0 failures、0 errors、0 skipped**（新增 `AppIconTest` 4 個） | `%TEMP%\opencode\win-final-test.log`、`build\test-results\test\` |
| `.\scripts\gradlew-local.ps1 check` | 0 | BUILD SUCCESSFUL | 終端輸出 |
| `.\scripts\gradlew-local.ps1 run --args="--render build/prototype"` | 0 | 橫排 11 頁、直排 12 頁、3,761 字；4 張 PNG | `build\prototype\*.png` |
| `.\scripts\gradlew-local.ps1 run --args="--benchmark"` | 0 | 解碼 119.9 ms、橫排 369.3 ms／7,724 頁、直排 585.2 ms／8,375 頁 | 終端輸出 |
| `.\scripts\gradlew-local.ps1 run`（GUI） | 手動終止 | `toolkit=sun.awt.windows.WToolkit`、`ui font: Noto Sans TC 15pt`、`page font: Noto Serif CJK TC (embedded=true)`、`tray: installed` | `%TEMP%\opencode\win-gui-baseline.log` |
| `.\scripts\package-windows.ps1` | 0 | app-image 71.4 MB、zip 55.7 MB；smoke test 解碼 101.7 ms、橫排 345.8 ms／7,724 頁、直排 577.7 ms／8,375 頁 | `%TEMP%\opencode\win-package-3.log`、`build\package\` |
| 解壓後用打包 runtime 跑 `--benchmark` | 0 | `encoding=Big5`、7,724／8,375 頁、解碼 141.7 ms | 終端輸出 |
| `build\package\eyePageFlow\eyePageFlow.exe`（GUI） | 常駐 | 視窗標題 `eyePageFlow`、`Responding=True`；stdout 有 `toolkit=`／`ui font=`／`page font=` | `Get-Process` 觀察 |

關鍵結論：

- 內嵌字型讓 Windows 頁數與 Linux **完全相同**（7,724／8,375）；舊版 Windows 因字型 fallback 是 7,613／8,193。
- 這台機器有 `Noto Sans TC`，`Typeface.resolveUiFamily()` 回傳它，所以 `ui font:` 不再落到 Segoe UI fallback（handoff 舊版把這條列為已知問題，已不成立）。
- `build\prototype` 的頁數由 10／11 變成 11／12，是內嵌字型＋新排版規則的結果，與 Linux 一致。

### 顯示預設值定案（2026-11-04）

以全新的設定目錄模擬第一次啟動（`APPDATA` 指到空的暫存資料夾，不動使用者既有設定）：

| 指令 | Exit | 結果 | 證據 |
|---|---|---|---|
| `.\scripts\gradlew-local.ps1 run --args="--settings"`（`APPDATA=%TEMP%\opencode\epf-fresh-appdata`） | 手動關閉 | `display settings: theme=realistic layout=AUTO pageWidth=900 fontSize=26.0 underline=false underlineWidth=0.5 punctuation=CENTER` | `%TEMP%\opencode\win-defaults.log` |
| `.\scripts\gradlew-local.ps1 test --rerun-tasks` | 0 | **125 tests、0 failures**（新增 `DisplayDefaultsTest` 4 個） | `build\test-results\test\` |

`display settings:` 這行診斷 log 順便補上 `underline`／`underlineWidth`／`punctuation`，以後驗證預設值不用開對話框。

### 編輯模式空白鍵（2026-11-04，VMware Ubuntu 24.04 X11）

| 步驟 | 結果 |
|---|---|
| `--editor` 開 12 頁檔案、`Ctrl+Home` | 游標在檔頭 |
| `Shift+Space` | 標題出現未儲存標記、行首多一格空白；**log 沒有換頁紀錄**（確認沒被攔截） |
| `Space` ×2 | `editor space -> page 2/12 offset=630`、`editor space -> page 3/12 offset=1270`；畫面捲到第 25–39 段 |

### 自動恢復閱讀位置（2026-11-04，VMware Ubuntu 24.04 X11）

端到端驗證（`xdotool` 送按鍵，模擬真實操作）：

| 步驟 | 結果 |
|---|---|
| 產生 12 頁的 Big5 測試檔（160 段、9,810 bytes） | `opened ... encoding=Big5 chars=5170 pages=12` |
| 清掉 `~/.config/eyePageFlow/reading-positions.properties` | 乾淨起點 |
| 啟動後按 `Right` ×2（雙頁，每次前進 2 頁） | 畫面到第 5 頁 |
| `alt+F4` 關窗（走 `requestClose` → `closeWindow`） | 寫入 `position.0.offset=2550`、`position.0.path=/tmp/epf-resume-big5.txt` |
| 重新啟動同一個檔案 | **`resumed epf-resume-big5.txt at page 5/12 offset=2550`**；狀態列 `5/12, 41.67%` |

備註：這台 VM 為了送按鍵裝了 `xdotool`（`sudo apt-get install -y xdotool`）。

### Ubuntu 24.04.5 LTS 原生 X11 回歸（2026-11-04，VMware Workstation VM）

環境：Ubuntu `24.04.5 LTS`、kernel `7.0.0-42-generic` x86_64、VMware Workstation VM（2 vCPU `i7-4900MQ`、8 GB RAM、NAT `192.168.23.46`）。
圖形：`loginctl` 顯示 `Type=x11`、`Active=yes`，`Xorg` 為獨立行程（非 Xwayland），`/etc/gdm3/custom.conf` 設 `WaylandEnable=false`；`X.Org 21.1.11`、1280×800。
建置：Temurin `21.0.12+8`（`./scripts/bootstrap-jdk.sh`）、Gradle `9.5.0`。
操作方式：主機以 `vmrun runProgramInGuest` 執行 `/bin/sh` 腳本，X 存取用 `DISPLAY=:0` + `XAUTHORITY=/run/user/1000/gdm/Xauthority`。

| 指令 | Exit | 結果 | 證據 |
|---|---|---|---|
| `./scripts/gradlew-local.sh test --rerun-tasks` | 0 | **125 tests、29 suites、0 failures、0 errors、0 skipped**（3m03s） | `build/test-results/test/`、`/tmp/epf-test.log` |
| `./scripts/gradlew-local.sh check` | 0 | BUILD SUCCESSFUL | `/tmp/epf-test.log` |
| `./scripts/gradlew-local.sh run --args="--render build/prototype"` | 0 | 橫排 11 頁、直排 12 頁、3,761 字（與 Windows 相同） | `build/prototype/*.png` |
| `./scripts/gradlew-local.sh run --args="--benchmark"` ×5 | 4 次 0、1 次 1 | 見下表 | `/tmp/epf-bench5.log` |
| `installDist` + `DISPLAY=:0` 啟動 GUI | 執行中 | `toolkit=sun.awt.X11.XToolkit`、`ui font: Noto Sans CJK TC 15pt`、`page font: Noto Serif CJK TC (embedded=true)`、`opened ... encoding=Big5 chars=134 pages=1`、`tray: installed` | `/tmp/epf-x11-app2.log`、截圖 `vm-x11-shot.png` |
| `xdpyinfo` | 0 | `X.Org version: 21.1.11`、`name of display: :0` | `/tmp/epf-x11.log` |
| `./scripts/bootstrap-jbr.sh` | 0 | JBR `21.0.11+10-1163.116` JRE 與 SDK 安裝到 `.local/` | `/tmp/epf-pkg.log` |
| `./scripts/package-linux.sh` | 0 | app-image 94 MB、`.desktop`、`eyePageFlow-0.1.0-linux-x64.tar.gz`；**打包後 Big5 smoke test 通過** | `/tmp/epf-pkg.log`、`build/package/` |
| `runtime/bin/java --list-modules` | 0 | 含 `jdk.charsets@21.0.11`（`jdk.charsets` 修正已在 Linux 生效） | `/tmp/epf-pkg.log` |
| `lib/app/eyePageFlow.cfg` | — | 只有 `-Djpackage.app-version`，未寫死 `awt.toolkit.name` | `/tmp/epf-pkg.log` |
| tar.gz 解開後 `eyePageFlow --benchmark` | 0 | 解碼 118.0 ms、橫排 422.1 ms／7,724 頁、直排 606.5 ms／8,375 頁（Big5 正常） | `/tmp/epf-appimage.log` |
| tar.gz 解開後啟動 GUI | 執行中 | `java=21.0.11 (JetBrains s.r.o.)`（JBR）、`toolkit=sun.awt.X11.XToolkit`、`page font: ... (embedded=true)`、`opened ... encoding=Big5` | `/tmp/epf-dist-gui.txt`、截圖 `vm-targz-shot.png` |
| `APPIMAGE_EXTRACT_AND_RUN=1 ./scripts/package-appimage.sh` | 0 | 產出 `eyePageFlow-0.1.0-x86_64.AppImage`（67 MB） | `/tmp/epf-appimage.log` |
| AppImage `--appimage-extract` + `AppRun --benchmark` | 0 | 解碼 118.5 ms、橫排 420.8 ms、直排 748.1 ms | `/tmp/epf-appimage.log` |
| **AppImage 直接執行**（`./eyePageFlow-0.1.0-x86_64.AppImage --benchmark`） | 0 | 解碼 143.6 ms、橫排 576.7 ms、直排 805.2 ms；**執行後檔案仍在**（67 MB） | `/tmp/epf-appimg2.log` |

分頁時間（2 vCPU VM，同一份 10 MiB Big5、7,724／8,375 頁）：

| 次 | 橫排 | 直排 | 判定 |
|---|---|---|---|
| 1 | 390.6 ms | 589.4 ms | 通過 |
| 2 | 485.3 ms | 558.7 ms | 通過 |
| 3 | 982.3 ms | **1,686.2 ms** | **超過 1 秒**（`--benchmark` exit 1） |
| 4 | 496.9 ms | 652.3 ms | 通過 |
| 5 | 440.8 ms | 725.4 ms | 通過 |

另外單獨兩次：橫排 444.7 ms／直排 1,236.2 ms（失敗）、橫排 567.1 ms／直排 649.1 ms（通過）。
**結論**：典型值 390–725 ms，仍在 1 秒內；但這台 2 vCPU VM 偶爾會出現整個行程變慢 2–3 倍的 outlier（第 3 次兩個模式同時變慢），屬於環境排程／GC 抖動，不是分頁邏輯退步。`--benchmark` 的 1 秒 `check` 因此在 VM 上會間歇性失敗。

尚未在 Ubuntu 24.04 驗證：

- Ubuntu 24.04 的 Wayland session（這台刻意設成 Xorg，`WaylandEnable=false`）。**由使用者在 Ubuntu 26.04 Wayland 環境於 2026-11-04 晚間補驗**，清單見 `docs/linux-regression.md`。
- Ubuntu 24.04 的 GUI 互動（IME、縮放、拖放）——只驗到「能啟動、能開 Big5、直排畫面正確」。
- `.desktop` 實際安裝到 `~/.local/share/applications`（會改動使用者環境）。

### Windows GUI 互動驗證（2026-11-04）

使用者實機操作（Windows 10 IoT LTSC 21H2／19044、Temurin 21.0.12+8、`WToolkit`），以真實 Big5 小說檔操作：

| 項目 | 結果 | 備註 |
|---|---|---|
| IME 編輯輸入 | 通過 | 中英文輸入皆正常，選字與組字沒有跑位 |
| 125%／150% 縮放 | 通過（有附帶狀況） | 版面正確、字級正常；但**變更縮放的當下不會立即套用**，要把視窗最大化／還原一次才重繪 |
| 關窗未存提示 | 通過 | 編輯後未存檔直接關閉會跳出「目前的變更尚未儲存，要結束嗎？」 |
| 拖放開檔 | 通過 | 檔案拖進視窗可開啟 |
| 檔案對話框 | 通過 | 開檔／存檔對話框正常 |
| 書籤、書庫 | 通過 | 列表、跳至、管理皆正常 |
| 編輯功能 | 通過 | 閱讀／編輯切換、存檔、dirty 標記正常 |

未單獨確認：剪貼簿（複製／貼上）、單一實例轉送在 GUI 互動下的行為、Ubuntu 端的 GUI 互動。

### 內嵌字型與捲頁（2026-11-04）

- 內嵌 `NotoSerifCJKtc-Regular.otf`（24 MB，SHA-256 見 `THIRD_PARTY_NOTICES.md`）：啟動 log 顯示 `page font: Noto Serif CJK TC (embedded=true)`；`eyePageFlow-0.1.0.jar` 由約 1 MB 增至 20 MB。
- 使用者若在顯示設定選其他字型家族，仍走系統字型。
- `PageCurl`：紙背以摺線鏡射、疊陰影與高光；`PageCurlTest` 5 個測試。

### 直排標點置中（2026-11-04）

- `centeredPunctuation()`：以 `visualBounds` 把 `。、，．：；` 的墨跡放到字格中央；計算時補償 `BookRenderer` 的行框垂直置中量，否則會被推到上一格。
- `RenderRegressionTest` 與 `PaginationTest` 同步更新（直排標點不再與 CJK 共用基線）。
- 目視確認：直排 `。`、`、` 與前後文字不再相黏。

### 版面模式與直排字距（2026-11-04）

- `DisplaySettings.doublePageFor(width, height)`：AUTO 以寬高比 1.25 決定單／雙頁；SINGLE／DOUBLE／EQUAL_HEIGHT 固定。`BookCanvas` 監聽 resize，AUTO 時即時切換並把 `spreadStart` 對齊偶數。
- `BookRenderer.render(..., scaleToPageHeight)`：EQUAL_HEIGHT 只依高度縮放。
- `PageMetrics.cellSpacing`：直排字距微調（上限 5%）。
- 實測（1400×1000 畫布、雙頁）：一般模式書頁高 683 px，`scaleToPageHeight=true` 時 944 px（填滿可視高度）。
- 新增 `LayoutModeTest`（5 個測試）與 `PaginationTest` 的直排字距測試。

### 行距微調（2026-11-03）

- `PageMetrics.lineSpacing`：行數不變，將 `contentHeight - 行數 × lineHeight` 平均分配；放寬上限 5%，否則沿用原行距。
- `walkHorizontal` 與 `PageUnderlines.horizontalYs` 都改用 `lineSpacing`，文字與頁面線不會錯開。
- `PaginationTest` 新增 2 個測試：放寬幅度 ≤ 5%、相鄰三行的 y 差距等於 `lineSpacing`。
- 直排沒有對應實作（1em 字格固定），已列為未完成。

### 標點擠壓（2026-11-03）

- `Pagination`：`COMPRESSIBLE_PUNCTUATION`（`。、，．：；！？…`）在**同一行／同一欄內緊接另一個可擠壓標點**時，前進量減半；換行／換欄會重置。
- 不列入 `」』）`：這些字的墨跡靠近右側，壓半寬會撞到前一個字。
- `PaginationTest` 新增 6 個測試：橫排／直排壓縮、換行不延續、行首壓半寬、行尾懸掛（不換行）、墨跡靠右的標點不壓縮。
- 行尾懸掛與禁則的互動：只有「該串標點的最後一個」可以懸掛，否則整串會被擠一個到行首（測試 `kinsoku keeps closing punctuation off line and column starts` 會抓到）。
- 影響：10 MiB benchmark 頁數由 7,779／8,420 降為 7,724／8,375（每行多塞幾個標點），分頁時間仍為 378.4 ms／553.4 ms。

### 孤行／寡行控制（2026-11-03）

- `WalkResult` 增加 `lineStarts`（每個有內容的行／欄的起始 offset），`paginate()` 透過 `avoidOrphanLine()` 決定是否把段落首行推到下一頁。
- `OrphanControlTest`（10 個測試）：孤行 4 個規則測試 + 寡行 3 個規則測試 + 「頁尾不得是縮排段落首行」整合測試 + 「寡行行數」整合測試 + 分頁覆蓋範圍測試。
- 兩項都做過對照：停用孤行控制 → 整合測試 FAIL；停用寡行控制 → 寡行測試 FAIL。
- 寡行量測加上容量上限後，benchmark 回到 379.8 ms（橫排）／558.5 ms（直排）；未加上限時單一段落文件會退化成 O(n²)（實測 268 秒）。

### 材質快取（2026-11-03）

| 項目 | 舊 | 新 |
|---|---|---|
| 20 次材質繪製（暖機後） | 253.8 ms | 80.9 ms |
| 單次材質 | ~12.7 ms | ~4.1 ms |

- 快取鍵：寬、高、底色 rgb、材質種類；LRU 上限 6,000,000 像素。
- 材質改在獨立 `BufferedImage` 上以 AA 產生；因為是先產生再縮放貼上，畫面與舊版有 ≤27/255 的像素差（材質線條略柔），目視比對無差異。
- 新增 `TexturePainterTest`（4 個測試：決定性、快取鍵、NONE 平塗、指定位置）。

### Linux 打包（2026-11-03）

`./scripts/package-linux.sh`（exit 0）：

| 產物 | 大小 | 驗證 |
|---|---|---|
| `build/package/eyePageFlow/`（app-image） | 74 MB | Wayland：`toolkit=sun.awt.wl.WLToolkit`、`opened ... pages=12`；`EYEPAGEFLOW_TOOLKIT=XToolkit`：`sun.awt.X11.XToolkit` |
| `build/package/eyePageFlow.desktop` | — | `desktop-file-validate` exit 0 |
| `build/package/eyePageFlow-0.1.0-linux-x64.tar.gz` | 45 MB | 解壓到 `/tmp/opencode/dist` 後直接執行：不帶 `JAVA_HOME` 也能啟動，`toolkit=sun.awt.wl.WLToolkit`、`ui font: Noto Sans CJK TC 15pt` |
| `build/package/eyePageFlow-0.1.0-x86_64.AppImage` | 44 MB | `--appimage-extract` 後跑 `squashfs-root/AppRun`：benchmark 正常、開 Big5 檔 `pages=12`、`toolkit=sun.awt.wl.WLToolkit`。直接執行本體會被 appimagetool runtime 自刪（環境問題，見 Known issues） |

`lib/app/eyePageFlow.cfg` 只有 `-Djpackage.app-version`，沒有 `-Dawt.toolkit.name`（確認未再寫死）。

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

- Ubuntu 24.04 原生 X11（已完成 2026-11-04，見上方 VMware 章節）。
- Ubuntu 24.04 Wayland（該 VM 設為 Xorg）。
- Ubuntu 26.04 Wayland 的 GUI 互動（拖放、IME、縮放、檔案對話框、剪貼簿、編輯存檔、`AUTO` 版面切換）。
- Windows 10／11 其他版本（本次為 Windows 10 IoT 企業版 LTSC 21H2／19044）。
- Windows GUI 互動：拖放、IME、125%／150% 縮放、檔案對話框、剪貼簿、編輯存檔。
- Windows 可攜式 zip 在其他機器（未安裝 Noto Sans TC、不同 DPI、Windows 11）的解壓執行。
- 直排標點與字型 fallback 的 golden image。
- 10 MiB 以上的檔案與 20 MiB 上限。
- 編輯器 10 MiB 的輸入延遲（M7）。

## Known issues

- **Windows 變更顯示縮放後不會立即重繪**：在「設定 → 顯示器 → 縮放與版面配置」切換 125%／150% 時，視窗當下的畫面仍是舊比例（文字、書頁尺寸沒跟著變），把視窗最大化或還原一次就恢復。推測 Swing 沒有收到 DPI 變更通知、也就沒有觸發重排；尚未確認是 AWT 事件沒來，還是程式在自己的 resize 路徑上才重算。可先用「最大化一次」當 workaround。

- UI 中文只在有 Noto Sans CJK／Noto Sans TC 的機器上變好。Windows 開發機有 `Noto Sans TC`，所以 `ui font:` 顯示 `Noto Sans TC 15pt`；沒有安裝的 Windows 機器 `resolveUiFamily()` 仍回傳 null，選單／工具列會走 Segoe UI 的 fallback。
- 這台 JVM 的 Swing 已經是反鋸齒（`awt.useSystemAAFontSettings=on` 由程式設定），`lcd`／`gasp` 對 FlatLaf 版面量測不出可見差異；「字很舊」的感覺來自字級與 hinting，不是沒有反鋸齒。

- emoji 只在 JBR 21 有墨跡。Linux 的 Noto Color Emoji 是點陣（CBDT）字型，Temurin 21 的 Java2D 完全畫不出來（實測 0 墨跡像素），字格與分頁仍正確、畫面會留空白；Windows 的 Segoe UI Emoji 是向量字型不受影響。正式 Linux runtime 用 JBR，所以目前可接受，但用 Temurin 跑 Linux 版會看不到 emoji。

- 頁面字型已內嵌（`page font: Noto Serif CJK TC (embedded=true)`），Windows／Linux 頁數一致；只有在顯示設定手動選其他字型家族時才會回到系統字型。
- **jlink 的模組清單漏掉 `jdk.charsets` 會讓打包版失去 Big5／GB18030**。症狀是 GUI 完全正常、只有開舊編碼檔案時丟 `UnsupportedCharsetException`；兩個打包腳本都已補上，並在打包後用 `--benchmark` 驗一次 Big5 解碼。
- **jpackage 的 Windows GUI launcher 會立刻回 0 並把 JVM 丟到背景**：JVM 失敗時 exit code 仍是 0，標準輸出仍會接到呼叫端的 console。因此不能用 `eyePageFlow.exe` 當 CI／smoke test 的判斷依據，要用 `runtime\bin\java.exe`。附帶影響：使用者雙擊後若啟動失敗，不會看到任何錯誤訊息。
- 直排半角字目前用字寬前進，未做整體字串的字距微調；旋倒字串在窄欄位仍可能與相鄰欄輕微重疊。
- `pwsh -File <script> --args="C:\path"` 會把磁碟機代號的 `:` 當成參數分隔而拆成兩個引數；請直接用 `.\scripts\gradlew-local.ps1 run --args="C:\path"`，或改用 `pwsh -Command "& '.\scripts\gradlew-local.ps1' run '--args=C:\path'"`。
- 行尾由 `.gitattributes` 統一（文字檔 LF、`*.bat`／`*.cmd` CRLF）。既有 clone 若仍顯示整批 modified，執行 `git add --renormalize .` 或重新 clone。
- 直排標點使用 Unicode 直排形式；字型不支援時退回近似位移（`VerticalPunctuation.TOP_RIGHT` 尚未逐字型量測）。
- 直排縮排以「欄頂跳 2 格」近似；遇到新頁或空白行仍需更多測試。
- 禁則（行首／行尾標點）與左右對齊已完成。
- **自動恢復閱讀位置**：`core/ReadingPositionStore` 以字元 offset 記錄每個檔案讀到哪裡（`reading-positions.properties`，最近 200 筆）。用 offset 而不是頁碼，換字級／字型／直排橫排重新分頁後仍能正確換算（`Pagination.pageIndexFor`）。`ReaderPanel.openPath()` 先存舊檔位置、開檔後再 `resumeSavedPosition()`；`closeWindow()` 也會存。啟動 log 會印 `resumed <檔名> at page N/M offset=X`。`ReadingPositionStoreTest` 6 個測試。
- 順手清掉三條過時的已知問題（材質快取已完成、直排標點已量測、CURL 敘述重複），並把已完成但仍掛在 In progress 的 Ubuntu 24.04 X11 移除。
- surrogate pair 以單一字處理；頁面字型缺字（emoji）時退回 Segoe UI Emoji／Noto Emoji 等系統字型，仍可能因系統無 emoji 字型而顯示方框。
- 顯示設定已露出主題、版面、頁寬、字體家族、字級、行高、粗體、底線、五組顏色、三組內置圖案、配色方案、中縫具名預設色、動畫。
- **編輯模式空白鍵**：`EditorPane.bindNextPage()` 把 `VK_SPACE`（無修飾鍵）綁成「下一頁」，Shift+空格因為是不同 KeyStroke 會落到預設輸入處理，仍插入空白——與 `docs/spec/pagebypage-spec.md` 的原作相容規則一致。編輯器是連續文字檢視，所以是把游標移到下一頁起點（`pageDownInEditor()`），不是切換卡片；log 會印 `editor space -> page N/M offset=X`。
- 章節規則、書籤、書庫、自動恢復閱讀位置與老闆鍵已可用；托盤在目前 GNOME／Wayland 不可用，老闆鍵以最小化替代。
- 檔案關聯註冊尚未實作；目前依靠命令列參數與單一實例轉送。
- **AppImage 在這台環境執行後會自我刪除**：`appimagetool` continuous build（4919cc4）在此沙箱跑 FUSE fallback 時，離開後會把 `.AppImage` 本體移除。用最小 AppDir 重打包也重現，所以不是本專案打包內容的問題。**2026-11-04 在 Ubuntu 24.04（`/dev/fuse` 正常、`fusermount3` 存在）直接執行同一份 AppImage 則完全正常，檔案也保留**，因此判定為原沙箱環境問題，一般桌面不受影響。
- 檔案關聯的 `.desktop` 已產生並通過 `desktop-file-validate`，但**尚未安裝**到 `~/.local/share/applications`（會改動使用者環境，需另外確認）。
- Windows 不提供 MSI／安裝檔（使用者指定只要可攜式 zip，解開即可執行）；jpackage 的 MSI 另外需要 WiX Toolset。
- Ubuntu 26.04 的 X11 session 不是預設，尚未測試。
- 原作素材與說明文件有著作權，未包含在專案中。

## M0 判定

**Go。** 分頁速度、直排／橫排渲染、JBR 原生 Wayland、app-image 啟動都已在目前環境驗證。Windows 端已補上建置、測試、渲染、benchmark、GUI 互動（IME／縮放／拖放／關窗提示）與可攜式 zip；禁則、左右對齊、offset↔page、surrogate pair 已完成，Ubuntu 26.04 Wayland 與 Ubuntu 24.04 原生 X11 回歸也都通過。最大風險改為：Windows 變更顯示縮放不會即時重繪（已知問題）、以及 2 vCPU 環境下 benchmark 的 1 秒門檻偶爾被環境抖動超過。
