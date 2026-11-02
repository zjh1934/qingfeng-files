package com.qingfeng.files.ui.browse

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.qingfeng.files.R
import com.qingfeng.files.data.FileItem
import com.qingfeng.files.data.ViewMode
import com.qingfeng.files.util.FileFormat

/**
 * 文件列表适配器。
 *
 * 借鉴点：参照物有 9 种 item 布局变体（不同视图模式用不同布局），
 * 且列表项**自带复选框**（多选不需要先进特殊模式）。
 * 我们做两套：列表布局 + 网格布局。
 */
class FileAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Unit,
    private val onCheckChange: (FileItem, Boolean) -> Unit,
    private val onMoreClick: ((FileItem) -> Unit)? = null
) : ListAdapter<FileItem, FileAdapter.Holder>(Diff) {

    var viewMode: ViewMode = ViewMode.GRID_MEDIUM
    var selectionMode: Boolean = false
    private val checked = HashSet<String>()

    fun setChecked(paths: Set<String>) {
        checked.clear()
        checked.addAll(paths)
        notifyDataSetChanged()
    }

    fun isChecked(item: FileItem) = item.path in checked

    object Diff : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(a: FileItem, b: FileItem) = a.path == b.path
        override fun areContentsTheSame(a: FileItem, b: FileItem) =
            a.name == b.name && a.size == b.size && a.lastModified == b.lastModified
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: TextView = view.findViewById(R.id.tvIcon)
        val name: TextView = view.findViewById(R.id.tvName)
        val meta: TextView = view.findViewById(R.id.tvMeta)
        val check: CheckBox = view.findViewById(R.id.cbSelect)
        val more: TextView? = view.findViewById(R.id.btnMore)
    }

    override fun getItemViewType(position: Int): Int =
        if (viewMode.isGrid) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val layout = if (viewType == 1) R.layout.item_file_grid else R.layout.item_file
        val v = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return Holder(v)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = getItem(position)
        holder.icon.text = FileFormat.icon(item)
        holder.name.text = item.name
        holder.meta.text = "${FileFormat.size(item)}   ${FileFormat.time(item)}".trim()
        holder.meta.visibility = if (viewMode.isGrid) View.GONE else View.VISIBLE
        holder.check.visibility = if (selectionMode) View.VISIBLE else View.GONE
        holder.check.setOnCheckedChangeListener(null)
        holder.check.isChecked = item.path in checked
        holder.check.setOnCheckedChangeListener { _, isChecked -> onCheckChange(item, isChecked) }
        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener { onItemLongClick(item); true }
        holder.more?.setOnClickListener { onMoreClick?.invoke(item) }
    }
}
