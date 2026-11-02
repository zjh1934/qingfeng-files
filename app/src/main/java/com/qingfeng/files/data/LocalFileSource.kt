package com.qingfeng.files.data

import java.io.File
import java.io.InputStream
import java.io.OutputStream

/**
 * 本地存储实现。
 *
 * 复制/移动带进度回调，且**可取消**（对应参照物 `ig`/`k_`：
 * "确定要停止压缩/解压吗" —— 长操作可中断）。
 */
class LocalFileSource : FileSource {

    override fun rootPath(): String =
        android.os.Environment.getExternalStorageDirectory().absolutePath

    override fun supports(cap: FileSource.Capability): Boolean = true

    private fun toItem(f: File): FileItem = FileItem(
        path = f.absolutePath,
        name = f.name,
        isDirectory = f.isDirectory,
        size = if (f.isDirectory) 0L else f.length(),
        lastModified = f.lastModified(),
        extension = f.extension.lowercase(),
        mimeType = guessMime(f),
        childCount = if (f.isDirectory) -1 else -1
    )

    private fun guessMime(file: File): String? {
        val ext = file.extension.lowercase()
        return android.webkit.MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(ext)
    }

    override suspend fun list(path: String): List<FileItem> {
        val dir = File(path)
        if (!dir.exists()) throw FileError.NotFound(path)
        if (!dir.isDirectory) throw FileError.Unknown("不是目录：$path")
        val children = dir.listFiles() ?: throw FileError.PermissionDenied(path)
        return children.map { toItem(it) }.sortedWith(
            compareByDescending<FileItem> { it.isDirectory }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        )
    }

    override suspend fun search(
        base: String,
        keyword: String,
        scope: SearchScope,
        limit: Int
    ): List<FileItem> {
        val root = File(base)
        if (!root.exists()) throw FileError.NotFound(base)
        val kw = keyword.trim().lowercase()
        if (kw.isEmpty()) return emptyList()
        val exts = scope.exts
        val out = ArrayList<FileItem>()
        walk(root, out, limit) { f ->
            f.name.lowercase().contains(kw) && (exts == null || f.extension.lowercase() in exts)
        }
        return out
    }

    override suspend fun stat(path: String): FileStat? {
        val f = File(path)
        if (!f.exists()) return null
        return FileStat(
            exists = true,
            size = if (f.isDirectory) 0L else f.length(),
            lastModified = f.lastModified(),
            canRead = f.canRead(),
            canWrite = f.canWrite(),
            childCount = if (f.isDirectory) f.listFiles()?.size ?: 0 else -1
        )
    }

    override suspend fun countChildren(path: String): Int =
        File(path).listFiles()?.size ?: 0

    override suspend fun mkdir(parent: String, name: String): Boolean {
        val target = File(parent, name)
        if (target.exists()) throw FileError.Unknown("已存在同名文件夹")
        return target.mkdirs()
    }

    override suspend fun createFile(parent: String, name: String): Boolean {
        val target = File(parent, name)
        if (target.exists()) throw FileError.Unknown("已存在同名文件")
        return target.createNewFile()
    }

    override suspend fun delete(item: FileItem): Boolean =
        File(item.path).deleteRecursively()

    override suspend fun deleteAll(items: List<FileItem>): Boolean =
        items.all { File(it.path).deleteRecursively() }

    override suspend fun rename(item: FileItem, newName: String): Boolean {
        val src = File(item.path)
        val dest = File(src.parentFile, newName)
        if (dest.exists()) throw FileError.Unknown("已存在同名文件")
        return src.renameTo(dest)
    }

    override suspend fun copy(item: FileItem, destDir: String, cb: ProgressCallback?): Boolean =
        copyAll(listOf(item), destDir, cb)

    override suspend fun copyAll(
        items: List<FileItem>,
        destDir: String,
        cb: ProgressCallback?
    ): Boolean {
        var done = 0L
        val total = items.sumOf { sizeOf(File(it.path)) }.coerceAtLeast(1L)
        for (item in items) {
            if (cb?.isCancelled() == true) throw FileError.Unknown("已取消")
            val src = File(item.path)
            val dest = uniqueDest(src, destDir)
            copyOne(src, dest) { n -> done += n; cb?.onProgress(done, total) }
        }
        return true
    }

    override suspend fun move(item: FileItem, destDir: String, cb: ProgressCallback?): Boolean =
        moveAll(listOf(item), destDir, cb)

    override suspend fun moveAll(
        items: List<FileItem>,
        destDir: String,
        cb: ProgressCallback?
    ): Boolean {
        var i = 0
        for (item in items) {
            if (cb?.isCancelled() == true) throw FileError.Unknown("已取消")
            val src = File(item.path)
            val dest = File(destDir, src.name)
            if (dest.exists()) throw FileError.Unknown("目标已存在同名文件")
            src.renameTo(dest)
            cb?.onProgress(++i.toLong(), items.size.toLong())
        }
        return true
    }

    override suspend fun openRead(path: String): InputStream =
        File(path).inputStream()

    override suspend fun openWrite(path: String): OutputStream =
        File(path).outputStream()

    // ---------- 内部 ----------
    private fun sizeOf(f: File): Long =
        if (f.isDirectory) f.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        else f.length()

    private fun uniqueDest(src: File, destDir: String): File {
        var dest = File(destDir, src.name)
        var n = 1
        while (dest.exists()) dest = File(destDir, "${src.name}(${n++})")
        return dest
    }

    /** 单文件或目录复制，带字节级进度 */
    private fun copyOne(src: File, dest: File, onBytes: (Long) -> Unit) {
        if (src.isDirectory) {
            dest.mkdirs()
            src.listFiles()?.forEach { copyOne(it, File(dest, it.name), onBytes) }
            return
        }
        src.inputStream().use { input ->
            dest.outputStream().use { output ->
                val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val r = input.read(buf)
                    if (r <= 0) break
                    output.write(buf, 0, r)
                    onBytes(r.toLong())
                }
            }
        }
    }

    private fun walk(root: File, out: MutableList<FileItem>, limit: Int, match: (File) -> Boolean) {
        val stack = ArrayDeque<File>()
        stack.add(root)
        val deadline = System.currentTimeMillis() + 10_000L
        while (stack.isNotEmpty() && out.size < limit) {
            if (System.currentTimeMillis() > deadline) return
            val dir = stack.removeFirst()
            val children = dir.listFiles() ?: continue
            for (f in children) {
                if (out.size >= limit) return
                if (f.isDirectory) stack.addLast(f)
                else if (match(f)) out.add(toItem(f))
            }
        }
    }
}
