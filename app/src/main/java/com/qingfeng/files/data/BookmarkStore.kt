package com.qingfeng.files.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 书签 / 收藏。
 *
 * 借鉴点：参照物两套 —— 书签（目录）和收藏（文件）：
 * - `f0` = Bookmarks、`m4` = Add Bookmark
 * - `iv` = Add to favorite
 *
 * 我们做一套：既能加目录也能加文件，列表里点一下就跳过去。
 */
class BookmarkStore(context: Context) {

    private val sp = context.getSharedPreferences("qingfeng_bookmarks", Context.MODE_PRIVATE)

    data class Bookmark(val path: String, val name: String, val isDirectory: Boolean, val addedAt: Long)

    fun list(): List<Bookmark> {
        val raw = sp.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).mapNotNull { i ->
                val o = arr.getJSONObject(i)
                val p = o.optString("p")
                if (p.isBlank()) null
                else Bookmark(p, o.optString("n"), o.optBoolean("d"), o.optLong("t"))
            }.sortedBy { it.addedAt }
        } catch (e: Exception) { emptyList() }
    }

    fun has(path: String): Boolean = list().any { it.path == path }

    fun add(path: String, name: String, isDirectory: Boolean) {
        if (has(path)) return
        val list = list().toMutableList()
        list.add(Bookmark(path, name, isDirectory, System.currentTimeMillis()))
        save(list)
    }

    fun remove(path: String) = save(list().filter { it.path != path })

    fun toggle(path: String, name: String, isDirectory: Boolean): Boolean {
        return if (has(path)) { remove(path); false } else { add(path, name, isDirectory); true }
    }

    private fun save(list: List<Bookmark>) {
        val arr = JSONArray()
        list.forEach { b ->
            arr.put(JSONObject().apply {
                put("p", b.path); put("n", b.name); put("d", b.isDirectory); put("t", b.addedAt)
            })
        }
        sp.edit().putString(KEY, arr.toString()).apply()
    }

    companion object { private const val KEY = "list" }
}
