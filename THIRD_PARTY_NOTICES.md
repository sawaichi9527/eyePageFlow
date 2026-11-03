# Third-Party Notices

本專案使用或參考下列第三方元件。正式發佈前需補齊完整授權文字與版本。

## Runtime

### Eclipse Temurin 21

- 使用平台：Windows x64
- 授權：GPLv2 with Classpath Exception
- 來源：https://adoptium.net/

### JetBrains Runtime 21

- 使用平台：Linux x64
- 版本：JBR-21.0.11+10-1163.116
- 授權：GPLv2 with Classpath Exception
- 來源：https://github.com/JetBrains/JetBrainsRuntime
- 打包時需附上授權與源碼取得方式。

## Libraries

| 元件 | 版本 | 授權 |
|---|---|---|
| Kotlin standard library | 2.4.10 | Apache-2.0 |
| FlatLaf | 3.7.2 | Apache-2.0 |
| JUnit Jupiter | 6.1.2 | EPL-2.0 |
| RSyntaxTextArea | 3.6.3 | BSD-3-Clause |

## Fonts

### Noto Serif CJK TC（內嵌）

- 檔案：`src/main/resources/fonts/NotoSerifCJKtc-Regular.otf`（24 MB）
- 授權：SIL Open Font License 1.1，完整條文見 `src/main/resources/fonts/OFL.txt`
- 來源：https://github.com/notofonts/noto-cjk
- SHA-256：`234301038e76e7c35c43113785024700c4e4fe7bdce1d1fbbc42fca7e6683798`
- 用途：預設頁面字型，讓 Windows／Linux 的頁數與字形一致；使用者可在顯示設定改選系統字型。

## Reference

- PageByPage，作者馬健（stronghorse_mj@hotmail.com）。
- 本專案僅參考功能與操作流程；不包含原作的程式碼、圖像、logo 或說明文件。
- `docs/reference/PageByPage.htm` 為本機參考資料，公開 repo 前需確認授權或移除。
