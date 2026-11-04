# Linux 回歸清單

這份清單給 Ubuntu 開發機（`Ubuntu 26.04 / Wayland`）使用。基準 commit：`e168686`（2026-11-04）。

> Ubuntu 24.04 原生 X11 已於 2026-11-04 在 VMware VM 上完成（見 `handoff.md`）。
> 該 VM 固定 `WaylandEnable=false`，所以 24.04 的 Wayland session 仍未驗證。

## 0. 取得程式

```bash
git clone https://github.com/sawaichi9527/eyePageFlow.git
cd eyePageFlow
git log -1 --oneline          # 應為 e168686 或之後
```

## 1. 工具鏈

```bash
./scripts/bootstrap-jdk.sh    # Temurin 21.0.12+8 → .local/jdk-21
./scripts/bootstrap-jbr.sh    # 只有要跑原生 Wayland/X11 需要（JBR 21）
./scripts/gradlew-local.sh --version
```

## 2. 建置與測試

```bash
./scripts/gradlew-local.sh test --rerun-tasks
./scripts/gradlew-local.sh check
```

**預期：125 tests、29 suites、0 failures、0 errors、0 skipped**（2026-11-04 在 Windows 與 Ubuntu 24.04 X11 都是這個數字）。若有失敗，請把失敗的測試名稱貼回來。

## 3. 效能基準

```bash
./scripts/gradlew-local.sh run --args="--benchmark"
```

**預期**：10 MiB Big5、5,934,667 字，解碼與橫排／直排分頁都 **< 1 秒**。
頁面字型已內嵌（`Noto Serif CJK TC`），所以頁數應與 Windows **完全相同**：橫排 **7,724**、直排 **8,375**。

注意：`--benchmark` 內建 1 秒斷言，超過會 `exit 1`。若在低規格機器／VM 上偶發超標，
先重跑一次確認是否為環境抖動（2 vCPU VM 實測偶有 1.2–1.7 秒的 outlier，重跑即恢復 400–700 ms）。

## 4. 渲染快照

```bash
./scripts/gradlew-local.sh run --args="--render build/prototype"
```

**預期頁數**：橫排 11 頁、直排 12 頁、3,761 字（與 Windows 相同）。
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

啟動 log 會印 `startup typography: fontSize=26.0 font=<家族>/26` 與
`display settings: ... underlineWidth=... punctuation=...`，請一起回報。

| 項目 | 檢查方式 |
|---|---|
| 排版比例 | 直排 20 欄／頁左右、欄距 1.5em（與 Windows 對照） |
| 禁則 | 橫排：每行開頭不應是 `。，、！？」』` |
| 左右對齊 | 橫排：換行的行尾應貼齊內容區右緣 |
| 直排標點 | `。，` 在字格內（顯示設定「置中」）；切「右上」應靠右上 |
| emoji | 連續 emoji 各佔一格、不重疊（需要系統有 Noto Emoji 或 Noto Color Emoji） |
| 下劃線 | 改顏色只影響線、不影響文字；粗細 淡/普通/粗黑 正常 |
| **翻頁方向（2026-11-04 新增）** | 顯示設定把「動畫」設成 **捲頁**：橫排「下一頁」摺線由右往左；**直排「下一頁」摺線由左往右**（右翻書），「上一頁」反向 |
| **直排雙頁半頁掃描** | 直排雙頁時摺線只掃**左半頁**、不跨越中縫；橫排雙頁仍掃整個跨頁 |
| **CURL 紙背** | 摺線旁應看得到鏡射的紙背與漸暗陰影，不是只有一條線 |
| **顯示預設值** | 刪掉 `~/.config/eyePageFlow/settings.properties` 後啟動：字級 26、直排標點置中、下劃線粗細「淡（0.5）」 |
| **自動恢復閱讀位置（2026-11-04 新增）** | 開檔翻到第 5 頁、正常關窗（Alt+F4）、重開同一檔：應回到第 5 頁，log 有 `resumed <檔名> at page 5/N offset=...` |
| **編輯模式空白鍵（2026-11-04 新增）** | `--editor` 進編輯模式：按空白鍵應捲到下一頁起點（log `editor space -> page N/M`）；**Shift+空白仍要打出空白**，不能換頁 |

## 7. 打包（2026-11-04 新增）

```bash
./scripts/bootstrap-jbr.sh      # 需要 JBR SDK 才能 jpackage
./scripts/package-linux.sh
```

**預期**：app-image、`eyePageFlow.desktop`、`eyePageFlow-0.1.0-linux-x64.tar.gz` 都產生，
且腳本結尾的「驗證 app-image（Big5 解碼）」不會失敗
（這一步是在抓 `jlink` 忘了 `jdk.charsets` 會讓打包版失去 Big5 的回歸）。

```bash
build/package/eyePageFlow/runtime/bin/java --list-modules | grep jdk.charsets   # 必須有
grep awt.toolkit build/package/eyePageFlow/lib/app/eyePageFlow.cfg              # 應該沒有輸出
```

AppImage（需要 `appimagetool` 放在 `.local/tools/`）：

```bash
APPIMAGE_EXTRACT_AND_RUN=1 ./scripts/package-appimage.sh
```

## 8. 回報格式

```text
commit:
OS / session:
toolkit:
startup typography:
tests: N tests, N failures
benchmark: 解碼 x ms / 橫排 x ms (N 頁) / 直排 x ms (N 頁)
render: 橫排 x 頁 / 直排 x 頁
翻頁動畫: 橫排下一頁=右→左 or 左→右 / 直排下一頁=右→左 or 左→右
異常:
```
