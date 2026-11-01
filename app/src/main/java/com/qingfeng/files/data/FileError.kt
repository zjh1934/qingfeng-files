package com.qingfeng.files.data

/**
 * 文件操作的错误类型。
 *
 * 借鉴点：参照物给每种来源定义了独立异常，但都继承同一个根。
 * 我们用 sealed class，when 分支能穷尽，比继承树更适合 Kotlin。
 */
sealed class FileError(message: String, cause: Throwable? = null) : Throwable(message, cause) {
    class NotFound(path: String) : FileError("找不到：$path")
    class PermissionDenied(path: String) : FileError("没有权限：$path")
    class Io(cause: Throwable) : FileError(cause.message ?: "读写失败", cause)
    class Unknown(msg: String) : FileError(msg)
}
