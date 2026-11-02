package com.qingfeng.files.data

/**
 * 文件元信息（扩展 FileItem 之外的可变信息）。
 *
 * 借鉴点：参照物的 FileInfo 不止路径和大小，还带"访问类型"标记
 * （SMB 类型常量 SHARE/WORKGROUP/COMM/FILESYSTEM/NAMED_PIPE/PRINTER/SERVER），
 * 说明它把"来源身份"也编码进数据模型 —— 一套模型吃所有来源。
 */
data class FileStat(
    val exists: Boolean,
    val size: Long,
    val lastModified: Long,
    val canRead: Boolean,
    val canWrite: Boolean,
    val childCount: Int
)

/**
 * 读写进度回调。
 *
 * 借鉴点：参照物有 SegmentInfo / SegmentInfos —— 分片复制的断点续传结构。
 * 我们先把"进度 + 可取消"做出来，续传（记录已完成分片）下一步。
 */
interface ProgressCallback {
    fun onProgress(done: Long, total: Long)
    fun isCancelled(): Boolean = false
}
