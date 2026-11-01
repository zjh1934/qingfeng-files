package com.qingfeng.files.data

import android.webkit.MimeTypeMap
import java.io.File

/**
 * 本地存储实现。这是第一个 FileSource，后续可加 SMB / FTP / 云盘。
 */
class LocalFileSource : FileSource {

    override fun rootPath(): String = "/storage/emulated/0"

    @Suppress("DEPRECATION")
    private fun guessMime(file: File): String? {
        if (file.isDirectory) return null
        val ext = file.extension.lowercase()
        if (ext.isEmpty()) return null
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
    }

    private fun toItem(f: File): FileItem = FileItem(
        path = f.absolutePath,
        name = f.name,
        isDirectory = f.isDirectory,
        size = if (f.isDirectory) 0L else f.length(),
        lastModified = f.lastModified(),
        extension = f.extension.lowercase(),
        mimeType = guessMime(f),
        childCount = if (f.isDirectory) (f.listFiles()?.size ?: 0) else -1
    )

    override suspend fun list(path: String): List<FileItem> {
        val dir = File(path)
        if (!dir.exists()) throw FileError.NotFound(path)
        if (!dir.isDirectory) throw FileError.Unknown("不是目录：$path")
        val children = dir.listFiles() ?: throw FileError.PermissionDenied(path)

        val items = children.map { toItem(it) }

        // 目录在前，其余按名称排序（忽略大小写）
        return items.sortedWith(
            compareByDescending<FileItem> { it.isDirectory }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
        )
    }

    override suspend fun delete(item: FileItem): Boolean {
        return try {
            val f = File(item.path)
            if (f.isDirectory) f.deleteRecursively() else f.delete()
        } catch (e: Exception) {
            throw FileError.Io(e)
        }
    }

    override suspend fun rename(item: FileItem, newName: String): Boolean {
        return try {
            val f = File(item.path)
            val target = File(f.parentFile, newName)
            if (target.exists()) throw FileError.Unknown("已存在同名文件")
            f.renameTo(target)
        } catch (e: FileError) {
            throw e
        } catch (e: Exception) {
            throw FileError.Io(e)
        }
    }

    override suspend fun mkdir(parent: String, name: String): Boolean {
        return try {
            val target = File(parent, name)
            if (target.exists()) throw FileError.Unknown("已存在同名文件夹")
            target.mkdirs()
        } catch (e: FileError) {
            throw e
        } catch (e: Exception) {
            throw FileError.Io(e)
        }
    }

    override suspend fun copy(item: FileItem, destDir: String): Boolean {
        return try {
            val src = File(item.path)
            var dest = File(destDir, item.name)
            var n = 1
            while (dest.exists()) {
                dest = File(destDir, "${item.name}(${n++})")
            }
            if (src.isDirectory) src.copyRecursively(dest) else src.copyTo(dest)
            true
        } catch (e: Exception) {
            throw FileError.Io(e)
        }
    }

    override suspend fun move(item: FileItem, destDir: String): Boolean {
        return try {
            val src = File(item.path)
            val dest = File(destDir, item.name)
            if (dest.exists()) throw FileError.Unknown("目标已存在同名文件")
            src.renameTo(dest)
        } catch (e: FileError) {
            throw e
        } catch (e: Exception) {
            throw FileError.Io(e)
        }
    }

    override suspend fun search(base: String, keyword: String, limit: Int): List<FileItem> {
        val root = File(base)
        if (!root.exists()) throw FileError.NotFound(base)
        val kw = keyword.trim().lowercase()
        if (kw.isEmpty()) return emptyList()
        val out = ArrayList<FileItem>()
        walk(root, out, limit) { f -> f.name.lowercase().contains(kw) }
        return out
    }


    /** 深度优先遍历，达到 limit 或超时即停，避免大目录卡死 */
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
