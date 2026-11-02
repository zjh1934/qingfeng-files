package com.qingfeng.files.data

/**
 * 筛选条件。
 *
 * 借鉴点：参照物的分类不是"扫盘出结果页"，而是**在已有列表上收窄**的筛选器，
 * 五个维度：类型 / 大小 / 时间 / 创建应用 / 附加（隐藏·媒体·系统）。
 * 我们做前四个里最实用的：类型、大小区间、时间区间、是否显示隐藏文件。
 *
 * 全部条件为"并"关系；为空表示不限。
 */
data class Filter(
    /** 限定这些分类，空 = 不限 */
    val categories: Set<Category> = emptySet(),
    /** 大小区间（字节），null 表示不限 */
    val sizeFrom: Long? = null,
    val sizeTo: Long? = null,
    /** 时间区间（毫秒），null 表示不限 */
    val timeFrom: Long? = null,
    val timeTo: Long? = null,
    /** 是否显示隐藏文件（以 . 开头），默认不显示 */
    val showHidden: Boolean = false,
    /** 文件名关键字（在当前列表内过滤；跨目录搜索走 FileSource.search） */
    val keyword: String = ""
) {
    fun isEmpty(): Boolean =
        categories.isEmpty() && sizeFrom == null && sizeTo == null &&
                timeFrom == null && timeTo == null && !showHidden && keyword.isBlank()

    fun match(item: FileItem): Boolean {
        // 隐藏文件
        if (!showHidden && item.name.startsWith(".")) return false

        // 关键字
        if (keyword.isNotBlank() && !item.name.contains(keyword, ignoreCase = true)) return false

        // 目录不受类型/大小/时间筛选影响，始终保留
        if (item.isDirectory) return true

        // 类型
        if (categories.isNotEmpty()) {
            val c = item.category() ?: return false
            if (c !in categories) return false
        }

        // 大小区间
        if (sizeFrom != null && item.size < sizeFrom) return false
        if (sizeTo != null && item.size > sizeTo) return false

        // 时间区间
        if (timeFrom != null && item.lastModified < timeFrom) return false
        if (timeTo != null && item.lastModified > timeTo) return false

        return true
    }
}
