package com.qingfeng.files.data

/**
 * 归档格式。
 *
 * 借鉴点：实测参照物代码里的格式常量 —— "zip" / "7z" / "rar" / "tar"。
 * 我们只有 zip（JDK 原生支持）；7z/rar 需要额外库，且 rar 只能解压不能压。
 */
enum class ArchiveFormat(
    val label: String,
    val ext: String,
    val canCompress: Boolean,
    val canExtract: Boolean
) {
    ZIP("ZIP", "zip", true, true),
    TAR("TAR", "tar", true, true)
}
