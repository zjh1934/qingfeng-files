package com.qingfeng.files.data

/**
 * 排序方式。
 */
enum class SortType(val label: String) {
    NAME("按名称"),
    SIZE("按大小"),
    TIME("按时间"),
    TYPE("按类型")
}

fun List<FileItem>.sortedBy(t: SortType): List<FileItem> {
    val dirs = filter { it.isDirectory }
    val files = filter { !it.isDirectory }
    val sortedDirs = when (t) {
        SortType.NAME -> dirs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        SortType.TIME -> dirs.sortedByDescending { it.lastModified }
        else -> dirs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }
    val sortedFiles = when (t) {
        SortType.NAME -> files.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
        SortType.SIZE -> files.sortedByDescending { it.size }
        SortType.TIME -> files.sortedByDescending { it.lastModified }
        SortType.TYPE -> files.sortedWith(
            compareBy<FileItem> { it.extension }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        )
    }
    // 目录永远排在前
    return sortedDirs + sortedFiles
}
