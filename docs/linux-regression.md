# Linux 回歸清單

這份清單給 Ubuntu 開發機使用。Windows 端最後一次驗證的基準是 commit `e432a87`。

## 0. 取得程式

```bash
git clone git@github.com:sawaichi9527/eyePageFlow.git
cd eyePageFlow
git log -1 --oneline          # 應為 e432a87 之後
```

## 1. 工具鏈

```bash
./scripts/bootstrap-jdk.sh    # Temurin 21.0.12+8 → .local/jdk-21
./scripts/bootstrap-jbr.sh    # 只有要跑原生 Wayland/X11 需要（JBR 21）
./scripts/gradlew-local.sh --version
```

## 2. 建置與測試

```bash
./scripts/gradlew-local.sh test
./scripts/gradlew-local.sh check
```

**預期：67 tests、0 failures**（20 suites）。若有失敗，請把失敗的測試名稱貼回來。

## 3. 效能基準

```bash
./scripts/gradlew-local.sh run --args="--benchmark"
```

**預期**：10 MiB Big5，解碼與橫排／直排分頁都 **< 1 秒**。
頁數會與 Windows 不同（字型字寬不同），Windows 目前是橫排 7,613／直排 8,193。

## 4. 渲染快照

```bash
./scripts/gradlew-local.sh run --args="--render build/prototype"
```

看 `build/prototype/*.png` 是否為正常的中文直排／橫排版面（不是空白、不是方框）。

## 5. 原生 Wayland／X11 啟動

```bash
./scripts/gradlew-local.sh installDist
JAVA_HOME="$PWD/.local/jbr-21" build/install/eyePageFlow/bin/eyePageFlow
```

- toolkit 由程式依 session 自動選擇；啟動 log 必須是 `toolkit=sun.awt.wl.WLToolkit`
  才算原生 Wayland，`session=` 應為 `wayland`。
- X11 session（或 XWayland 測試）要強制指定：`EYEPAGEFLOW_TOOLKIT=XToolkit`，
  log 應為 `sun.awt.X11.XToolkit`。不要再依賴 `-Dawt.toolkit.name`，JBR 會自己塞預設值。

## 6. 這批改動要看的重點

啟動 log 會印 `startup typography: fontSize=26.0 font=<家族>/26`，請把它一起回報。

| 項目 | 檢查方式 |
|---|---|
| 排版比例 | 直排 20 欄／頁左右、欄距 1.5em（與 Windows 對照） |
| 禁則 | 橫排：每行開頭不應是 `。，、！？」』` |
| 左右對齊 | 橫排：換行的行尾應貼齊內容區右緣 |
| 直排標點 | `。，` 在字格內（顯示設定「置中」）；切「右上」應靠右上 |
| emoji | 連續 emoji 各佔一格、不重疊（需要系統有 Noto Emoji 或 Noto Color Emoji） |
| 下劃線 | 改顏色只影響線、不影響文字；粗細 淡/普通/粗黑 正常 |

## 7. 回報格式

```text
commit:
OS / session:
toolkit:
startup typography:
tests: N tests, N failures
benchmark: 解碼 x ms / 橫排 x ms (N 頁) / 直排 x ms (N 頁)
異常:
```
