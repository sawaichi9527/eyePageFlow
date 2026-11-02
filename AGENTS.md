# AGENTS.md

## OpenCode 專案目的

eyePageFlow 是「PageByPage: An application to view & edit plain text file.」的功能復刻版本，以 AI 協助開發，但使用自己的品牌、素材與程式碼。

- 目標平台：Windows 10/11 x64、Ubuntu 24.04 LTS x64（原生 X11）、Ubuntu 26.04 LTS x64（原生 Wayland）。
- 技術棧：Kotlin/JVM 21、Swing + FlatLaf、Java2D 自繪閱讀器、RSyntaxTextArea 編輯器。
- Linux runtime：JetBrains Runtime 21（`WLToolkit` 原生 Wayland、`XToolkit` 原生 X11）。
- Windows runtime：Eclipse Temurin 21 x64。
- 成功條件：在三個平台都能開啟 10 MiB Big5 純文字檔、1 秒內完成分頁，並以直排／橫排閱讀、編輯、保持原編碼存檔。細節見 `docs/spec/`。

## OpenCode 工作規則

- 變更專案檔案前，必須先讀取本文件與 `handoff.md`。
- 變更應小、可審閱，且與本次需求直接相關。
- 破壞性或不可逆操作前，先顯示預計的檔案變更。
- 不得儲存密碼、API key、token、私鑰或任何內部憑證。
- `git push`、部署、刪除、硬體控制、壓力測試等高影響操作前必須先詢問。

## 程式碼變更基準

- 先理解受影響的程式路徑再修改；不要只修症狀而忽略共同成因。
- 明確說明會影響實作或驗證結果的假設。
- 需求、安全性、測試目標或平台行為有歧義時，先詢問。
- 優先重用專案既有程式碼，再考慮新增 helper 或抽象層。
- 優先使用標準函式庫、平台原生能力與已安裝的相依套件。
- 以能滿足實際需求的最小變更為原則。
- 不順手重構無關的程式碼、註解、命名或格式。
- 變更程式碼後必須定義並執行對應的驗證。
- 不得為了縮減程式碼而移除必要的測試覆蓋、日誌、回報、安全檢查或可追溯性。

## 驗證

```bash
./scripts/gradlew-local.sh test
./scripts/gradlew-local.sh check
./scripts/gradlew-local.sh run --args="--render build/prototype"
./scripts/gradlew-local.sh run --args="--benchmark"
```

Windows（pwsh7）：

```powershell
.\scripts\bootstrap-jdk.ps1
.\scripts\gradlew-local.ps1 test
.\scripts\gradlew-local.ps1 check
.\scripts\gradlew-local.ps1 run --args="--render build/prototype"
.\scripts\gradlew-local.ps1 run --args="--benchmark"
```

- 本機 Windows 開發一律用 `pwsh`（opencode-extension-packs 的 pwsh7），不要用內建 `powershell.exe` 5.1。
- Windows 只用 `.local\jdk-21`（Temurin 21）與 `.local\gradle-home`；不需要 `bootstrap-jbr.sh`（那僅供 Linux 原生 Wayland／X11）。

原生 Wayland 啟動（Linux）：

```bash
./scripts/gradlew-local.sh installDist
JAVA_HOME="$PWD/.local/jbr-21" \
  JAVA_OPTS="-Dawt.toolkit.name=WLToolkit" \
  build/install/eyePageFlow/bin/eyePageFlow
```

X11／XWayland 啟動：

```bash
JAVA_HOME="$PWD/.local/jbr-21" \
  JAVA_OPTS="-Dawt.toolkit.name=XToolkit" \
  build/install/eyePageFlow/bin/eyePageFlow
```

打包驗證（M0 已可執行）：

```bash
.local/jbrsdk-21/bin/jpackage --type app-image ...
build/package/eyePageFlow/bin/eyePageFlow
```

- 沒有當次變更的新鮮驗證證據，不得宣稱工作「完成」、「已修好」或「通過」。
- 驗證紀錄需包含：實際指令、exit code、通過／失敗／略過數量或觀察結果、執行環境、相關日誌或報告路徑，以及尚未驗證的範圍。
- 診斷失敗時需讀取完整的錯誤輸出，不可只看最後一行或沿用先前的執行結果。
- 跨平台功能必須在 Windows、Ubuntu 24.04 X11、Ubuntu 26.04 Wayland 驗證；若只驗證其中一平台，必須明確標示其他平台為未驗證。
- 若啟動 log 顯示 `sun.awt.X11.XToolkit`，不可宣稱已在原生 Wayland 執行；原生 Wayland 必須是 `sun.awt.wl.WLToolkit`。
- 破壞性、壓力、長時間、影響硬體或接近正式環境的驗證，執行前必須詢問；未執行時要明確說明，不可假設成功。

## 專案特定限制

- 只支援 64-bit（x86_64／amd64）；不建立 32-bit 版本、不引入 32-bit native library。
- Linux 只用 JBR 21；Windows 只用 Temurin 21。不要把 Temurin 當成原生 Wayland 方案。
- `core` 不得 import `javax.swing`；UI、renderer、platform 分層保持清楚。AWT 的 `Font`／`FontMetrics` 可用於量測。
- 純文字檔處理必須明確處理：Big5、GB18030、UTF-8、UTF-16 LE/BE、BOM、CRLF／LF。
- Legacy 編碼以 Big5 為主；無法表示的字每次詢問，預設另存 UTF-8，不靜默轉換。
- 預設字型 Noto Serif CJK TC（OFL）；允許系統字型，但換字型要重新分頁。
- 不直接複製原作的 logo、內置圖案、工具列圖示、說明文件文字；素材自製或使用 CC0／OFL，並記錄授權。
- `docs/reference/PageByPage.htm` 只作本機參考；公開 repo 前要確認授權，建議改寫成自己的規格摘要。
- 測試文字不可使用截圖中的小說內容；使用自製或公共領域文字。
- Wayland 不允許任意全域熱鍵；老闆鍵以聚焦時 Esc 實作，托盤還原方式待驗證。
- 尚未確認的 PageByPage 行為不得當成既定需求寫入程式；先記錄為待確認項目。
- 除非使用者明確要求，不建立 `CLAUDE.md`、Cursor Rules、Codex Plugin、Hook 或其他 Agent 專用設定。
