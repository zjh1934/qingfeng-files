package com.qingfeng.files.data

/**
 * 视图模式。
 *
 * 借鉴点：参照物不止"列表/网格"两档，还有**密度档位**：
 * List / Grid / Compact / Medium / Bigger / Smaller（string ao0~ao5）。
 * 我们做：列表 + 网格，网格再分三档密度。
 */
enum class ViewMode(val label: String, val isGrid: Boolean, val span: Int, val itemHeightDp: Int) {
    LIST("列表", false, 1, 56),
    GRID_COMPACT("网格·紧凑", true, 4, 88),
    GRID_MEDIUM("网格·中等", true, 3, 108),
    GRID_LARGE("网格·大", true, 2, 140)
}
