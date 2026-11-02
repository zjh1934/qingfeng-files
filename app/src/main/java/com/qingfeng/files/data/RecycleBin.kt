package com.qingfeng.files.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * 回收站。
 *
 * 借鉴点：参照物**删除默认进回收站**，不是直接抹掉。
 * 证据（strings.xml）：
 * - `az` / `bt` / `kn` = "Move to Recycle Bin" / "Put file into Recycle Bin"
 * - `l5` = "Delete file permanently instead of putting into Recycle Bin in the future"
 *   → 反过来说明：默认行为就是放进回收站，想永久删除才要另外勾。
 *
 * 这是防误删的安全设计，文件管理器里误删代价很高。
 *
 * 实现：文件移到 `.qingfeng_trash/<时间戳_名称>`，用 JSON 记录原路径。
 */
class RecycleBin(private val root: File) {

    private val dir = File(root, TRASH_DIR)
    private val index = File(dir, INDEX_FILE)

    data class Entry(
        val trashName: String,
        val originPath: String,
        val deletedAt: Long,
        val isDirectory: Boolean
    )

    fun ensure() {
        if (!dir.exists()) dir.mkdirs()
    }

    /** 把文件/目录移入回收站，返回是否成功 */
    fun moveToTrash(item: FileItem): Boolean {
        ensure()
        val src = File(item.path)
        if (!src.exists()) return false
        val trashName = "${System.currentTimeMillis()}_${src.name}"
        val dest = File(dir, trashName)
        if (!src.renameTo(dest)) return false
        val list = readIndex().toMutableList()
        list.add(Entry(trashName, item.path, System.currentTimeMillis(), item.isDirectory))
        writeIndex(list)
        return true
    }

    fun list(): List<Entry> = readIndex().sortedByDescending { it.deletedAt }

    /** 恢复到原路径 */
    fun restore(entry: Entry): Boolean {
        val src = File(dir, entry.trashName)
        if (!src.exists()) { remove(entry); return false }
        val dest = File(entry.originPath)
        if (dest.exists()) return false
        dest.parentFile?.mkdirs()
        return if (src.renameTo(dest)) { remove(entry); true } else false
    }

    fun deleteForever(entry: Entry) {
        File(dir, entry.trashName).deleteRecursively()
        remove(entry)
    }

    fun clear() {
        val list = readIndex()
        list.forEach { File(dir, it.trashName).deleteRecursively() }
        writeIndex(emptyList())
    }

    fun count(): Int = readIndex().size

    fun totalSize(): Long = readIndex().sumOf { File(dir, it.trashName).let { f ->
        if (f.isDirectory) f.walkTopDown().filter { it.isFile }.sumOf { it.length() } else f.length()
    } }

    private fun remove(entry: Entry) {
        writeIndex(readIndex().filter { it.trashName != entry.trashName })
    }

    private fun readIndex(): List<Entry> {
        if (!index.exists()) return emptyList()
        return try {
            val arr = JSONArray(index.readText())
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                Entry(
                    trashName = o.optString("t"),
                    originPath = o.optString("o"),
                    deletedAt = o.optLong("d"),
                    isDirectory = o.optBoolean("isDir")
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    private fun writeIndex(list: List<Entry>) {
        ensure()
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(JSONObject().apply {
                put("t", e.trashName)
                put("o", e.originPath)
                put("d", e.deletedAt)
                put("isDir", e.isDirectory)
            })
        }
        index.writeText(arr.toString())
    }

    companion object {
        const val TRASH_DIR = ".qingfeng_trash"
        const val INDEX_FILE = "index.json"

        fun from(context: Context): RecycleBin =
            RecycleBin(android.os.Environment.getExternalStorageDirectory())
    }
}
