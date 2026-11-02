package com.qingfeng.files.data

import android.content.Context

/**
 * 偏好存储。
 *
 * 借鉴点：参照物用**两个** SharedPreferences 文件来做视图记忆：
 * - `key_view_mode_path`  按目录记（不同目录可以有不同视图）
 * - `key_view_mode_global` 全局（勾了"应用到所有文件夹"时用）
 *
 * 我们简化为一个文件，但保留「按目录 + 全局开关」的语义。
 */
class FilePrefs(context: Context) {

    private val global = context.getSharedPreferences("qingfeng_global", Context.MODE_PRIVATE)
    private val perPath = context.getSharedPreferences("qingfeng_per_path", Context.MODE_PRIVATE)

    /** 是否统一视图（对应参照物的 "Apply to all folders"） */
    var applyToAll: Boolean
        get() = global.getBoolean(KEY_APPLY_ALL, false)
        set(v) = global.edit().putBoolean(KEY_APPLY_ALL, v).apply()

    var globalViewMode: ViewMode
        get() = ViewMode.values().getOrNull(global.getInt(KEY_VIEW_MODE, ViewMode.GRID_MEDIUM.ordinal))
            ?: ViewMode.GRID_MEDIUM
        set(v) = global.edit().putInt(KEY_VIEW_MODE, v.ordinal).apply()

    fun viewModeFor(path: String): ViewMode =
        if (applyToAll) globalViewMode
        else ViewMode.values().getOrNull(perPath.getInt(key(path), ViewMode.GRID_MEDIUM.ordinal))
            ?: ViewMode.GRID_MEDIUM

    fun setViewMode(path: String, mode: ViewMode, applyAll: Boolean) {
        applyToAll = applyAll
        if (applyAll) globalViewMode = mode else perPath.edit().putInt(key(path), mode.ordinal).apply()
    }

    /** 是否显示隐藏文件 */
    var showHidden: Boolean
        get() = global.getBoolean(KEY_SHOW_HIDDEN, false)
        set(v) = global.edit().putBoolean(KEY_SHOW_HIDDEN, v).apply()

    private fun key(path: String): String = "vm_$path".take(200)

    companion object {
        private const val KEY_APPLY_ALL = "apply_all"
        private const val KEY_VIEW_MODE = "view_mode"
        private const val KEY_SHOW_HIDDEN = "show_hidden"
    }
}
