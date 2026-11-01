package com.qingfeng.files.util

import com.qingfeng.files.data.FileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 文件展示用的格式化工具。
 */
object FileFormat {

    fun size(item: FileItem): String = when {
        item.isDirectory -> if (item.childCount >= 0) "${item.childCount} 项" else "文件夹"
        item.size < 1024 -> "${item.size} B"
        item.size < 1024 * 1024 -> "%.1f KB".format(item.size / 1024.0)
        item.size < 1024 * 1024 * 1024 -> "%.1f MB".format(item.size / (1024.0 * 1024.0))
        else -> "%.2f GB".format(item.size / (1024.0 * 1024.0 * 1024.0))
    }

    fun time(item: FileItem): String {
        if (item.lastModified <= 0L) return ""
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(item.lastModified))
    }

    /** 用 Unicode 符号做图标，避免引入一堆矢量资源 */
    fun icon(item: FileItem): String = when {
        item.isDirectory -> "\uD83D\uDCC1"
        else -> when (item.extension) {
            "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic" -> "\uD83D\uDDBC️"
            "mp4", "mkv", "avi", "mov", "flv", "webm" -> "\uD83C\uDFA5"
            "mp3", "wav", "flac", "aac", "ogg", "m4a" -> "\uD83C\uDFB5"
            "apk" -> "\uD83D\uDCE6"
            "zip", "rar", "7z", "tar", "gz" -> "\uD83D\uDDDC️"
            "pdf" -> "\uD83D\uDCC4"
            "doc", "docx" -> "\uD83D\uDCC4"
            "xls", "xlsx" -> "\uD83D\uDCCA"
            "ppt", "pptx" -> "\uD83D\uDCCA"
            "txt", "md", "log" -> "\uD83D\uDCDD"
            else -> "\uD83D\uDCCE"
        }
    }
}
