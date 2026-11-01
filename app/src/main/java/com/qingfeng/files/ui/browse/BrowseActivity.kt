package com.qingfeng.files.ui.browse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.qingfeng.files.R
import com.qingfeng.files.base.BaseActivity
import com.qingfeng.files.data.Category
import com.qingfeng.files.data.FileItem
import java.io.File

/**
 * 文件浏览主页面：浏览 + 搜索 + 分类 + 基本操作。
 */
class BrowseActivity : BaseActivity() {

    private val viewModel: BrowseViewModel by viewModels()
    private lateinit var adapter: FileAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var pathText: TextView
    private lateinit var emptyText: TextView
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var chipBar: com.google.android.material.chip.ChipGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        pathText = findViewById(R.id.tvPath)
        emptyText = findViewById(R.id.tvEmpty)
        swipe = findViewById(R.id.swipeRefresh)
        recycler = findViewById(R.id.recycler)
        chipBar = findViewById(R.id.chipBar)

        adapter = FileAdapter(
            onItemClick = { onItemClick(it) },
            onItemLongClick = { showItemMenu(it) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        setupChips()
        swipe.setOnRefreshListener { viewModel.open(viewModel.currentPathValue()) }

        // 三种数据源互斥：搜索 > 分类 > 目录浏览
        viewModel.items.observe(this) { list ->
            if (viewModel.isPlainBrowse()) render(list)
        }
        viewModel.searchResults.observe(this) { list ->
            if (list != null) render(list, "搜索结果 ${list.size} 项")
        }
        viewModel.categoryResults.observe(this) { list ->
            if (list != null) render(list, "${viewModel.categoryLabel.value ?: "分类"} ${list.size} 项")
        }
        viewModel.currentPath.observe(this) { path ->
            if (viewModel.isPlainBrowse()) pathText.text = path
        }
        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.start()
    }

    private fun render(list: List<FileItem>, titleOverride: String? = null) {
        adapter.submitList(list)
        emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        swipe.isRefreshing = false
        if (titleOverride != null) pathText.text = titleOverride
    }

    private fun setupChips() {
        val cats = listOf(null) + Category.values().toList()
        for (c in cats) {
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = c?.label ?: "全部文件"
                isCheckable = true
                isChecked = (c == null)
            }
            chip.setOnCheckedChangeListener { _, isChecked ->
                if (!isChecked) return@setOnCheckedChangeListener
                if (c == null) {
                    viewModel.clearCategory()
                    viewModel.clearSearch()
                    viewModel.open(viewModel.currentPathValue())
                } else {
                    viewModel.clearSearch()
                    viewModel.openCategory(c)
                }
            }
            chipBar.addView(chip)
        }

        val searchChip = com.google.android.material.chip.Chip(this).apply {
            text = "搜索"
            isCheckable = false
        }
        searchChip.setOnClickListener { showSearchDialog() }
        chipBar.addView(searchChip)

        val newFolderChip = com.google.android.material.chip.Chip(this).apply {
            text = "新建文件夹"
            isCheckable = false
        }
        newFolderChip.setOnClickListener { showNewFolderDialog() }
        chipBar.addView(newFolderChip)
    }

    private fun showSearchDialog() {
        val input = TextInputEditText(this).apply { hint = "文件名关键字" }
        MaterialAlertDialogBuilder(this)
            .setTitle("在当前目录及子目录中搜索")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("搜索") { _, _ ->
                val kw = input.text?.toString()?.trim()
                if (!kw.isNullOrEmpty()) {
                    viewModel.clearCategory()
                    viewModel.search(kw)
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
                if (!n.isNullOrEmpty()) viewModel.createFolder(n)
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
        val dirs = (base.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList())
            .toMutableList().apply { add(0, "..（上一级）") }
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
        if (viewModel.searchResults.value != null) { viewModel.clearSearch(); viewModel.open(viewModel.currentPathValue()); return }
        if (viewModel.categoryResults.value != null) { viewModel.clearCategory(); viewModel.open(viewModel.currentPathValue()); return }
        if (!viewModel.goUp()) super.onBackPressed()
    }
}
