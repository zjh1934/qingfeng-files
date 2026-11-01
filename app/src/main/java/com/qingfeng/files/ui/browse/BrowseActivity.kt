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
import com.qingfeng.files.data.FileItem
import java.io.File

/**
 * 文件浏览主页面。
 */
class BrowseActivity : BaseActivity() {

    private val viewModel: BrowseViewModel by viewModels()
    private lateinit var adapter: FileAdapter
    private lateinit var recycler: RecyclerView
    private lateinit var pathText: TextView
    private lateinit var emptyText: TextView
    private lateinit var swipe: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browse)

        pathText = findViewById(R.id.tvPath)
        emptyText = findViewById(R.id.tvEmpty)
        swipe = findViewById(R.id.swipeRefresh)
        recycler = findViewById(R.id.recycler)

        adapter = FileAdapter(
            onItemClick = { item -> onItemClick(item) },
            onItemLongClick = { item -> showItemMenu(item) }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        swipe.setOnRefreshListener { viewModel.open(viewModel.currentPathValue()) }

        viewModel.items.observe(this) { list ->
            adapter.submitList(list)
            emptyText.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            swipe.isRefreshing = false
        }
        viewModel.currentPath.observe(this) { path -> pathText.text = path }
        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.start()
    }

    private fun onItemClick(item: FileItem) {
        if (item.isDirectory) {
            viewModel.enterDirectory(item)
        } else {
            openFile(item)
        }
    }

    private fun openFile(item: FileItem) {
        try {
            val file = File(item.path)
            val uri: Uri = FileProvider.getUriForFile(
                this, "${packageName}.fileprovider", file
            )
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
        val options = arrayOf("删除", "重命名")
        MaterialAlertDialogBuilder(this)
            .setTitle(item.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> confirmDelete(item)
                    1 -> showRenameDialog(item)
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
                if (!newName.isNullOrEmpty()) {
                    viewModel.rename(item, newName)
                }
            }
            .show()
    }

    override fun onBackPressed() {
        if (!viewModel.goUp()) {
            super.onBackPressed()
        }
    }
}
