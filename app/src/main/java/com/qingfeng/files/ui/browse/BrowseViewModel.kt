package com.qingfeng.files.ui.browse

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qingfeng.files.data.FileError
import com.qingfeng.files.data.FileItem
import com.qingfeng.files.data.FileSource
import com.qingfeng.files.data.LocalFileSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 浏览页的 ViewModel。MVVM：数据在这里，Activity 只管显示。
 */
class BrowseViewModel : ViewModel() {

    private val source: FileSource = LocalFileSource()
    private val pathStack = ArrayDeque<String>()

    private val _items = MutableLiveData<List<FileItem>>()
    val items: LiveData<List<FileItem>> = _items

    private val _currentPath = MutableLiveData<String>()
    val currentPath: LiveData<String> = _currentPath

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _canGoUp = MutableLiveData<Boolean>()
    val canGoUp: LiveData<Boolean> = _canGoUp

    fun start() {
        if (pathStack.isEmpty()) {
            val root = source.rootPath()
            pathStack.addLast(root)
            open(root)
        }
    }

    fun open(path: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            _currentPath.value = path
            _canGoUp.value = pathStack.size > 1
            try {
                val list = withContext(Dispatchers.IO) { source.list(path) }
                _items.value = list
            } catch (e: FileError) {
                _error.value = e.message
                _items.value = emptyList()
            } catch (e: Exception) {
                _error.value = e.message ?: "读取失败"
                _items.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }

    fun enterDirectory(item: FileItem) {
        if (!item.isDirectory) return
        pathStack.addLast(item.path)
        open(item.path)
    }

    /** 返回上一层，返回 false 表示已经在根目录 */
    fun goUp(): Boolean {
        if (pathStack.size <= 1) return false
        pathStack.removeLast()
        open(pathStack.last())
        return true
    }

    fun delete(item: FileItem) {
        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) { source.delete(item) }
                if (ok) open(pathStack.last()) else _error.value = "删除失败"
            } catch (e: FileError) {
                _error.value = e.message
            } catch (e: Exception) {
                _error.value = e.message ?: "删除失败"
            }
        }
    }

    fun rename(item: FileItem, newName: String) {
        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) { source.rename(item, newName) }
                if (ok) open(pathStack.last()) else _error.value = "重命名失败"
            } catch (e: FileError) {
                _error.value = e.message
            } catch (e: Exception) {
                _error.value = e.message ?: "重命名失败"
            }
        }
    }

    fun mkdir(name: String) {
        viewModelScope.launch {
            try {
                val ok = withContext(Dispatchers.IO) { source.mkdir(pathStack.last(), name) }
                if (ok) open(pathStack.last()) else _error.value = "新建失败"
            } catch (e: FileError) {
                _error.value = e.message
            } catch (e: Exception) {
                _error.value = e.message ?: "新建失败"
            }
        }
    }

    fun currentPathValue(): String = pathStack.lastOrNull() ?: source.rootPath()
}
