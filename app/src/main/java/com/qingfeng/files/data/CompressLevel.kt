package com.qingfeng.files.data

/**
 * 压缩级别。
 *
 * 借鉴点：实测参照物 `res/values/arrays.xml` 的 `<string-array name="u">`：
 *   **Store / Fast / Standard / Best**（4 档）
 * 对应 ZIP Deflater 级别：0 / 1 / 6 / 9
 *
 * 文案依据：`ii`="Compress Level"、`uf`="Compress level:"
 */
enum class CompressLevel(
    val label: String,
    val deflaterLevel: Int,
    val desc: String
) {
    STORE("Store", 0, "不压缩，最快，文件最大"),
    FAST("Fast", 1, "快速压缩"),
    STANDARD("Standard", 6, "均衡（默认）"),
    BEST("Best", 9, "最高压缩率，最慢")
}
