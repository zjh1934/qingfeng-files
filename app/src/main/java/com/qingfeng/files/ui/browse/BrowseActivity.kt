package com.qingfeng.files.ui.browse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.qingfeng.files.R
import com.qingfeng.files.base.BaseActivity
import com.qingfeng.files.data.Category
import com.qingfeng.files.data.FileItem
import com.qingfeng.files.data.Filter
import com.qingfeng.files.data.SortType
import java.io.File

/**
 * 文件浏览主页面。
 *
 * 借鉴参照物的设计：
 * - 分类是**筛选器**（在当前列表上收窄），不是"扫盘出结果页"
 * - 有排序切换、列表/网格切换
 * - 路径用面包屑分段显示，点一段跳回那一级
 */
class BrowseActivity : BaseActivity() {

    private val viewModel: BrowseViewModel by viewModels()
    private lateinit var adapter: FileAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var pathBar: android.widget.LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var statusText: TextView

    private var isGrid = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        pathBar = findViewById(R.id.pathBar)
        emptyText = findViewById(R.id.tvEmpty)
        swipe = findViewById(R.id.swipeRefresh)
        recycler = findViewById(R.id.recycler)
        statusText = findViewById(R.id.tvStatus)

        adapter = FileAdapter(
            onItemClick = { onItemClick(it) },
            onItemLongClick = { showItemMenu(it) }
        )
        applyLayoutManager()
        recycler.adapter = adapter

        swipe.setOnRefreshListener { viewModel.open(viewModel.currentPathValue()) }

        findViewById<View>(R.id.btnSort).setOnClickListener { showSortDialog() }
        findViewById<View>(R.id.btnFilter).setOnClickListener { showFilterDialog() }
        findViewById<View>(R.id.btnView).setOnClickListener { toggleViewMode() }
        findViewById<View>(R.id.btnSearch).setOnClickListener { showSearchDialog() }

        viewModel.items.observe(this) { list ->
            if (viewModel.isPlainBrowse()) {
                adapter.submitList(list)
                emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                updateStatus(list.size)
            }
            swipe.isRefreshing = false
        }
        viewModel.searchResults.observe(this) { list ->
            if (list != null) {
                adapter.submitList(list)
                emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                updateStatus(list.size, "搜索结果")
            }
        }
        viewModel.currentPath.observe(this) { if (viewModel.isPlainBrowse()) renderBreadcrumb(it) }
        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.start()
    }

    private fun updateStatus(count: Int, prefix: String = "共") {
        val f = viewModel.currentFilter()
        val suffix = if (f.isEmpty()) "" else "（已筛选）"
        statusText.text = "$prefix $count 项$suffix"
    }

    private fun applyLayoutManager() {
        recycler.layoutManager =
            if (isGrid) GridLayoutManager(this, 3) else LinearLayoutManager(this)
    }

    private fun toggleViewMode() {
        isGrid = !isGrid
        applyLayoutManager()
    }

    /** 面包屑：把路径拆成一段段，点一段跳回那一级 */
    private fun renderBreadcrumb(path: String) {
        pathBar.removeAllViews()
        val root = viewModel.rootPathValue()
        val rel = path.removePrefix(root).trim('/')
        val segs = mutableListOf("根目录")
        if (rel.isNotEmpty()) segs.addAll(rel.split('/'))
        segs.forEachIndexed { i, name ->
            if (i > 0) {
                val sep = TextView(this).apply { text = " › " }
                pathBar.addView(sep)
            }
            val tv = TextView(this).apply {
                text = name
                textSize = 13f
                setTextColor(resources.getColor(R.color.primary, theme))
                setOnClickListener {
                    // 回退到这一级
                    repeat(segs.size - 1 - i) { viewModel.goUp() }
                }
            }
            pathBar.addView(tv)
        }
    }

    private fun showSortDialog() {
        val labels = SortType.values().map { it.label }.toTypedArray()
        val cur = SortType.values().indexOf(viewModel.currentSort())
        MaterialAlertDialogBuilder(this)
            .setTitle("排序方式")
            .setSingleChoiceItems(labels, cur) { d, which ->
                viewModel.applySort(SortType.values()[which])
                d.dismiss()
            }
            .show()
    }

    private fun showFilterDialog() {
        val cur = viewModel.currentFilter()
        val catLabels = Category.values().map { it.label }.toTypedArray()
        val catChecked = BooleanArray(catLabels.size) { i -> Category.values()[i] in cur.categories }

        val sizeLabels = arrayOf("不限", "< 1MB", "1–10MB", "10–100MB", "> 100MB")
        val sizeIdx = when {
            cur.sizeFrom == null && cur.sizeTo == null -> 0
            cur.sizeTo != null && cur.sizeTo <= 1_048_576L -> 1
            cur.sizeFrom == 1_048_576L -> 2
            cur.sizeFrom == 10_485_760L -> 3
            else -> 4
        }

        val timeLabels = arrayOf("不限", "今天", "最近 7 天", "最近 30 天", "更早")
        val timeIdx = when {
            cur.timeFrom == null && cur.timeTo == null -> 0
            cur.timeFrom != null && cur.timeFrom >= todayStart() -> 1
            cur.timeFrom != null && cur.timeFrom >= daysAgo(7) -> 2
            cur.timeFrom != null && cur.timeFrom >= daysAgo(30) -> 3
            else -> 4
        }

        val hiddenLabels = arrayOf("显示隐藏文件")
        val hiddenChecked = booleanArrayOf(cur.showHidden)

        MaterialAlertDialogBuilder(this)
            .setTitle("筛选")
            .setMultiChoiceItems(catLabels, catChecked) { _, w, c -> catChecked[w] = c }
            .setSingleChoiceItems(sizeLabels, sizeIdx) { _, _ -> }
            .setNegativeButton("取消", null)
            .setNeutralButton("清除筛选") { _, _ -> viewModel.applyFilter(Filter()) }
            .setPositiveButton("应用") { _, _ ->
                val cats = Category.values().filterIndexed { i, _ -> catChecked[i] }.toSet()
                val (sf, st) = sizeRangeOf(indexOfSize(sizeLabels, sizeIdx))
                val (tf, tt) = timeRangeOf(indexOfTime(timeLabels, timeIdx))
                viewModel.applyFilter(Filter(cats, sf, st, tf, tt, hiddenChecked[0]))
            }
            .show()
    }

    private fun indexOfSize(labels: Array<String>, idx: Int) = idx
    private fun indexOfTime(labels: Array<String>, idx: Int) = idx

    private fun sizeRangeOf(i: Int): Pair<Long?, Long?> = when (i) {
        1 -> 0L to 1_048_576L
        2 -> 1_048_576L to 10_485_760L
        3 -> 10_485_760L to 104_857_600L
        4 -> 104_857_600L to null
        else -> null to null
    }

    private fun timeRangeOf(i: Int): Pair<Long?, Long?> = when (i) {
        1 -> todayStart() to null
        2 -> daysAgo(7) to null
        3 -> daysAgo(30) to null
        4 -> null to daysAgo(30)
        else -> null to null
    }

    private fun todayStart(): Long = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }.timeInMillis

    private fun daysAgo(n: Int): Long = System.currentTimeMillis() - n * 86_400_000L

    private fun showSearchDialog() {
        val input = TextInputEditText(this).apply { hint = "文件名关键字" }
        MaterialAlertDialogBuilder(this)
            .setTitle("在当前目录及子目录中搜索")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("搜索") { _, _ ->
                val kw = input.text?.toString()?.trim()
                if (!kw.isNullOrEmpty()) viewModel.search(kw)
            }
            .show()
    }

    private fun onItemClick(item: FileItem) {
        if (item.isDirectory) viewModel.enterDirectory(item) else openFile(item)
    }

    private fun openFile(item: FileItem) {
        try {
            val file = File(item.path)
            val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, item.mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "打开方式"))
        } catch (e: Exception) {
            Toast.makeText(this, "打不开这个文件", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showItemMenu(item: FileItem) {
        val options = arrayOf("删除", "重命名", "复制到…", "移动到…")
        MaterialAlertDialogBuilder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmDelete(item)
                    1 -> showRenameDialog(item)
                    2 -> pickTargetDir(item, move = false)
                    3 -> pickTargetDir(item, move = true)
                }
            }
            .show()
    }

    private fun pickTargetDir(item: FileItem, move: Boolean) {
        val base = File(viewModel.currentPathValue())
        val dirs = mutableListOf("..（上一级）").apply {
            addAll(base.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList())
        }
        if (dirs.size <= 1) {
            Toast.makeText(this, "当前目录下没有子文件夹", Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(if (move) "移动到" else "复制到")
            .setItems(dirs.toTypedArray()) { _, i ->
                val target = if (i == 0) base.parentFile?.absolutePath else File(base, dirs[i]).absolutePath
                if (target != null) {
                    if (move) viewModel.moveTo(item, target) else viewModel.copyTo(item, target)
                }
            }
            .show()
    }

    private fun confirmDelete(item: FileItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除")
            .setMessage("确定删除「${item.name}」？${if (item.isDirectory) "文件夹内所有内容也会被删除。" else ""}")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ -> viewModel.delete(item) }
            .show()
    }

    private fun showRenameDialog(item: FileItem) {
        val input = TextInputEditText(this).apply { setText(item.name) }
        MaterialAlertDialogBuilder(this)
            .setTitle("重命名")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _, _ ->
                val newName = input.text?.toString()?.trim()
                if (!newName.isNullOrEmpty()) viewModel.rename(item, newName)
            }
            .show()
    }

    override fun onBackPressed() {
        if (viewModel.searchResults.value != null) {
            viewModel.clearSearch(); viewModel.open(viewModel.currentPathValue()); return
        }
        if (!viewModel.goUp()) super.onBackPressed()
    }
}
