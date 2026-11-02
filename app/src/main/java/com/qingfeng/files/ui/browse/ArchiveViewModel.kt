package com.qingfeng.files.ui.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingfeng.files.data.ArchiveEngine
import com.qingfeng.files.data.ArchiveState
import com.qingfeng.files.data.CompressLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 压缩 / 解压的 ViewModel。
 *
 * 借鉴点：参照物把它当**前台长任务**处理（可中断 + 实时进度）。
 */
class ArchiveViewModel : ViewModel() {

    private val engine = ArchiveEngine()

    private val _state = MutableLiveData<ArchiveState>(ArchiveState.Idle)
    val state: LiveData<ArchiveState> = _state

    var level: CompressLevel = CompressLevel.STANDARD

    fun compress(files: List<File>, zipFile: File) {
        viewModelScope.launch {
            _state.value = ArchiveState.Running(true, zipFile.name, 0, 0)
            val r = withContext(Dispatchers.IO) {
                engine.compress(files, zipFile, level) { name, done, total ->
                    _state.postValue(ArchiveState.Running(true, name, done, total))
                    !isStopped()
                }
            }
            _state.value = r.fold(
                onSuccess = { ArchiveState.Done(zipFile.absolutePath, it) },
                onFailure = {
                    if (it.message == "已取消") ArchiveState.Cancelled
                    else ArchiveState.Failed(it.message ?: "压缩失败")
                }
            )
        }
    }

    fun extract(zipFile: File, destDir: File, intoSubDir: Boolean) {
        viewModelScope.launch {
            _state.value = ArchiveState.Running(false, zipFile.name, 0, 0)
            val r = withContext(Dispatchers.IO) {
                engine.extract(zipFile, destDir, intoSubDir) { name, done, total ->
                    _state.postValue(ArchiveState.Running(false, name, done, total))
                    !isStopped()
                }
            }
            _state.value = r.fold(
                onSuccess = { ArchiveState.Done(destDir.absolutePath, it) },
                onFailure = {
                    if (it.message == "已取消") ArchiveState.Cancelled
                    else ArchiveState.Failed(it.message ?: "解压失败")
                }
            )
        }
    }

    /**
     * 停止。
     * 借鉴点：参照物有确认框（`ig`="确定要停止压缩吗？"）——
     * 中断长操作要有二次确认，避免误触。
     */
    fun stop() {
        engine.cancel()
        _stopped = true
    }

    fun reset() { _stopped = false; _state.value = ArchiveState.Idle }

    @Volatile
    private var _stopped = false
    private fun isStopped(): Boolean = _stopped
}
