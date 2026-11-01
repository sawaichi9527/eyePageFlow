package io.github.sawaichi9527.eyepageflow

object SampleText {
    val SHORT: String = """
        eyePageFlow 排版原型

        這是一段用來檢查直排與橫排的測試文字。書頁、封面、桌面與中縫應該要正確顯示。
        「引號、書名號《測試》、括號（甲乙）、省略號……與破折號——」都要能正確排列。
        PageByFlow 2026：Latin text 與數字 1234567890 在直排時應該旋轉或保持正立。

        第二段用來測試換行與段落縮排。中文排版需要處理禁則：句號、逗號、問號、驚嘆號不應出現在行首；開引號、開括號不應出現在行尾。
        第三段混合 English words、數字 42、以及全形標點：，。！？；：「」『』《》〈〉（）。

        第四段測試長句換行與頁面邊界。當文字到達頁面底部時，應該自動接到下一頁；直排時由右至左，橫排時由左至右。
        頁碼應該出現在頁面外角；直排時第一頁在右、第二頁在左，橫排時第一頁在左、第二頁在右。
    """.trimIndent()

    fun longText(repeat: Int = 8): String = buildString {
        repeat(repeat) { index ->
            append(SHORT)
            append("\n\n")
            append("第 ${index + 1} 段結束。")
            append("\n\n")
        }
    }
}
