package com.qingfeng.files.util

import android.webkit.MimeTypeMap
import com.qingfeng.files.data.Category
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileFormat {

    /** 用 emoji 当图标，避免引入图标资源 */
    fun icon(item: com.qingfeng.files.data.FileItem): String = when {
        item.isDirectory -> "📁"
        else -> when (item.category()) {
            Category.IMAGE -> "🖼"
            Category.VIDEO -> "🎬"
            Category.AUDIO -> "🎵"
            Category.DOC -> "📄"
            Category.ARCHIVE -> "🗜"
            Category.APK -> "📦"
            null -> "📄"
        }
    }

    fun size(item: com.qingfeng.files.data.FileItem): String =
        if (item.isDirectory) "${item.childCount.coerceAtLeast(0)} 项" else size(item.size)

    fun size(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digit = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(
            Locale.getDefault(),
            "%.1f %s",
            bytes / Math.pow(1024.0, digit.toDouble()),
            units[digit]
        )
    }

    fun time(item: com.qingfeng.files.data.FileItem): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(item.lastModified))
    }

    fun mime(name: String): String? {
        val ext = name.substringAfterLast('.', "")
        if (ext.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.lowercase())
    }
}
