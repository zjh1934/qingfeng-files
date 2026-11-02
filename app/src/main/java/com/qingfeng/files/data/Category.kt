package com.qingfeng.files.data

/**
 * 文件分类。按扩展名归类，用于分类浏览入口。
 */
enum class Category(val label: String, val exts: Set<String>) {
    IMAGE("图片", setOf("jpg", "jpeg", "png", "gif", "webp", "bmp", "heic", "svg")),
    VIDEO("视频", setOf("mp4", "mkv", "avi", "mov", "flv", "webm", "3gp", "m4v")),
    AUDIO("音乐", setOf("mp3", "wav", "flac", "aac", "ogg", "m4a", "opus")),
    DOC("文档", setOf("pdf", "doc", "docx", "txt", "md", "xls", "xlsx", "ppt", "pptx", "epub")),
    ARCHIVE("压缩包", setOf("zip", "rar", "7z", "tar", "gz", "bz2")),
    APK("安装包", setOf("apk", "apks", "xapk"))
}

fun FileItem.category(): Category? =
    if (isDirectory) null else Category.values().firstOrNull { extension in it.exts }


/**
 * 搜索范围。
 *
 * 借鉴点：参照物的搜索溢出菜单有 6 项类型范围 ——
 * Search in App / Document / Music / Image / Video + Advanced search。
 * 不是只有"输个关键字"。
 */
enum class SearchScope(val label: String, val exts: Set<String>?) {
    ALL("全部文件", null),
    IMAGE("图片", Category.IMAGE.exts),
    VIDEO("视频", Category.VIDEO.exts),
    AUDIO("音乐", Category.AUDIO.exts),
    DOC("文档", Category.DOC.exts),
    ARCHIVE("压缩包", Category.ARCHIVE.exts),
    APK("安装包", Category.APK.exts)
}
