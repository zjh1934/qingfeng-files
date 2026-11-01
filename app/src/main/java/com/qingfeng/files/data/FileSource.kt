package com.qingfeng.files.data

/**
 * 文件来源抽象。
 *
 * 借鉴点（参照物最有价值的一处设计）：
 * 「文件从哪来」和「文件怎么显示」彻底分开。
 * 以后加网上邻居、FTP、云盘，只要再实现一个 FileSource，UI 一行都不用改。
 */
interface FileSource {
    /** 根目录 */
    fun rootPath(): String

    /** 列出某目录下的内容（目录排前，再按名称排） */
    suspend fun list(path: String): List<FileItem>

    /** 删除文件或空目录 */
    suspend fun delete(item: FileItem): Boolean

    /** 重命名 */
    suspend fun rename(item: FileItem, newName: String): Boolean

    /** 新建文件夹 */
    suspend fun mkdir(parent: String, name: String): Boolean

    /** 复制到目标目录 */
    suspend fun copy(item: FileItem, destDir: String): Boolean

    /** 移动到目标目录 */
    suspend fun move(item: FileItem, destDir: String): Boolean

    /**
     * 搜索：在 base 下递归找名字含 keyword 的项。
     * limit 控制返回条数，避免大目录卡死。
     */
    suspend fun search(base: String, keyword: String, limit: Int = 200): List<FileItem>

}
