package com.qingfeng.files.data

import java.io.File

/**
 * 压缩 / 解压任务的状态。
 *
 * 借鉴点：参照物是**可中断的长任务**，且有明确文案：
 * - `ig` = "确定要停止压缩吗？"
 * - `ih`/`afi` = "Compressing"、`ka`/`afp` = "Decompressing"
 * - `zu` = "正在压缩 %1$s.%2$s"（带文件名和扩展名）
 * → 说明它有**实时进度显示**，不只是转圈。
 */
sealed class ArchiveState {
    object Idle : ArchiveState()
    data class Running(
        val isCompress: Boolean,
        val currentName: String,
        val done: Int,
        val total: Int
    ) : ArchiveState() {
        val percent: Int get() = if (total <= 0) 0 else (done * 100 / total)
    }
    data class Done(val outputPath: String, val count: Int) : ArchiveState()
    data class Failed(val reason: String) : ArchiveState()
    object Cancelled : ArchiveState()
}

/** 解压时的目标选择（参照物 `ak`="Extract to"、`l0`="Extract selected files to"） */
data class ExtractOption(
    val destDir: File,
    /** 解压到以压缩包命名的子目录，还是直接摊在当前目录 */
    val intoSubDir: Boolean
)
