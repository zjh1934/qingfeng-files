package com.qingfeng.files.data

/**
 * 文件来源接口。
 *
 * 借鉴点：参照物的文件源接口有 **20+ 个方法**（`ace/lt3`），
 * 而且有个**工厂方法** `zr5.j(String) → lt3`：
 * 按路径里的协议前缀（gdrive/googledrive/onedrive/dropbox/mega/box/yandex/pcs）
 * 返回对应的实现类 —— **一套 UI 接 N 种源**就是靠这个。
 *
 * 我们目前只有本地，但接口按"将来要接 N 种源"来设计，
 * 并预留工厂（FileSources.forPath）。
 */
interface FileSource {

    // ---------- 浏览 ----------
    suspend fun list(path: String): List<FileItem>

    suspend fun search(
        base: String,
        keyword: String,
        scope: SearchScope = SearchScope.ALL,
        limit: Int = 200
    ): List<FileItem>

    // ---------- 元信息 ----------
    /** 单个文件的详细信息（不存在返回 null） */
    suspend fun stat(path: String): FileStat?

    /** 目录里的条目数（用于目录排序按大小） */
    suspend fun countChildren(path: String): Int

    // ---------- 增删改 ----------
    suspend fun mkdir(parent: String, name: String): Boolean
    suspend fun createFile(parent: String, name: String): Boolean
    suspend fun delete(item: FileItem): Boolean
    suspend fun deleteAll(items: List<FileItem>): Boolean
    suspend fun rename(item: FileItem, newName: String): Boolean

    // ---------- 复制 / 移动（带进度） ----------
    suspend fun copy(item: FileItem, destDir: String, cb: ProgressCallback? = null): Boolean
    suspend fun copyAll(items: List<FileItem>, destDir: String, cb: ProgressCallback? = null): Boolean
    suspend fun move(item: FileItem, destDir: String, cb: ProgressCallback? = null): Boolean
    suspend fun moveAll(items: List<FileItem>, destDir: String, cb: ProgressCallback? = null): Boolean

    // ---------- 读写流 ----------
    suspend fun openRead(path: String): java.io.InputStream
    suspend fun openWrite(path: String): java.io.OutputStream

    // ---------- 能力查询 ----------
    /** 该来源是否支持某能力（云盘可能不支持 rename 等） */
    fun supports(cap: Capability): Boolean

    fun rootPath(): String

    enum class Capability { CREATE_FILE, RENAME, DELETE, COPY, MOVE, STREAM, SEARCH }
}
