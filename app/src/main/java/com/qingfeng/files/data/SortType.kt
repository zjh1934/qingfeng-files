package com.qingfeng.files.data

/**
 * 排序字段。
 *
 * 借鉴点：参照物的排序是 4 种 —— Name / Date / Size / Type
 * （string 反查：ajy=Name, ajx=Date, ajz=Size, ak0=Type）。
 */
enum class SortField(val label: String) {
    NAME("名称"),
    DATE("日期"),
    SIZE("大小"),
    TYPE("类型")
}

/** 排序方向 */
enum class SortDir(val label: String) { ASC("升序"), DESC("降序") }

/**
 * 完整排序状态：字段 + 方向。
 *
 * 借鉴点：参照物的每个排序项都带一个方向箭头（布局 gg.xml 的 select_arrow），
 * 点文字排序、点箭头切方向。
 */
data class SortState(
    val field: SortField = SortField.NAME,
    val dir: SortDir = SortDir.ASC
) {
    fun toggled(): SortState = copy(dir = if (dir == SortDir.ASC) SortDir.DESC else SortDir.ASC)
    fun label(): String = "${field.label} · ${dir.label}"
}

fun List<FileItem>.sortedBy(s: SortState): List<FileItem> {
    val dirs = filter { it.isDirectory }
    val files = filter { !it.isDirectory }
    val asc = s.dir == SortDir.ASC

    fun <R : Comparable<R>> List<FileItem>.by(f: (FileItem) -> R): List<FileItem> =
        if (asc) sortedBy(f) else sortedByDescending(f)

    // 目录永远排在前
    val sd = when (s.field) {
        SortField.NAME -> dirs.by { it.name.lowercase() }
        SortField.DATE -> dirs.by { it.lastModified }
        SortField.SIZE -> dirs.by { it.childCount }
        SortField.TYPE -> dirs.sortedBy { it.name.lowercase() }
    }
    val sf = when (s.field) {
        SortField.NAME -> files.by { it.name.lowercase() }
        SortField.DATE -> files.by { it.lastModified }
        SortField.SIZE -> files.by { it.size }
        SortField.TYPE -> if (asc) files.sortedWith(
            compareBy<FileItem> { it.extension }.thenBy { it.name.lowercase() }
        ) else files.sortedWith(
            compareByDescending<FileItem> { it.extension }.thenByDescending { it.name.lowercase() }
        )
    }
    return sd + sf
}
