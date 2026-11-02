package com.qingfeng.files.data

import android.os.Environment
import java.io.File

/**
 * 存储位置。
 *
 * 借鉴点：参照物工具栏有 `device_icon`（存储/设备切换），
 * 支持内部存储、SD 卡、USB OTG（`a14` 文案：选择外置 SD 卡根目录授权）。
 *
 * 我们做最小版：内部存储 + /storage 下能读到的挂载点。
 */
data class StorageEntry(
    val path: String,
    val label: String,
    val isRemovable: Boolean
) {
    val file: File get() = File(path)
}

object Storages {

    fun list(): List<StorageEntry> {
        val out = ArrayList<StorageEntry>()

        val primary = Environment.getExternalStorageDirectory()
        if (primary.exists()) {
            out.add(
                StorageEntry(
                    primary.absolutePath,
                    "内部存储",
                    false
                )
            )
        }

        // /storage 下的其他挂载点（SD 卡、USB）
        val storageDir = File("/storage")
        val children = storageDir.listFiles()
        if (children != null) {
            for (f in children) {
                if (!f.isDirectory) continue
                if (f.absolutePath == primary.absolutePath) continue
                if (!f.canRead()) continue
                if (f.name.equals("self", ignoreCase = true)) continue
                if (f.name.equals("emulated", ignoreCase = true)) continue
                out.add(StorageEntry(f.absolutePath, f.name, true))
            }
        }

        // 常见快捷目录
        val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (dl.exists()) out.add(StorageEntry(dl.absolutePath, "下载", false))
        val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        if (dcim.exists()) out.add(StorageEntry(dcim.absolutePath, "相机", false))

        return out.distinctBy { it.path }
    }
}
