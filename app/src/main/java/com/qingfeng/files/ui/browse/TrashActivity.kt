package com.qingfeng.files.ui.browse

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.qingfeng.files.R
import com.qingfeng.files.base.BaseActivity
import com.qingfeng.files.data.RecycleBin
import com.qingfeng.files.util.FileFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 回收站界面。
 *
 * 参照物有完整的回收站体系：
 * `il` = "确定要清空回收站吗？"、`az`/`bt`/`kn` = 移入回收站。
 * 我们做最小可用版：列表 + 恢复 + 永久删除 + 清空。
 */
class TrashActivity : BaseActivity() {

    private val viewModel: BrowseViewModel by viewModels()
    private lateinit var container: LinearLayout
    private lateinit var empty: TextView
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trash)

        container = findViewById(R.id.trashList)
        empty = findViewById(R.id.trashEmpty)
        status = findViewById(R.id.trashStatus)

        findViewById<View>(R.id.btnClearTrash).setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("清空回收站")
                .setMessage("确定要清空回收站吗？此操作不可恢复。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空") { _, _ ->
                    viewModel.clearTrash()
                    render()
                    Toast.makeText(this, "已清空", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

        render()
    }

    private fun render() {
        val entries = viewModel.trashEntries()
        container.removeAllViews()
        empty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
        status.text = if (entries.isEmpty()) "回收站是空的"
        else "${entries.size} 项 · 共 ${FileFormat.size(viewModel.trashSize())}"

        val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        entries.forEach { e ->
            val row = layoutInflater.inflate(R.layout.item_trash, container, false)
            row.findViewById<TextView>(R.id.tvTrashName).text =
                e.originPath.substringAfterLast('/')
            row.findViewById<TextView>(R.id.tvTrashPath).text = e.originPath
            row.findViewById<TextView>(R.id.tvTrashTime).text = sdf.format(Date(e.deletedAt))

            row.findViewById<View>(R.id.btnRestore).setOnClickListener {
                if (viewModel.restoreFromTrash(e)) {
                    Toast.makeText(this, "已恢复", Toast.LENGTH_SHORT).show()
                    render()
                } else Toast.makeText(this, "恢复失败（原位置可能已存在同名文件）", Toast.LENGTH_SHORT).show()
            }
            row.findViewById<View>(R.id.btnDeleteForever).setOnClickListener {
                MaterialAlertDialogBuilder(this)
                    .setTitle("永久删除")
                    .setMessage("「${e.originPath.substringAfterLast('/')}」将被彻底删除，无法恢复。")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("删除") { _, _ ->
                        viewModel.deleteFromTrash(e)
                        render()
                    }
                    .show()
            }
            container.addView(row)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}
