package com.qingfeng.files.data

/**
 * 剪贴板：两步式复制/移动的中间状态。
 *
 * 借鉴点：参照物的多选工具栏里 **Paste 常驻可见**，说明复制/移动是两步式：
 * 先在 A 目录选中 → Cut/Copy，再浏览到 B 目录 → Paste。
 *
 * 好处：可以跨多次浏览找目标目录，不受"只能选当前目录的子文件夹"限制。
 * （我之前做的一步对话框只能选当前目录的子目录，局限性很明显。）
 */
data class Clipboard(
    val items: List<FileItem>,
    val mode: Mode
) {
    enum class Mode(val label: String) { COPY("复制"), CUT("剪切") }
}
