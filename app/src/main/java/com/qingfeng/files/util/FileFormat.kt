package com.qingfeng.files.util

import android.webkit.MimeTypeMap
import com.qingfeng.files.data.Category
import com.qingfeng.files.data.FileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileFormat {

    /** 用 emoji 当图标，避免引入图标资源 */
    fun icon(item: FileItem): String {
        if (item.isDirectory) return "📁"
        val ext = item.extension.lowercase()
        return when {
            ext in Category.IMAGE.exts -> "🖼"
            ext in Category.VIDEO.exts -> "🎬"
            ext in Category.AUDIO.exts -> "🎵"
            ext in Category.DOC.exts -> "📄"
            ext in Category.ARCHIVE.exts -> "🗜"
            ext in Category.APK.exts -> "📦"
            else -> "📄"
        }
    }

    fun size(item: FileItem): String =
        if (item.isDirectory) "文件夹" else size(item.size)

    fun size(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digit = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
            .coerceAtMost(units.size - 1)
        return String.format(
            Locale.getDefault(),
            "%.1f %s",
            bytes / Math.pow(1024.0, digit.toDouble()),
            units[digit]
        )
    }

    fun time(item: FileItem): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(item.lastModified))

    fun mime(name: String): String? {
        val ext = name.substringAfterLast('.', "")
        if (ext.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
    }
}
