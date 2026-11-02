package com.qingfeng.files.ui.browse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
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
import com.qingfeng.files.data.Clipboard
import com.qingfeng.files.data.FileItem
import com.qingfeng.files.data.Filter
import com.qingfeng.files.data.SearchScope
import com.qingfeng.files.data.SortDir
import com.qingfeng.files.data.SortField
import com.qingfeng.files.data.SortState
import com.qingfeng.files.data.ViewMode
import com.qingfeng.files.data.BookmarkStore
import com.qingfeng.files.util.FileFormat
import java.io.File

/**
 * 文件浏览主页面。
 */
class BrowseActivity : BaseActivity() {

    private val viewModel: BrowseViewModel by viewModels()
    private lateinit var adapter: FileAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var pathBar: LinearLayout
    private lateinit var emptyText: TextView
    private lateinit var deepSearchText: TextView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var statusText: TextView
    private lateinit var actionBar: LinearLayout
    private lateinit var btnPaste: TextView
    private lateinit var progressRow: View
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var btnCancel: TextView
    private lateinit var btnSelectAll: TextView

    private val checked = HashSet<String>()
    private var selectionMode = false
    private var lastKeyword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        pathBar = findViewById(R.id.pathBar)
        emptyText = findViewById(R.id.tvEmpty)
        deepSearchText = findViewById(R.id.tvDeepSearch)
        swipe = findViewById(R.id.swipeRefresh)
        recycler = findViewById(R.id.recycler)
        statusText = findViewById(R.id.tvStatus)
        actionBar = findViewById(R.id.actionBar)
        btnPaste = findViewById(R.id.btnPaste)
        btnSelectAll = findViewById(R.id.btnSelectAll)
        findViewById<FastScroller>(R.id.fastScroller).recyclerView = recycler
        progressRow = findViewById(R.id.progressRow)
        progressBar = findViewById(R.id.progressBar)
        btnCancel = findViewById(R.id.btnCancel)

        adapter = FileAdapter(
            onItemClick = { onItemClick(it) },
            onItemLongClick = { showFileMenu(it) },
            onMoreClick = { showFileMenu(it) },
            onCheckChange = { item, isChecked ->
                if (isChecked) checked.add(item.path) else checked.remove(item.path)
                refreshSelectionUi()
            }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        swipe.setOnRefreshListener { viewModel.open(viewModel.currentPathValue()) }
        findViewById<View>(R.id.btnUp).setOnClickListener { if (!viewModel.goUp()) finish() }
        findViewById<View>(R.id.btnTrash).setOnClickListener { openTrash() }
        findViewById<View>(R.id.btnDevice).setOnClickListener { showStorageMenu() }
        findViewById<View>(R.id.btnBookmark).setOnClickListener { showBookmarkMenu() }
        viewModel.currentBookmarked.observe(this) { on ->
            findViewById<TextView>(R.id.btnBookmark).text = if (on) "★" else "☆"
        }
        viewModel.trashCount.observe(this) { n ->
            findViewById<TextView>(R.id.btnTrash).text = if (n > 0) "回收站($n)" else "回收站"
        }

        findViewById<View>(R.id.btnSort).setOnClickListener { showSortDialog() }
        findViewById<View>(R.id.btnView).setOnClickListener { showViewDialog() }
        findViewById<View>(R.id.btnFilter).setOnClickListener { showFilterDialog() }
        findViewById<View>(R.id.btnSearch).setOnClickListener { showSearchDialog() }
        findViewById<View>(R.id.btnNew).setOnClickListener { showNewFolderDialog() }
        val btnHidden = findViewById<TextView>(R.id.btnHidden)
        btnHidden.setOnClickListener {
            val on = viewModel.toggleHidden()
            btnHidden.text = if (on) "隐藏✓" else "隐藏"
        }
        viewModel.hiddenOn.observe(this) { btnHidden.text = if (it) "隐藏✓" else "隐藏" }
        btnSelectAll.setOnClickListener { toggleSelectAll() }
        btnPaste.setOnClickListener { viewModel.pasteHere() }
        findViewById<View>(R.id.btnActionCopy).setOnClickListener { putClipboard(Clipboard.Mode.COPY) }
        findViewById<View>(R.id.btnActionCut).setOnClickListener { putClipboard(Clipboard.Mode.CUT) }
        findViewById<View>(R.id.btnActionDelete).setOnClickListener { confirmDeleteSelected() }
        findViewById<View>(R.id.btnActionClose).setOnClickListener { exitSelection() }
        btnCancel.setOnClickListener { viewModel.cancelPaste() }
        viewModel.progress.observe(this) { pr ->
            if (pr == null) {
                // 先隐藏子控件，再隐藏容器（容器默认 gone，不先显示子控件就看不见）
                progressBar.visibility = View.GONE
                btnCancel.visibility = View.GONE
                progressRow.visibility = View.GONE
            } else {
                progressRow.visibility = View.VISIBLE
                progressBar.visibility = View.VISIBLE
                btnCancel.visibility = View.VISIBLE
                val (done, total) = pr
                progressBar.progress = (done * 100 / total.coerceAtLeast(1)).toInt().coerceIn(0, 100)
            }
        }
        deepSearchText.setOnClickListener {
            if (lastKeyword.isNotBlank()) viewModel.searchDeeper(lastKeyword, viewModel.searchScope.value ?: SearchScope.ALL)
        }

        viewModel.items.observe(this) { list ->
            if (viewModel.isPlainBrowse()) { adapter.submitList(list); renderState(list) }
            swipe.isRefreshing = false
        }
        viewModel.searchResults.observe(this) { list ->
            if (list != null) { adapter.submitList(list); renderState(list, "搜索结果") }
        }
        viewModel.currentPath.observe(this) { if (viewModel.isPlainBrowse()) renderBreadcrumb(it) }
        viewModel.viewMode.observe(this) { applyViewMode(it) }
        viewModel.clipboard.observe(this) { updatePasteUi(it) }
        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.start()
    }

    private fun renderState(list: List<FileItem>, prefix: String = "共") {
        emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        // 借鉴点：搜索无结果时给"深度搜索"引导，而不是死胡同
        deepSearchText.visibility =
            if (list.isEmpty() && lastKeyword.isNotBlank()) View.VISIBLE else View.GONE
        val f = viewModel.currentFilter()
        statusText.text = "$prefix ${list.size} 项${if (f.isEmpty()) "" else "（已筛选）"}"
    }

    private fun applyViewMode(mode: ViewMode) {
        adapter.viewMode = mode
        adapter.notifyDataSetChanged()
        recycler.layoutManager =
            if (mode.isGrid) GridLayoutManager(this, mode.span) else LinearLayoutManager(this)
    }

    private fun renderBreadcrumb(path: String) {
        pathBar.removeAllViews()
        val root = viewModel.rootPathValue()
        val rel = path.removePrefix(root).trim('/')
        val segs = mutableListOf("根目录")
        if (rel.isNotEmpty()) segs.addAll(rel.split('/'))
        segs.forEachIndexed { i, name ->
            if (i > 0) pathBar.addView(TextView(this).apply { text = " › " })
            pathBar.addView(TextView(this).apply {
                text = name
                textSize = 13f
                setOnClickListener { repeat(segs.size - 1 - i) { viewModel.goUp() } }
            })
        }
    }

    // ---------- 排序面板 ----------
    /**
     * 排序面板。
     * 借鉴点：参照物是 **PopupMenu**（`yw2.smali` 的 onDismiss(PopupMenu)），
     * 不是 Dialog。挂在工具栏按钮下方弹出。
     */
    private fun showSortDialog() {
        val anchor = findViewById<View>(R.id.btnSort)
        val popup = android.widget.PopupMenu(this, anchor)
        val cur = viewModel.currentSort()
        SortField.values().forEachIndexed { i, f ->
            val mark = if (f == cur.field) (if (cur.dir == SortDir.ASC) " ↑" else " ↓") else ""
            popup.menu.add(0, i, 0, f.label + mark)
        }
        popup.menu.add(1, 99, 0, "切换升降序")
        popup.setOnMenuItemClickListener { item ->
            if (item.itemId == 99) {
                viewModel.applySort(cur.toggled())
            } else {
                val f = SortField.values()[item.itemId]
                // 点已选中的字段 = 切方向（对应参照物每个选项的方向箭头）
                viewModel.applySort(if (f == cur.field) cur.toggled() else SortState(f, SortDir.ASC))
            }
            true
        }
        popup.show()
    }

    // ---------- 视图面板 ----------
    /** 视图面板，同样用 PopupMenu */
    private fun showViewDialog() {
        val anchor = findViewById<View>(R.id.btnView)
        val popup = android.widget.PopupMenu(this, anchor)
        ViewMode.values().forEachIndexed { i, mode ->
            val item = popup.menu.add(0, i, 0, mode.label)
            item.isChecked = mode == adapter.viewMode
        }
        popup.menu.setGroupCheckable(0, true, true)
        popup.setOnMenuItemClickListener {
            viewModel.setViewMode(ViewMode.values()[it.itemId], applyAll = true)
            true
        }
        popup.show()
    }

    // ---------- 筛选面板 ----------
    private fun showFilterDialog() {
        val cur = viewModel.currentFilter()
        val catLabels = Category.values().map { it.label }.toTypedArray()
        val catChecked = BooleanArray(catLabels.size) { i -> Category.values()[i] in cur.categories }
        val hiddenChecked = booleanArrayOf(cur.showHidden)

        MaterialAlertDialogBuilder(this)
            .setTitle("筛选")
            .setMultiChoiceItems(catLabels, catChecked) { _, w, c -> catChecked[w] = c }
            .setMultiChoiceItems(arrayOf("显示隐藏文件"), hiddenChecked) { _, _, c -> hiddenChecked[0] = c }
            .setNegativeButton("取消", null)
            .setNeutralButton("清除") { _, _ -> viewModel.applyFilter(Filter()) }
            .setPositiveButton("应用") { _, _ ->
                val cats = Category.values().filterIndexed { i, _ -> catChecked[i] }.toSet()
                viewModel.applyFilter(cur.copy(categories = cats, showHidden = hiddenChecked[0]))
            }
            .show()
    }

    // ---------- 搜索（带范围） ----------
    private fun showSearchDialog() {
        val input = TextInputEditText(this).apply { hint = "文件名关键字" }
        val scopes = SearchScope.values().map { it.label }.toTypedArray()
        var scope = SearchScope.ALL
        MaterialAlertDialogBuilder(this)
            .setTitle("搜索")
            .setView(input)
            .setSingleChoiceItems(scopes, 0) { _, w -> scope = SearchScope.values()[w] }
            .setNegativeButton("取消", null)
            .setPositiveButton("搜索") { _, _ ->
                val kw = input.text?.toString()?.trim()
                if (!kw.isNullOrEmpty()) {
                    lastKeyword = kw
                    viewModel.search(kw, scope)
                }
            }
            .show()
    }

    private fun showNewFolderDialog() {
        val input = TextInputEditText(this).apply { hint = "文件夹名称" }
        MaterialAlertDialogBuilder(this)
            .setTitle("新建文件夹")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("创建") { _, _ ->
                val n = input.text?.toString()?.trim()
                if (!n.isNullOrEmpty()) viewModel.mkdir(n)
            }
            .show()
    }

    // ---------- 多选 ----------
    /** 单文件长按 → 操作菜单（参照物：Open/Copy/Cut/Rename/Delete/Share/Properties...） */
    private fun showFileMenu(item: FileItem) {
        val opts = mutableListOf("打开", "打开为…", "复制", "剪切", "重命名", "分享", "属性", "书签", "多选", "删除")
        MaterialAlertDialogBuilder(this)
            .setTitle(item.name)
            .setItems(opts.toTypedArray()) { _, which ->
                when (which) {
                    0 -> openFile(item)
                    1 -> openFileAs(item)
                    2 -> { viewModel.putClipboard(listOf(item), Clipboard.Mode.COPY); hintCopied(1, Clipboard.Mode.COPY) }
                    3 -> { viewModel.putClipboard(listOf(item), Clipboard.Mode.CUT); hintCopied(1, Clipboard.Mode.CUT) }
                    4 -> showRenameDialog(item)
                    5 -> shareFile(item)
                    6 -> showProperties(item)
                    7 -> {
                        viewModel.toggleBookmark(item)
                        Toast.makeText(this, "已切换书签", Toast.LENGTH_SHORT).show()
                    }
                    8 -> { enterSelection(item); }
                    9 -> confirmDeleteOne(item)
                }
            }
            .show()
    }

    /** 打开为：指定 MIME 类型打开（参照物 "Open As"） */
    private fun openFileAs(item: FileItem) {
        val types = arrayOf("文本", "图片", "视频", "音频", "其他（任意类型）")
        val mimes = arrayOf("text/plain", "image/*", "video/*", "audio/*", "*/*")
        MaterialAlertDialogBuilder(this)
            .setTitle("打开为")
            .setItems(types) { _, i -> openFile(item, mimes[i]) }
            .show()
    }

    private fun shareFile(item: FileItem) {
        try {
            val file = java.io.File(item.path)
            val uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = item.mimeType ?: "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "分享"))
        } catch (e: Exception) {
            Toast.makeText(this, "无法分享", Toast.LENGTH_SHORT).show()
        }
    }

    /** 属性（参照物 "Properties"） */
    private fun showProperties(item: FileItem) {
        val f = java.io.File(item.path)
        val info = buildString {
            append("名称：${item.name}\n")
            append("路径：${item.path}\n")
            append("类型：${if (item.isDirectory) "文件夹" else item.extension.ifEmpty { "文件" }}\n")
            if (!item.isDirectory) append("大小：${FileFormat.size(item.size)}\n")
            append("修改时间：${FileFormat.time(item)}\n")
            append("可读：${if (f.canRead()) "是" else "否"}   可写：${if (f.canWrite()) "是" else "否"}\n")
            if (item.isDirectory) {
                val n = f.listFiles()?.size ?: 0
                append("包含：$n 项")
            }
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("属性")
            .setMessage(info)
            .setPositiveButton("确定", null)
            .show()
    }

    /** 删除：默认进回收站，可勾选永久删除（参照物 `l5`） */
    private fun confirmDeleteOne(item: FileItem) {
        var permanent = false
        val msg = "「${item.name}」将被移到回收站，可在回收站中恢复。"
        MaterialAlertDialogBuilder(this)
            .setTitle("删除")
            .setMessage(msg)
            .setMultiChoiceItems(arrayOf("永久删除（不可恢复）"), booleanArrayOf(false)) { _, _, c -> permanent = c }
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ -> viewModel.delete(item, permanent) }
            .show()
    }

    /** 批量删除：同样默认进回收站 */
    private fun confirmDeleteSelected() {
        val items = adapter.currentList.filter { it.path in checked }
        if (items.isEmpty()) return
        var permanent = false
        MaterialAlertDialogBuilder(this)
            .setTitle("删除 ${items.size} 项")
            .setMessage("这些内容将被移到回收站，可在回收站中恢复。")
            .setMultiChoiceItems(arrayOf("永久删除（不可恢复）"), booleanArrayOf(false)) { _, _, c -> permanent = c }
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteAllSelected(items, permanent)
                exitSelection()
            }
            .show()
    }

    private fun openTrash() {
        startActivity(Intent(this, TrashActivity::class.java))
    }

    /** 存储切换（参照物 device_icon） */
    private fun showStorageMenu() {
        val list = viewModel.storages()
        if (list.isEmpty()) { Toast.makeText(this, "没找到其他存储", Toast.LENGTH_SHORT).show(); return }
        val labels = list.map { it.label + "（" + it.path + "）" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("切换存储")
            .setItems(labels) { _, i -> viewModel.switchStorage(list[i]) }
            .show()
    }

    /** 书签菜单（参照物 f0=Bookmarks / m4=Add Bookmark） */
    private fun showBookmarkMenu() {
        val bms = viewModel.bookmarkList.value ?: emptyList()
        val opts = mutableListOf("把当前目录加入书签 / 取消")
        opts.addAll(bms.map { it.name + "（" + it.path + "）" })
        if (bms.isNotEmpty()) opts.add("管理书签…")

        MaterialAlertDialogBuilder(this)
            .setTitle("书签")
            .setItems(opts.toTypedArray()) { _, which ->
                when (which) {
                    0 -> {
                        val added = viewModel.toggleBookmarkCurrent()
                        Toast.makeText(this, if (added) "已加入书签" else "已取消书签", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val idx = which - 1
                        if (idx < bms.size) viewModel.openBookmark(bms[idx])
                        else manageBookmarks(bms)
                    }
                }
            }
            .show()
    }

    private fun manageBookmarks(bms: List<BookmarkStore.Bookmark>) {
        val names = bms.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("删除书签")
            .setItems(names) { _, i -> viewModel.removeBookmark(bms[i].path) }
            .show()
    }

    private fun showRenameDialog(item: FileItem) {
        val input = com.google.android.material.textfield.TextInputEditText(this).apply { setText(item.name) }
        MaterialAlertDialogBuilder(this)
            .setTitle("重命名")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _, _ ->
                val n = input.text?.toString()?.trim()
                if (!n.isNullOrEmpty()) viewModel.rename(item, n)
            }
            .show()
    }

    private fun hintCopied(n: Int, mode: Clipboard.Mode) {
        Toast.makeText(this, "已${mode.label} $n 项，去目标目录点「粘贴」", Toast.LENGTH_LONG).show()
    }

    private fun enterSelection(item: FileItem) {
        selectionMode = true
        checked.clear()
        checked.add(item.path)
        adapter.selectionMode = true
        adapter.setChecked(checked)
        refreshSelectionUi()
    }

    private fun exitSelection() {
        selectionMode = false
        checked.clear()
        adapter.selectionMode = false
        adapter.setChecked(emptySet())
        refreshSelectionUi()
    }

    private fun toggleSelectAll() {
        val all = adapter.currentList.toList()
        if (checked.size >= all.size) checked.clear() else checked.addAll(all.map { it.path })
        adapter.setChecked(checked)
        refreshSelectionUi()
    }

    private fun refreshSelectionUi() {
        actionBar.visibility = if (selectionMode) View.VISIBLE else View.GONE
        btnPaste.visibility = if (viewModel.clipboard.value != null) View.VISIBLE else View.GONE
        if (selectionMode) statusText.text = "已选 ${checked.size} 项"
        btnSelectAll.text = if (checked.isNotEmpty()) "取消全选" else "全选"
    }

    private fun putClipboard(mode: Clipboard.Mode) {
        val items = adapter.currentList.filter { it.path in checked }
        if (items.isEmpty()) { Toast.makeText(this, "没有选中文件", Toast.LENGTH_SHORT).show(); return }
        viewModel.putClipboard(items, mode)
        exitSelection()
        Toast.makeText(this, "已${mode.label} ${items.size} 项，去目标目录点「粘贴」", Toast.LENGTH_LONG).show()
    }

    private fun updatePasteUi(cb: Clipboard?) {
        btnPaste.visibility = if (cb != null) View.VISIBLE else View.GONE
        if (cb != null) btnPaste.text = "粘贴 ${cb.items.size} 项"
    }

    // ---------- 打开 ----------
    private fun onItemClick(item: FileItem) {
        if (selectionMode) {
            if (item.path in checked) checked.remove(item.path) else checked.add(item.path)
            adapter.setChecked(checked); refreshSelectionUi(); return
        }
        if (item.isDirectory) viewModel.enterDirectory(item) else openFile(item)
    }

    private fun openFile(item: FileItem, mimeOverride: String? = null) {
        try {
            val file = File(item.path)
            val uri: Uri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeOverride ?: item.mimeType ?: "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "打开方式"))
        } catch (e: Exception) {
            Toast.makeText(this, "打不开这个文件", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onBackPressed() {
        if (selectionMode) { exitSelection(); return }
        if (viewModel.searchResults.value != null) {
            lastKeyword = ""
            viewModel.clearSearch()
            viewModel.open(viewModel.currentPathValue())
            return
        }
        if (!viewModel.goUp()) super.onBackPressed()
    }
}
