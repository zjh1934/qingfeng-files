package com.qingfeng.files.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * 统一的文件数据模型。
 *
 * 借鉴点：参照物用一个 FileInfo 吃下所有来源（本地/网盘/FTP），
 * UI 层因此完全不需要知道文件从哪来。
 * 我们用 Kotlin 的 @Parcelize，不用手写 writeToParcel。
 */
@Parcelize
data class FileItem(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val extension: String = "",
    val mimeType: String? = null,
    val childCount: Int = -1
) : Parcelable
